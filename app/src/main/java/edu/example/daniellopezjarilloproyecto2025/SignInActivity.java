package edu.example.daniellopezjarilloproyecto2025;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
@SuppressWarnings("deprecation")
public class SignInActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private EditText editEmail, editPassword;
    private Button btnVipLogin, btnGoRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        configureGoogleSignIn();

        // --- Referencias para VIP login ---
        editEmail = findViewById(R.id.editTextEmail);
        editPassword = findViewById(R.id.editTextPassword);
        btnVipLogin = findViewById(R.id.btnLoginVip);
        btnGoRegister = findViewById(R.id.btnGoRegisterVip);

        btnVipLogin.setOnClickListener(v -> loginWithEmail());
        btnGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, RegisterVipActivity.class);
            startActivity(intent);
        });
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        SignInButton signInButton = findViewById(R.id.btnGoogleSignIn);
        setGoogleSignInButtonText(signInButton, "Sign in with Google");
        signInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void setGoogleSignInButtonText(SignInButton signInButton, String buttonText) {
        for (int i = 0; i < signInButton.getChildCount(); i++) {
            View view = signInButton.getChildAt(i);
            if (view instanceof TextView) {
                ((TextView) view).setText(buttonText);
                return;
            }
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            com.google.android.gms.tasks.Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(com.google.android.gms.common.api.ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Google Sign-In Fallido", Toast.LENGTH_SHORT).show();
                Log.e("GoogleSignIn", "Error", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    saveUserIdToPreferences(user.getUid(), user.getEmail());
                    firestore.collection("users").document(user.getUid()).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (!documentSnapshot.exists()) {
                                    saveUserToFirestore(user);
                                }
                                saveLoginToFirestore(user.getUid());
                                navigateToMainActivity(user);
                            })
                            .addOnFailureListener(e -> Log.e("Firestore", "Error al verificar usuario en Firestore", e));
                }
            }
        });
    }

    private void loginWithEmail() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Introduce email y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        saveUserIdToPreferences(user.getUid(), user.getEmail());
                        saveLoginToFirestore(user.getUid());
                        navigateToMainActivity(user);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Login fallido: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void navigateToMainActivity(FirebaseUser user) {
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        intent.putExtra("USER_NAME", user.getDisplayName());
        intent.putExtra("USER_EMAIL", user.getEmail());
        intent.putExtra("USER_PHOTO", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        startActivity(intent);
        finish();
    }

    private void saveUserIdToPreferences(String userId, String email) {
        SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).edit();
        editor.putString("USER_ID", userId);
        editor.putString("USER_EMAIL", email);
        editor.apply();
    }

    private void saveLoginToFirestore(String userId) {
        String loginTime = getCurrentTime();
        Map<String, Object> loginEntry = new HashMap<>();
        loginEntry.put("login_time", loginTime);
        loginEntry.put("logout_time", null);

        firestore.collection("users").document(userId)
                .update("activity_log", FieldValue.arrayUnion(loginEntry))
                .addOnFailureListener(e -> Log.e("Firestore", "Error al guardar login", e));
    }

    private void saveUserToFirestore(FirebaseUser user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("name", user.getDisplayName());
        if (user.getPhotoUrl() != null) {
            userData.put("photoUrl", user.getPhotoUrl().toString());
        }

        firestore.collection("users").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnFailureListener(e -> Log.e("Firestore", "Error al guardar usuario", e));
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}
