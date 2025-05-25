package edu.example.daniellopezjarilloproyecto2025;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText editName, editEmail, editPassword, editConfirmPassword;
    private Button btnRegister;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // referencias
        editName            = findViewById(R.id.editName);
        editEmail           = findViewById(R.id.editEmail);
        editPassword        = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        btnRegister         = findViewById(R.id.btnRegister);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore    = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> registrarUsuario());
    }

    private void registrarUsuario() {
        String name     = editName.getText().toString().trim();
        String email    = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String confirm  = editConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(auth -> {
                    FirebaseUser user = auth.getUser();
                    if (user != null) {
                        guardarUsuarioEnFirestore(user.getUid(), name, email);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RegisterVIP", "Error registro", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void guardarUsuarioEnFirestore(String uid, String name, String email) {
        Map<String,Object> data = new HashMap<>();
        data.put("userId",     uid);
        data.put("name",       name);     // <-- nuevo
        data.put("email",      email);
        data.put("vip",        false);    // usuario normal
        data.put("login_time",    com.google.firebase.Timestamp.now());
        data.put("logout_time",   null);

        firestore.collection("users")
                .document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Registrado con éxito", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error guardando usuario", e);
                    Toast.makeText(this, "Error guardando datos", Toast.LENGTH_SHORT).show();
                });
    }
}
