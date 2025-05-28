package edu.example.daniellopezjarilloproyecto2025.ui.perfil;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import edu.example.daniellopezjarilloproyecto2025.databinding.FragmentEditProfileBinding;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding b;
    private Uri                      pickedImageUri;
    private String                   currentPhotoUrl;
    private final FirebaseFirestore  db   = FirebaseFirestore.getInstance();
    private final FirebaseUser       user = FirebaseAuth.getInstance().getCurrentUser();
    private boolean                  isVip;

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

    @Nullable @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             android.view.ViewGroup container,
                             Bundle savedInstanceState) {
        b = FragmentEditProfileBinding.inflate(inflater, container, false);

        if (user == null) {
            Toast.makeText(requireContext(), "No autenticado", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return b.getRoot();
        }

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

                    // Rellenar campos
                    b.etName.setText(doc.getString("name"));
                    b.etPhone.setText(doc.getString("phone"));
                    b.etAddress.setText(doc.getString("address"));
                    String url = doc.getString("photoUrl");
                    if (url != null) {
                        currentPhotoUrl = url;
                        Glide.with(this).load(url).into(b.ivProfileImage);
                    }

                    // Si NO es VIP, deshabilitar edición
                    if (!isVip) {
                        b.btnSelectImage.setVisibility(View.GONE);
                        b.btnSave       .setVisibility(View.GONE);
                        b.etName  .setEnabled(false);
                        b.etPhone .setEnabled(false);
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

        String name    = b.etName.getText().toString().trim();
        String phone   = b.etPhone.getText().toString().trim();
        String address = b.etAddress.getText().toString().trim();

        Map<String,Object> updates = new HashMap<>();
        updates.put("name",    name);
        updates.put("phone",   phone);
        updates.put("address", address);
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
}
