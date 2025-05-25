package edu.example.daniellopezjarilloproyecto2025;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import edu.example.daniellopezjarilloproyecto2025.databinding.ActivityMainBinding;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME     = "MyPrefs";
    private static final String THEME_PREFS    = "ThemePrefs";
    private static final String KEY_DARK_MODE  = "dark_mode";

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private boolean isVip = false;

    // Referencias del header
    private TextView  headerName;
    private TextView  headerEmail;
    private ImageView headerImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 0) Aplicar tema desde prefs (persistente tras logout)
        SharedPreferences themeSp = getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE);
        boolean darkMode = themeSp.getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);

        // Inicializar views del header
        headerName  = headerView.findViewById(R.id.nameTextView);
        headerEmail = headerView.findViewById(R.id.emailTextView);
        headerImage = headerView.findViewById(R.id.imageView);
        ImageButton logoutButton = headerView.findViewById(R.id.imageButtonBack);

        // Configurar Drawer + NavController
        DrawerLayout drawer = binding.drawerLayout;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
        ).setOpenableLayout(drawer).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
        NavigationUI.setupWithNavController(bottomNav, navController);

        // ** NUEVO: cada vez que cambias de fragmento recargamos header **
        navController.addOnDestinationChangedListener((controller, destination, args) -> {
            setupUserHeader();
            checkVipStatus();
        });

        // Primera carga del header
        setupUserHeader();

        // Logout
        logoutButton.setOnClickListener(v -> {
            GoogleSignInClient gsc = GoogleSignIn.getClient(this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .build()
            );
            gsc.signOut().addOnCompleteListener(t -> gsc.revokeAccess());
            FirebaseAuth.getInstance().signOut();
            // Limpiar sólo prefs de usuario, no tema
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().clear().apply();
            startActivity(new Intent(this, SignInActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        // VIP
        MenuItem vipItem = menu.findItem(R.id.action_vip);
        if (isVip) {
            vipItem.setIcon(R.drawable.esvip);
            vipItem.setEnabled(false);
        }
        // Dark mode toggle
        MenuItem darkItem = menu.findItem(R.id.action_toggle_dark);
        boolean darkMode = getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_DARK_MODE, false);
        darkItem.setTitle(darkMode ? "Modo claro" : "Modo oscuro");
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
                    .setMessage("¿Estás seguro de que quieres eliminar tu cuenta y todos tus datos?")
                    .setPositiveButton("Sí", (d,w) -> {
                        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                        if (u != null) {
                            String uid = u.getUid();
                            FirebaseFirestore.getInstance()
                                    .collection("users").document(uid).delete()
                                    .addOnCompleteListener(t -> u.delete().addOnCompleteListener(tt -> {
                                        FirebaseAuth.getInstance().signOut();
                                        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                                .edit().clear().apply();
                                        startActivity(new Intent(this, SignInActivity.class)
                                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                        finish();
                                    }));
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        }
        if (id == R.id.action_toggle_dark) {
            SharedPreferences themeSp = getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE);
            boolean darkMode = themeSp.getBoolean(KEY_DARK_MODE, false);
            themeSp.edit().putBoolean(KEY_DARK_MODE, !darkMode).apply();
            AppCompatDelegate.setDefaultNightMode(
                    darkMode ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES
            );
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController nav = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(nav, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    /** Rellena nombre/email/foto y detecta VIP */
    private void setupUserHeader() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;
        String uid = u.getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String name     = doc.getString("name");
                    String email    = doc.getString("email");
                    String photoUrl = doc.getString("photoUrl");
                    Boolean vipFlag = doc.getBoolean("vip");

                    headerName.setText(name  != null ? name  : "Usuario");
                    headerEmail.setText(email != null ? email : "Email");
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(this).load(photoUrl)
                                .placeholder(R.mipmap.ic_launcher_round)
                                .error(R.mipmap.ic_launcher_round)
                                .into(headerImage);
                    } else {
                        loadLocalProfileImage(headerImage);
                    }

                    if (Boolean.TRUE.equals(vipFlag) && !isVip) {
                        isVip = true;
                        invalidateOptionsMenu();
                    }
                });
    }

    /** Comprueba VIP y refresca el menú */
    private void checkVipStatus() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;
        String uid = u.getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && Boolean.TRUE.equals(doc.getBoolean("vip")) && !isVip) {
                        isVip = true;
                        invalidateOptionsMenu();
                    }
                });
    }

    private void loadLocalProfileImage(ImageView imageView) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String encoded = prefs.getString("PROFILE_IMAGE", null);
        if (encoded != null) {
            try {
                byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
                Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                imageView.setImageBitmap(bmp);
            } catch (Exception e) {
                imageView.setImageResource(R.mipmap.ic_launcher_round);
            }
        } else {
            imageView.setImageResource(R.mipmap.ic_launcher_round);
        }
    }
}
