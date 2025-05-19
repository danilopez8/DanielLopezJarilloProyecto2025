package edu.example.daniellopezjarilloproyecto2025;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.hbb20.CountryCodePicker;

import java.util.HashMap;
import java.util.Map;

public class VipActivity extends AppCompatActivity {

    private EditText etPhone, etAddress;
    private CountryCodePicker ccp;
    private Button btnUpgrade;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private boolean alreadyVip = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vip);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        etPhone    = findViewById(R.id.etVipPhone);
        etAddress  = findViewById(R.id.etVipAddress);
        ccp        = findViewById(R.id.ccp);
        btnUpgrade = findViewById(R.id.btnVipUpgrade);

        // Vincular picker y auto-detectar
        ccp.registerCarrierNumberEditText(etPhone);
        ccp.detectSIMCountry(true);

        // Comprobar si ya es VIP: desactivar formulario
        FirebaseUser u = auth.getCurrentUser();
        if (u != null) {
            db.collection("users")
                    .document(u.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && Boolean.TRUE.equals(doc.getBoolean("vip"))) {
                            alreadyVip = true;
                            btnUpgrade.setEnabled(false);
                            btnUpgrade.setText("Ya eres VIP");
                        }
                    });
        }

        btnUpgrade.setOnClickListener(v -> {
            if (!alreadyVip) upgradeToVip();
        });
    }

    private void upgradeToVip() {
        String phone   = ccp.getFullNumberWithPlus().trim();
        String address = etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(address) || !ccp.isValidFullNumber()) {
            Toast.makeText(this,
                    "Comprueba teléfono y dirección",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser u = auth.getCurrentUser();
        if (u == null) {
            Toast.makeText(this,
                    "Sesión no iniciada",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String,Object> data = new HashMap<>();
        data.put("vip", true);
        data.put("phone", phone);
        data.put("address", address);

        db.collection("users")
                .document(u.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(a -> {
                    Toast.makeText(this,
                            "Ahora eres VIP",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error actualizando perfil",
                                Toast.LENGTH_SHORT).show()
                );
    }
}
