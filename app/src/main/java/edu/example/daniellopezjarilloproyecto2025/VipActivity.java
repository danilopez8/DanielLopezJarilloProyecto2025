package edu.example.daniellopezjarilloproyecto2025;

import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.hbb20.CountryCodePicker;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class VipActivity extends AppCompatActivity {

    private static final String TAG = "VipActivity";

    private EditText etPhone, etAddress;
    private CountryCodePicker ccp;
    private Button btnUpgrade;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private boolean alreadyVip = false;

    // Parámetros para AES/GCM en Android Keystore
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS        = "TFG_KEY_ALIAS";
    private static final int    GCM_IV_LENGTH    = 12;    // 12 bytes de IV
    private static final int    GCM_TAG_LENGTH   = 128;   // 128 bits de tag
    private static final String TRANSFORMATION   = "AES/GCM/NoPadding";

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
        String phonePlain   = ccp.getFullNumberWithPlus().trim();
        String addressPlain = etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(addressPlain) || !ccp.isValidFullNumber()) {
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

        // ---- Cifrar teléfono y dirección si es posible ----
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                String encryptedPhone   = encryptString(phonePlain);
                String encryptedAddress = encryptString(addressPlain);
                data.put("phone", encryptedPhone);
                data.put("address", encryptedAddress);
            } catch (Exception e) {
                Log.e(TAG, "Error cifrando datos:", e);
                Toast.makeText(this,
                        "Error cifrando datos: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            // Versiones < M: guardamos en claro
            data.put("phone", phonePlain);
            data.put("address", addressPlain);
        }

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
                                "Error actualizando perfil", Toast.LENGTH_SHORT).show()
                );
    }

    // ---------------- Métodos de cifrado con Android Keystore ----------------

    @RequiresApi(api = Build.VERSION_CODES.M)
    private SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((SecretKey) keyStore.getKey(KEY_ALIAS, null));
        }
        KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build();
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
        );
        keyGenerator.init(keySpec);
        return keyGenerator.generateKey();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private String encryptString(String plainText) throws Exception {
        // 1) Obtenemos la clave del Keystore (o la creamos si no existe)
        SecretKey key = getOrCreateSecretKey();

        // 2) Pedimos un Cipher para AES/GCM, sin IV explícito (el Keystore lo genera internamente)
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        // 3) Recuperamos el IV que se generó dentro del Keystore
        byte[] iv = cipher.getIV(); // longitud = GCM_IV_LENGTH = 12

        // 4) Ciframos el texto plano
        byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 5) Concatenamos IV + ciphertext
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherBytes);
        byte[] combined = byteBuffer.array();

        // 6) Codificamos a Base64 y devolvemos la cadena
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }
    // ---------------------------------------------------------------------------------------
}
