package edu.example.daniellopezjarilloproyecto2025;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hbb20.CountryCodePicker;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class RegisterVipActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST_CODE = 2;

    private EditText editName, editEmail, editPassword, editConfirmPassword, editPhone;
    private Button btnRegisterVip, btnSelectImage;
    private ImageView imageViewProfile;
    private CountryCodePicker countryCodePicker;

    private Uri imageUri;
    private Bitmap profileBitmap; // guarda temporalmente la imagen
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_vip);

        // Inicializamos Firebase y componentes
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Referencias a vistas
        editName = findViewById(R.id.editTextVipName);
        editEmail = findViewById(R.id.editTextVipEmail);
        editPassword = findViewById(R.id.editTextVipPassword);
        editConfirmPassword = findViewById(R.id.editTextVipConfirmPassword);
        editPhone = findViewById(R.id.editTextVipPhone);
        countryCodePicker = findViewById(R.id.countryCodePicker);
        countryCodePicker.registerCarrierNumberEditText(editPhone);

        btnRegisterVip = findViewById(R.id.btnRegisterVip);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        imageViewProfile = findViewById(R.id.imageViewProfile);

        // Selección de imagen
        btnSelectImage.setOnClickListener(v -> showImagePickerDialog());

        // Botón registrar
        btnRegisterVip.setOnClickListener(v -> registerUser());
    }

    // Muestra un diálogo para elegir entre cámara o galería
    private void showImagePickerDialog() {
        String[] options = {"Cámara", "Galería"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Seleccionar imagen desde")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
                    } else {
                        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
                    }
                }).show();
    }

    // Devuelve el resultado de imagen seleccionada
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                imageUri = data.getData();
                imageViewProfile.setImageURI(imageUri);
                profileBitmap = uriToBitmap(imageUri);
            } else if (requestCode == CAMERA_REQUEST_CODE) {
                profileBitmap = (Bitmap) data.getExtras().get("data");
                imageViewProfile.setImageBitmap(profileBitmap);
            }
        }
    }

    // Convierte una Uri de imagen en Bitmap
    private Bitmap uriToBitmap(Uri uri) {
        try {
            return MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        } catch (Exception e) {
            return null;
        }
    }

    private void registerUser() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!countryCodePicker.isValidFullNumber()) {
            Toast.makeText(this, "Número de teléfono inválido para el país seleccionado", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        if (profileBitmap != null) {
                            uploadImageToFirebaseStorage(user.getUid(), name, email, phone);
                        } else {
                            saveUserToFirestore(user.getUid(), name, email, phone, null);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RegisterVIP", "Error al registrar", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    // Guarda la imagen codificada en Base64 en SharedPreferences
    private void saveImageToPreferences() {
        if (profileBitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            profileBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("PROFILE_IMAGE", encodedImage)
                    .apply();
        }
    }

    private void saveUserToFirestore(String uid, String name, String email, String phone, @Nullable String photoUrl) {
        String fullPhone = countryCodePicker.getSelectedCountryCodeWithPlus() + phone;

        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", uid);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("vip", true);
        userData.put("phone", encrypt(fullPhone));
        if (photoUrl != null) {
            userData.put("photoUrl", photoUrl);  // aquí se guarda
        }

        firestore.collection("users").document(uid)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error guardando usuario", e));
    }



    // Cifra el número de teléfono con SHA-256
    private String encrypt(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            return Base64.encodeToString(hash, Base64.DEFAULT).trim();
        } catch (Exception e) {
            return input;
        }
    }

    private void uploadImageToFirebaseStorage(String uid, String name, String email, String phone) {
        StorageReference imageRef = storageRef.child("profile_images/" + uid + ".jpg");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        profileBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();

        imageRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            saveUserToFirestore(uid, name, email, phone, downloadUrl);
                        }))
                .addOnFailureListener(e -> {
                    Log.e("FirebaseStorage", "Error al subir imagen", e);
                    saveUserToFirestore(uid, name, email, phone, null);
                });
    }


}
