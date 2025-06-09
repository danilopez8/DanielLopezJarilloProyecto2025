package edu.example.daniellopezjarilloproyecto2025.ui.perfil;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;

import java.io.ByteArrayOutputStream;
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

import edu.example.daniellopezjarilloproyecto2025.databinding.FragmentEditProfileBinding;

public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";

    private FragmentEditProfileBinding b;
    private Uri pickedImageUri;
    private String currentPhotoUrl;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private boolean isVip;

    // Parámetros para AES/GCM en Android Keystore
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS        = "TFG_KEY_ALIAS";
    private static final int    GCM_IV_LENGTH    = 12;    // 12 bytes de IV
    private static final int    GCM_TAG_LENGTH   = 128;   // 128 bits de tag
    private static final String TRANSFORMATION   = "AES/GCM/NoPadding";

    // Cámara
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Object extras = result.getData().getExtras().get("data");
                            if (extras instanceof Bitmap) {
                                Bitmap bitmap = (Bitmap) extras;
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                                String path = MediaStore.Images.Media.insertImage(
                                        requireContext().getContentResolver(),
                                        bitmap, "captured", null
                                );
                                pickedImageUri = Uri.parse(path);
                                b.ivProfileImage.setImageBitmap(bitmap);
                            }
                        }
                    }
            );

    // Galería
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            pickedImageUri = uri;
                            b.ivProfileImage.setImageURI(uri);
                        }
                    }
            );

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull android.view.LayoutInflater inflater,
                                          android.view.ViewGroup container,
                                          Bundle savedInstanceState) {
        b = FragmentEditProfileBinding.inflate(inflater, container, false);

        if (user == null) {
            Toast.makeText(requireContext(), "No autenticado", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return b.getRoot();
        }

        b.ccp.registerCarrierNumberEditText(b.etPhone);

        // 1) Cargo datos + VIP
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(requireContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // VIP?
                    Boolean vipFlag = doc.getBoolean("vip");
                    isVip = Boolean.TRUE.equals(vipFlag);

                    // Rellenar campo nombre (sin cifrar)
                    b.etName.setText(doc.getString("name"));

                    // ---- Descifrar teléfono si existe ----
                    String telefonoGuardado = doc.getString("phone");
                    if (telefonoGuardado != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                String decryptedPhone = decryptString(telefonoGuardado);
                                b.ccp.setFullNumber(decryptedPhone);
                            } catch (Exception e) {
                                Log.e(TAG, "Error descifrando teléfono:", e);
                                Toast.makeText(requireContext(),
                                        "Error al descifrar teléfono: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                b.ccp.setFullNumber("");
                            }
                        } else {
                            // Versiones < M: asignamos tal cual (no había cifrado)
                            b.ccp.setFullNumber(telefonoGuardado);
                        }
                    }

                    // ---- Descifrar dirección si existe ----
                    String addressGuardada = doc.getString("address");
                    if (addressGuardada != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                String decryptedAddress = decryptString(addressGuardada);
                                b.etAddress.setText(decryptedAddress);
                            } catch (Exception e) {
                                Log.e(TAG, "Error descifrando dirección:", e);
                                Toast.makeText(requireContext(),
                                        "Error al descifrar dirección: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                b.etAddress.setText("");
                            }
                        } else {
                            // Versiones < M: asignamos tal cual
                            b.etAddress.setText(addressGuardada);
                        }
                    }

                    // Carga de foto (sin cifrar)
                    String url = doc.getString("photoUrl");
                    if (url != null) {
                        currentPhotoUrl = url;
                        Glide.with(this).load(url).into(b.ivProfileImage);
                    }

                    // Si NO es VIP, deshabilitar edición
                    if (!isVip) {
                        b.btnSelectImage.setVisibility(android.view.View.GONE);
                        b.btnSave.setVisibility(android.view.View.GONE);
                        b.etName.setEnabled(false);
                        b.etPhone.setEnabled(false);
                        b.etAddress.setEnabled(false);
                        Toast.makeText(requireContext(),
                                "Solo usuarios VIP pueden editar perfil", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Error cargando perfil", Toast.LENGTH_SHORT).show()
                );

        // Solo si es VIP permitimos estos listeners
        b.btnSelectImage.setOnClickListener(v -> {
            if (!isVip) return;
            showImagePickerDialog();
        });
        b.btnSave.setOnClickListener(v -> {
            if (!isVip) return;
            saveProfile();
        });

        return b.getRoot();
    }

    private void showImagePickerDialog() {
        String[] options = {"Tomar foto", "Elegir de la Galería"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Seleccionar imagen")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Cámara
                        if (ContextCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.CAMERA) !=
                                PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(
                                    new String[]{Manifest.permission.CAMERA},
                                    1001
                            );
                        } else {
                            openCamera();
                        }
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void openCamera() {
        cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else if (requestCode == 1001) {
            Toast.makeText(requireContext(),
                    "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfile() {
        if (!isVip || user == null) return;

        String name = b.etName.getText().toString().trim();
        String addressPlain = b.etAddress.getText().toString().trim();


        if (!b.ccp.isValidFullNumber()) {
            b.etPhone.setError("Número inválido para " + b.ccp.getSelectedCountryName());
            return;
        }
        String phonePlain = b.ccp.getFullNumberWithPlus(); // ej. "+34123456789"

        Map<String,Object> updates = new HashMap<>();
        updates.put("name", name);

        // Cifrar teléfono y dirección si es posible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                String encryptedPhone   = encryptString(phonePlain);
                String encryptedAddress = encryptString(addressPlain);
                updates.put("phone", encryptedPhone);
                updates.put("address", encryptedAddress);
            } catch (Exception e) {
                Log.e(TAG, "Error cifrando datos:", e);
                Toast.makeText(requireContext(),
                        "Error cifrando datos: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }
        } else {

            updates.put("phone", phonePlain);
            updates.put("address", addressPlain);
        }

        if (pickedImageUri != null) {
            updates.put("photoUrl", pickedImageUri.toString());
        } else if (currentPhotoUrl != null) {
            updates.put("photoUrl", currentPhotoUrl);
        }

        db.collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(a ->
                        Toast.makeText(requireContext(),
                                "Perfil actualizado", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Error guardando perfil", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    // Métodos de cifrado/descifrado con Android Keystore

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
        // Obtenemos la clave del Keystore (o la creamos si no existe)
        SecretKey key = getOrCreateSecretKey();

        // Pedimos un Cipher para AES/GCM, sin IV explícito (el Keystore lo genera internamente)
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        // Recuperamos el IV que se generó dentro del Keystore
        byte[] iv = cipher.getIV(); // longitud = GCM_IV_LENGTH = 12

        // Ciframos el texto plano
        byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Concatenamos IV + ciphertext
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherBytes);
        byte[] combined = byteBuffer.array();

        // Codificamos a Base64 y devolvemos la cadena
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private String decryptString(String base64IvCipher) throws Exception {
        // Obtenemos la misma clave del Keystore
        SecretKey key = getOrCreateSecretKey();

        // Decodificamos Base64 para recuperar IV + ciphertext
        byte[] combined = Base64.decode(base64IvCipher, Base64.NO_WRAP);
        ByteBuffer byteBuffer = ByteBuffer.wrap(combined);

        // Extraemos IV (primeros GCM_IV_LENGTH bytes)
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);

        // Extraemos ciphertext (bytes restantes)
        byte[] cipherBytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherBytes);

        // Creamos el Cipher con el mismo algoritmo y IV
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        // Desciframos y devolvemos el texto plano
        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

}
