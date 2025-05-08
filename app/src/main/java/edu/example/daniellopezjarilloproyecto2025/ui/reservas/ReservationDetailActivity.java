package edu.example.daniellopezjarilloproyecto2025.ui.reservas;

import android.content.Intent;
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
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import edu.example.daniellopezjarilloproyecto2025.R;
import edu.example.daniellopezjarilloproyecto2025.ui.concesionario.CarDetailActivity;
import edu.example.daniellopezjarilloproyecto2025.ui.concesionario.ImageSliderAdapter;

public class ReservationDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private String reservationId;

    @Override
    protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_reservation_detail);

        // 1) Recupero extras
        reservationId = getIntent().getStringExtra("reservationId");
        String brand    = getIntent().getStringExtra("brand");
        String model    = getIntent().getStringExtra("model");
        String date     = getIntent().getStringExtra("date");
        String location = getIntent().getStringExtra("location");
        int price       = getIntent().getIntExtra("price", 0);
        List<String> images = getIntent().getStringArrayListExtra("images");

        // 2) Referencias a vistas
        ViewPager2 viewPager    = findViewById(R.id.viewPagerImagesReserva);
        TextView txtMarca       = findViewById(R.id.txtReservaBrandDetail);
        TextView txtModelo      = findViewById(R.id.txtReservaModelDetail);
        TextView txtFecha       = findViewById(R.id.txtReservaDateDetail);
        TextView txtUbicacion   = findViewById(R.id.txtReservaLocationDetail);
        TextView txtPrecio      = findViewById(R.id.txtReservaPriceDetail);
        Button btnCancelar      = findViewById(R.id.btnCancelReserva);

        // 3) Muestro datos
        txtMarca    .setText("Marca: "      + brand);
        txtModelo   .setText("Modelo: "     + model);
        txtFecha    .setText("Fecha: "      + date);
        txtUbicacion.setText("Ubicación: "  + location);
        txtPrecio   .setText("Precio/día: " + price + "€");

        // 4) Inicializo carrusel de imágenes
        if (images != null && !images.isEmpty()) {
            viewPager.setAdapter(new ImageSliderAdapter(this, images));
        }

        // 5) Inicializo mapa
        SupportMapFragment mapFrag = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapReserva);
        if (mapFrag != null) {
            mapFrag.getMapAsync(this);
        }

        // 6) Botón cancelar reserva
        btnCancelar.setOnClickListener(v -> {
            FirebaseFirestore.getInstance()
                    .collection("reservations")
                    .document(reservationId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Reserva cancelada", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error al cancelar reserva", Toast.LENGTH_SHORT).show()
                    );
        });
        Button btnEditar = findViewById(R.id.btnEditarReserva);

        btnEditar.setOnClickListener(v -> {
            showDatePickerForEdit();
        });

    }

    @Override
    public void onMapReady(GoogleMap gm) {
        // Muestra el marcador en la ubicación de la reserva (si la pasaste como extras)
        double lat = getIntent().getDoubleExtra("lat", 0.0);
        double lng = getIntent().getDoubleExtra("lng", 0.0);
        String city = getIntent().getStringExtra("location");

        LatLng loc = new LatLng(lat, lng);
        gm.addMarker(new MarkerOptions().position(loc).title("Ubicación: " + city));
        gm.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 12f));
    }

    private void showDatePickerForEdit() {
        long todayUtc = MaterialDatePicker.todayInUtcMilliseconds();

        FirebaseFirestore.getInstance()
                .collection("reservations")
                .whereEqualTo("carBrand", getIntent().getStringExtra("brand"))
                .whereEqualTo("carModel", getIntent().getStringExtra("model"))
                .get()
                .addOnSuccessListener(qs -> {
                    Set<String> fechasReservadas = new HashSet<>();
                    for (QueryDocumentSnapshot doc : qs) {
                        String f = doc.getString("reservationDate");
                        if (f != null) fechasReservadas.add(f);
                    }

                    CalendarConstraints.DateValidator validator =
                            new CarDetailActivity.ReservedAndFutureDateValidator(todayUtc, fechasReservadas);

                    MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Selecciona nueva fecha")
                            .setCalendarConstraints(new CalendarConstraints.Builder()
                                    .setValidator(validator)
                                    .build())
                            .build();

                    picker.show(getSupportFragmentManager(), "EDITAR_FECHA");

                    picker.addOnPositiveButtonClickListener(selection -> {
                        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        c.setTimeInMillis(selection);
                        String nuevaFecha = c.get(Calendar.DAY_OF_MONTH) + "/" +
                                (c.get(Calendar.MONTH) + 1) + "/" +
                                c.get(Calendar.YEAR);

                        FirebaseFirestore.getInstance()
                                .collection("reservations")
                                .document(reservationId)
                                .update("reservationDate", nuevaFecha)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Fecha actualizada", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show();
                                });
                    });
                });
    }


}
