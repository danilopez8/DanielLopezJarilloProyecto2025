package edu.example.daniellopezjarilloproyecto2025;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import edu.example.daniellopezjarilloproyecto2025.databinding.ActivityMainBinding;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);

        // Referencias en el Header
        ImageButton logoutButton = headerView.findViewById(R.id.imageButtonBack);
        TextView nameTextView = headerView.findViewById(R.id.nameTextView);
        TextView emailTextView = headerView.findViewById(R.id.emailTextView);
        ImageView profileImageView = headerView.findViewById(R.id.imageView);

        // Obtener el usuario actual
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Obtener los datos desde Firestore
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String photoUrl = documentSnapshot.getString("photoUrl");

                            nameTextView.setText(name != null ? name : "Usuario");
                            emailTextView.setText(email != null ? email : "Email");

                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(photoUrl)
                                        .placeholder(R.mipmap.ic_launcher_round)
                                        .error(R.mipmap.ic_launcher_round)
                                        .into(profileImageView);
                            } else {
                                // Carga la imagen local si no hay URL de Firebase
                                loadLocalProfileImage(profileImageView);
                            }

                        }
                    })
                    .addOnFailureListener(e -> {
                        nameTextView.setText("Error");
                        emailTextView.setText("Error");
                    });
        }

        // Botón de logout
        logoutButton.setOnClickListener(view -> {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(MainActivity.this, gso);
            googleSignInClient.signOut().addOnCompleteListener(task -> googleSignInClient.revokeAccess());

            FirebaseAuth.getInstance().signOut();

            SharedPreferences sp = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            sp.edit().clear().apply();

            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Config Drawer
        DrawerLayout drawer = binding.drawerLayout;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_gallery,
                R.id.nav_slideshow
        ).setOpenableLayout(drawer).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
        NavigationUI.setupWithNavController(bottomNav, navController);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    private void loadLocalProfileImage(ImageView profileImageView) {
        SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String encodedImage = prefs.getString("PROFILE_IMAGE", null);
        if (encodedImage != null) {
            try {
                byte[] imageBytes = Base64.decode(encodedImage, Base64.DEFAULT);
                Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                profileImageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                profileImageView.setImageResource(R.mipmap.ic_launcher_round);
            }
        } else {
            profileImageView.setImageResource(R.mipmap.ic_launcher_round);
        }
    }

}
