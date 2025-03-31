package edu.example.daniellopezjarilloproyecto2025;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;  // Asegúrate de tener Glide en tu Gradle
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.example.daniellopezjarilloproyecto2025.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enlazamos el layout principal mediante ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configuramos la Toolbar como ActionBar
        setSupportActionBar(binding.appBarMain.toolbar);

        // Referencia al NavigationView (menú lateral)
        NavigationView navigationView = binding.navView;
        // Obtenemos la View del header para acceder a sus elementos
        View headerView = navigationView.getHeaderView(0);

        // --- Referencias en el Header ---
        ImageButton logoutButton = headerView.findViewById(R.id.imageButtonBack);
        TextView nameTextView = headerView.findViewById(R.id.nameTextView);
        TextView emailTextView = headerView.findViewById(R.id.emailTextView);
        ImageView profileImageView = headerView.findViewById(R.id.imageView);

        // --- Recuperar datos del usuario ---
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        String userName = getIntent().getStringExtra("USER_NAME");
        String userPhotoUrl = getIntent().getStringExtra("USER_PHOTO");

        // Por si el Intent no trae datos, comprobamos FirebaseAuth:
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if ((userEmail == null || userEmail.isEmpty()) && currentUser != null) {
            userEmail = currentUser.getEmail();
            userName = currentUser.getDisplayName();
            if (currentUser.getPhotoUrl() != null) {
                userPhotoUrl = currentUser.getPhotoUrl().toString();
            }
        }

        // --- Asignar los datos en la UI ---
        if (userName != null) {
            nameTextView.setText(userName);
        }
        if (userEmail != null) {
            emailTextView.setText(userEmail);
        }
        if (userPhotoUrl != null && !userPhotoUrl.isEmpty()) {
            Glide.with(this)
                    .load(userPhotoUrl)
                    .placeholder(R.mipmap.ic_launcher_round)  // Ajusta tu placeholder
                    .error(R.mipmap.ic_launcher_round)
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.mipmap.ic_launcher_round);
        }

        // --- Logout con ImageButton ---
        logoutButton.setOnClickListener(view -> {
            // 1) Logout de Google
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(MainActivity.this, gso);
            googleSignInClient.signOut().addOnCompleteListener(task -> googleSignInClient.revokeAccess());

            // 2) Logout de Firebase
            FirebaseAuth.getInstance().signOut();

            // 3) Limpiar SharedPreferences (por si guardaste email, userId, etc.)
            SharedPreferences sp = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            sp.edit().clear().apply();

            // 4) Ir a la pantalla de SignIn
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // --- Configuración del Drawer (menú lateral) ---
        DrawerLayout drawer = binding.drawerLayout;
        // Los IDs nav_home, nav_gallery, nav_slideshow deben estar en tu mobile_navigation.xml
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_gallery,
                R.id.nav_slideshow
        )
                .setOpenableLayout(drawer)
                .build();

        // --- Configuración de NavController ---
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        // Vincular NavController con la ActionBar (para que aparezca la hamburguesa, etc.)
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        // Vincular NavController con el NavigationView
        NavigationUI.setupWithNavController(navigationView, navController);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Infla el menú de la ActionBar (si lo tienes)
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Permite manejar el botón de "arriba" (navigateUp)
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(
                this,
                R.id.nav_host_fragment_content_main
        );
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}