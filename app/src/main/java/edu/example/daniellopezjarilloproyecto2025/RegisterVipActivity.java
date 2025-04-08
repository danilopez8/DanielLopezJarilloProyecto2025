package edu.example.daniellopezjarilloproyecto2025;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class RegisterVipActivity extends AppCompatActivity {

    // Declaramos las variables
    private EditText editName, editEmail, editPassword, editConfirmPassword;
    private Button btnRegisterVip;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_vip);

        // Inicializo FirebaseAuth y Firestore
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Enlazo las variables con los elementos del XML
        editName = findViewById(R.id.editTextVipName);
        editEmail = findViewById(R.id.editTextVipEmail);
        editPassword = findViewById(R.id.editTextVipPassword);
        editConfirmPassword = findViewById(R.id.editTextVipConfirmPassword);
        btnRegisterVip = findViewById(R.id.btnRegisterVip);

        // Cuando el usuario pulsa el botón, se llama a la función para registrarse
        btnRegisterVip.setOnClickListener(v -> registerUser());
    }

    // Recoge los datos introducidos por el usuario y los valida
    private void registerUser() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

        // Compruebo que ningún campo esté vacío
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Compruebo que las contraseñas coincidan
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        // Registramos al usuario en firebase
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        saveUserToFirestore(user.getUid(), name, email);
                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, SignInActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    // Si hay algún error en el registro, lo muestro en un Toast
                    Log.e("RegisterVIP", "Error al registrar", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Guardo los datos del usuario en la base de datos Firestore
    private void saveUserToFirestore(String uid, String name, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", uid);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("vip", true);

        firestore.collection("users").document(uid)
                .set(userData, SetOptions.merge())
                .addOnFailureListener(e -> Log.e("Firestore", "Error guardando VIP", e));
    }
}
