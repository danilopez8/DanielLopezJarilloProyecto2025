package edu.example.daniellopezjarilloproyecto2025;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.example.daniellopezjarilloproyecto2025.databinding.ActivityMainBinding;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME    = "MyPrefs";
    private static final String THEME_PREFS   = "ThemePrefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private boolean isVip = false;

    private TextView  headerName;
    private TextView  headerEmail;
    private ImageView headerImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Aplica tema guardado
        SharedPreferences themeSp = getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE);
        boolean darkMode = themeSp.getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        setSupportActionBar(binding.appBarMain.toolbar);
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);

        headerName  = headerView.findViewById(R.id.nameTextView);
        headerEmail = headerView.findViewById(R.id.emailTextView);
        headerImage = headerView.findViewById(R.id.imageView);
        ImageButton logoutButton = headerView.findViewById(R.id.imageButtonBack);

        DrawerLayout drawer = binding.drawerLayout;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
        ).setOpenableLayout(drawer).build();

        NavController navController = Navigation.findNavController(
                this, R.id.nav_host_fragment_content_main
        );
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
        NavigationUI.setupWithNavController(bottomNav, navController);

        navController.addOnDestinationChangedListener((c, d, a) -> {
            setupUserHeader();
            checkVipStatus();
        });

        // Primera carga
        setupUserHeader();

        // Logout manual: primero revocamos Google, luego registramos logout_time y cerramos sesión
        logoutButton.setOnClickListener(v -> {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u != null) {
                // Revoke Google access
                GoogleSignInClient gsc = GoogleSignIn.getClient(this,
                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .build()
                );
                gsc.revokeAccess().addOnCompleteListener(task -> {
                    // Actualizamos logout_time y, al terminar la transacción, doSignOut()
                    updateLogoutTimeAndSignOut(u.getUid());
                });
            } else {
                doSignOut();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem vipItem = menu.findItem(R.id.action_vip);
        if (isVip) {
            vipItem.setIcon(R.drawable.esvip);
            vipItem.setEnabled(false);
        }
        MenuItem darkItem = menu.findItem(R.id.action_toggle_dark);
        boolean dm = getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_DARK_MODE, false);
        darkItem.setTitle(dm ? "Modo claro" : "Modo oscuro");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_vip) {
            if (!isVip) startActivity(new Intent(this, VipActivity.class));
            return true;
        }
        if (id == R.id.action_delete_account) {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar cuenta")
                    .setMessage("¿Seguro quieres eliminar tu cuenta y datos?")
                    .setPositiveButton("Sí", (d, w) -> {
                        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                        if (u == null) return;
                        String uid   = u.getUid();
                        String email = u.getEmail();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        // 1) Recogemos y borramos todas las reservas de este email
                        db.collection("reservas")
                                .whereEqualTo("email", email)
                                .get()
                                .addOnSuccessListener(resSnapshot -> {
                                    WriteBatch batch = db.batch();
                                    for (DocumentSnapshot doc : resSnapshot.getDocuments()) {
                                        batch.delete(doc.getReference());
                                    }
                                    // también borramos al usuario
                                    batch.delete(db.collection("users").document(uid));

                                    // 2) Ejecutamos el batch
                                    batch.commit()
                                            .addOnSuccessListener(aVoid -> {
                                                // 3) Revoke Google + delete Auth user + cerrar sesión
                                                GoogleSignInClient gsc = GoogleSignIn.getClient(this,
                                                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                                .requestEmail()
                                                                .build()
                                                );
                                                gsc.revokeAccess().addOnCompleteListener(task1 -> {
                                                    u.delete().addOnCompleteListener(delTask -> {
                                                        FirebaseAuth.getInstance().signOut();
                                                        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                                                .edit().clear().apply();
                                                        startActivity(new Intent(this, SignInActivity.class)
                                                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                                        | Intent.FLAG_ACTIVITY_NEW_TASK
                                                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                                        finish();
                                                    });
                                                });
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("MainActivity","Error en batch",e);
                                                Toast.makeText(this,"Error eliminando datos.",Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("MainActivity","No pude leer reservas",e);
                                    Toast.makeText(this,"No se pudieron cargar reservas.",Toast.LENGTH_SHORT).show();
                                });

                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        }
        if (id == R.id.action_toggle_dark) {
            SharedPreferences tp = getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE);
            boolean dm2 = tp.getBoolean(KEY_DARK_MODE, false);
            tp.edit().putBoolean(KEY_DARK_MODE, !dm2).apply();
            AppCompatDelegate.setDefaultNightMode(
                    dm2 ? AppCompatDelegate.MODE_NIGHT_NO
                            : AppCompatDelegate.MODE_NIGHT_YES
            );
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController nav = Navigation.findNavController(
                this, R.id.nav_host_fragment_content_main
        );
        return NavigationUI.navigateUp(nav, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void setupUserHeader() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;
        String uid = u.getUid();
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    headerName.setText(doc.getString("name"));
                    headerEmail.setText(doc.getString("email"));
                    String photoUrl = doc.getString("photoUrl");
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(this).load(photoUrl)
                                .placeholder(R.mipmap.ic_launcher_round)
                                .error(R.mipmap.ic_launcher_round)
                                .into(headerImage);
                    } else {
                        loadLocalProfileImage(headerImage);
                    }
                    if (Boolean.TRUE.equals(doc.getBoolean("vip")) && !isVip) {
                        isVip = true;
                        invalidateOptionsMenu();
                    }
                });
    }

    private void checkVipStatus() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;
        String uid = u.getUid();
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (Boolean.TRUE.equals(doc.getBoolean("vip")) && !isVip) {
                        isVip = true;
                        invalidateOptionsMenu();
                    }
                });
    }

    private void loadLocalProfileImage(ImageView iv) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String enc = prefs.getString("PROFILE_IMAGE", null);
        if (enc != null) {
            try {
                byte[] b = Base64.decode(enc, Base64.DEFAULT);
                Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(b, 0, b.length);
                iv.setImageBitmap(bmp);
            } catch (Exception e) {
                iv.setImageResource(R.mipmap.ic_launcher_round);
            }
        } else {
            iv.setImageResource(R.mipmap.ic_launcher_round);
        }
    }

    private void updateLogoutTimeAndSignOut(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("users").document(uid);

        db.runTransaction((Transaction.Function<Void>) tx -> {
                    DocumentSnapshot snap = tx.get(ref);
                    @SuppressWarnings("unchecked")
                    List<Map<String,Object>> log = (List<Map<String,Object>>) snap.get("activity_log");
                    if (log != null && !log.isEmpty()) {
                        Map<String,Object> last = log.get(log.size() - 1);
                        if (last.get("logout_time") == null) {
                            String now = new SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
                            ).format(new Date());
                            last.put("logout_time", now);
                            tx.update(ref, "activity_log", log);
                        }
                    }
                    return null;
                }).addOnSuccessListener(unused -> doSignOut())
                .addOnFailureListener(e -> {
                    Log.e("MainActivity","Transaction failed", e);
                    doSignOut();
                });
    }

    /** Solo actualiza logout_time sin cerrar sesión */
    private void updateLogoutTimeOnly(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("users").document(uid);

        db.runTransaction((Transaction.Function<Void>) tx -> {
            DocumentSnapshot snap = tx.get(ref);
            @SuppressWarnings("unchecked")
            List<Map<String,Object>> log = (List<Map<String,Object>>) snap.get("activity_log");
            if (log != null && !log.isEmpty()) {
                Map<String,Object> last = log.get(log.size() - 1);
                if (last.get("logout_time") == null) {
                    String now = new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
                    ).format(new Date());
                    last.put("logout_time", now);
                    tx.update(ref, "activity_log", log);
                }
            }
            return null;
        }).addOnFailureListener(e ->
                Log.e("MainActivity","Transaction failed during restart", e)
        );
    }

    private void doSignOut() {
        FirebaseAuth.getInstance().signOut();
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().apply();
        startActivity(new Intent(this, SignInActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
