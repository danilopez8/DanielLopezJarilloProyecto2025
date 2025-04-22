package edu.example.daniellopezjarilloproyecto2025.ui.concesionario;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.example.daniellopezjarilloproyecto2025.R;

public class CarDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ViewPager2 viewPager;
    private TextView brandText, modelText, yearText, priceText;

    private double carLat;
    private double carLng;
    private String carCity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_detail);

        // Referencias a vistas
        viewPager = findViewById(R.id.viewPagerImages);
        brandText = findViewById(R.id.txtDetailBrand);
        modelText = findViewById(R.id.txtDetailModel);
        yearText = findViewById(R.id.txtDetailYear);
        priceText = findViewById(R.id.txtDetailPrice);

        // Obtener datos del intent
        String brand = getIntent().getStringExtra("car_brand");
        String model = getIntent().getStringExtra("car_model");
        int year = getIntent().getIntExtra("car_year", 0);
        int price = getIntent().getIntExtra("car_price", 0);
        List<String> images = getIntent().getStringArrayListExtra("car_images");

        // Coordenadas de ubicación
        carLat = getIntent().getDoubleExtra("car_lat", 0.0);
        carLng = getIntent().getDoubleExtra("car_lng", 0.0);
        carCity = getIntent().getStringExtra("car_city");

        // Mostrar datos
        brandText.setText("Marca: " + brand);
        modelText.setText("Modelo: " + model);
        yearText.setText("Año: " + year);
        priceText.setText("Precio: " + price + "€/día");

        // Adaptador del ViewPager2 para imágenes
        viewPager.setAdapter(new ImageSliderAdapter(this, images));

        // Botón reservar
        Button btnReserve = findViewById(R.id.btnReserveCar);
        btnReserve.setOnClickListener(v -> showDatePickerDialog());

        // Inicializar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                    sendReservationEmail(selectedDate);
                    saveReservationToFirestore(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void sendReservationEmail(String date) {
        String subject = "Reserva de coche: " + modelText.getText().toString();
        String body = "Has reservado el coche:\n\n" +
                "Marca: " + brandText.getText().toString() + "\n" +
                "Modelo: " + modelText.getText().toString() + "\n" +
                "Año: " + yearText.getText().toString() + "\n" +
                "Precio por día: " + priceText.getText().toString() + "\n" +
                "Ubicación: " + carCity + "\n" +
                "Fecha de reserva: " + date + "\n\n" +
                "Gracias por usar nuestro servicio.";

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{user.getEmail()});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, body);

            try {
                startActivity(Intent.createChooser(emailIntent, "Elige app de correo..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No hay apps de correo instaladas.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No se pudo obtener el correo del usuario", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveReservationToFirestore(String date) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Recuperar las imágenes desde el intent
        List<String> images = getIntent().getStringArrayListExtra("car_images");

        Map<String, Object> reservation = new HashMap<>();
        reservation.put("userId", currentUser.getUid());
        reservation.put("email", currentUser.getEmail());
        reservation.put("carBrand", brandText.getText().toString());
        reservation.put("carModel", modelText.getText().toString());
        reservation.put("reservationDate", date);
        reservation.put("location", carCity);
        reservation.put("carPrice", getIntent().getIntExtra("car_price", 0)); // también guarda precio si lo usas
        reservation.put("images", images);
        reservation.put("timestamp", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("reservations")
                .add(reservation)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Reserva guardada correctamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar reserva", Toast.LENGTH_SHORT).show();
                });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng carLocation = new LatLng(carLat, carLng);
        googleMap.addMarker(new MarkerOptions().position(carLocation).title("Ubicación: " + carCity));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carLocation, 12f));
    }
}
