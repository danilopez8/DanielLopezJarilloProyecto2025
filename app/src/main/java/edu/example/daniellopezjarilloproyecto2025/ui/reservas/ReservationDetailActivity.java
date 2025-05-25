// ReservationDetailActivity.java
package edu.example.daniellopezjarilloproyecto2025.ui.reservas;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.Locale;

import edu.example.daniellopezjarilloproyecto2025.R;
import edu.example.daniellopezjarilloproyecto2025.ui.concesionario.CarDetailActivity;
import edu.example.daniellopezjarilloproyecto2025.ui.concesionario.ImageSliderAdapter;

public class ReservationDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int REQUEST_CALENDAR = 43;
    private String reservationId;

    @Override
    protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_reservation_detail);

        reservationId = getIntent().getStringExtra("reservationId");
        String brand    = getIntent().getStringExtra("brand");
        String model    = getIntent().getStringExtra("model");
        String date     = getIntent().getStringExtra("date");
        String location = getIntent().getStringExtra("location");
        int price       = getIntent().getIntExtra("price", 0);
        List<String> images = getIntent().getStringArrayListExtra("images");

        ViewPager2 viewPager    = findViewById(R.id.viewPagerImagesReserva);
        TextView txtMarca       = findViewById(R.id.txtReservaBrandDetail);
        TextView txtModelo      = findViewById(R.id.txtReservaModelDetail);
        TextView txtFecha       = findViewById(R.id.txtReservaDateDetail);
        TextView txtUbicacion   = findViewById(R.id.txtReservaLocationDetail);
        TextView txtPrecio      = findViewById(R.id.txtReservaPriceDetail);
        Button btnCancelar      = findViewById(R.id.btnCancelReserva);
        Button btnEditar        = findViewById(R.id.btnEditarReserva);

        txtMarca.setText("Marca: " + brand);
        txtModelo.setText("Modelo: " + model);
        txtFecha.setText("Fecha: " + date);
        txtUbicacion.setText("Ubicación: " + location);
        txtPrecio.setText("Precio/día: " + price + "€");

        if (images != null && !images.isEmpty()) {
            viewPager.setAdapter(new ImageSliderAdapter(this, images));
        }

        SupportMapFragment mapFrag = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapReserva);
        if (mapFrag != null) mapFrag.getMapAsync(this);

        btnCancelar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Cancelar reserva")
                    .setMessage("¿Seguro que quieres cancelar esta reserva?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        FirebaseFirestore.getInstance()
                                .collection("reservations")
                                .document(reservationId)
                                .get()
                                .addOnSuccessListener(doc -> {
                                    // 1) Borrar del calendario si tenemos eventId y permisos
                                    if (doc.contains("eventId") && ensureCalendarPermission()) {
                                        long eventId = doc.getLong("eventId");
                                        Uri deleteUri = ContentUris.withAppendedId(
                                                CalendarContract.Events.CONTENT_URI, eventId);
                                        getContentResolver().delete(deleteUri, null, null);
                                    }
                                    // 2) Borrar el documento
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
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        btnEditar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Editar reserva")
                    .setMessage("¿Quieres cambiar el rango de fechas de esta reserva?")
                    .setPositiveButton("Sí", (dialog, which) -> showDateRangePickerForEdit())
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    @Override
    public void onMapReady(GoogleMap gm) {
        double lat = getIntent().getDoubleExtra("lat", 0.0);
        double lng = getIntent().getDoubleExtra("lng", 0.0);
        String city = getIntent().getStringExtra("location");
        LatLng loc = new LatLng(lat, lng);
        gm.addMarker(new MarkerOptions().position(loc).title("Ubicación: " + city));
        gm.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 12f));
    }

    private void showDateRangePickerForEdit() {
        long todayUtc = MaterialDatePicker.todayInUtcMilliseconds();
        FirebaseFirestore.getInstance()
                .collection("reservations")
                .whereEqualTo("carBrand", getIntent().getStringExtra("brand"))
                .whereEqualTo("carModel", getIntent().getStringExtra("model"))
                .get()
                .addOnSuccessListener(qs -> {
                    Set<String> reserved = new HashSet<>();
                    for (QueryDocumentSnapshot doc : qs) {
                        String f = doc.getString("reservationDate");
                        if (f != null) reserved.add(f);
                    }
                    CalendarConstraints.DateValidator validator =
                            new CarDetailActivity.ReservedAndFutureDateValidator(todayUtc, reserved);

                    CalendarConstraints constraints = new CalendarConstraints.Builder()
                            .setValidator(CompositeDateValidator.allOf(List.of(
                                    DateValidatorPointForward.now(), validator
                            )))
                            .build();

                    MaterialDatePicker<Pair<Long, Long>> picker =
                            MaterialDatePicker.Builder.dateRangePicker()
                                    .setTitleText("Selecciona nuevo rango de fechas")
                                    .setCalendarConstraints(constraints)
                                    .build();

                    picker.show(getSupportFragmentManager(), "EDITAR_RANGO");
                    picker.addOnPositiveButtonClickListener(selection -> {
                        SimpleDateFormat fmt = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                        Calendar cStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        cStart.setTimeInMillis(selection.first);
                        String start = fmt.format(cStart.getTime());
                        Calendar cEnd = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        cEnd.setTimeInMillis(selection.second);
                        String end = fmt.format(cEnd.getTime());

                        new AlertDialog.Builder(this)
                                .setTitle("Confirmar edición")
                                .setMessage("¿Deseas actualizar la reserva al rango de " + start + " al " + end + "?")
                                .setPositiveButton("Sí", (d,w) -> {
                                    // 1) Actualizar evento en calendario
                                    FirebaseFirestore.getInstance()
                                            .collection("reservations")
                                            .document(reservationId)
                                            .get()
                                            .addOnSuccessListener(doc -> {
                                                if (doc.contains("eventId") && ensureCalendarPermission()) {
                                                    long eventId = doc.getLong("eventId");
                                                    ContentValues vals = new ContentValues();
                                                    vals.put(CalendarContract.Events.DTSTART, cStart.getTimeInMillis());
                                                    vals.put(CalendarContract.Events.DTEND,   cEnd.getTimeInMillis());
                                                    Uri updateUri = ContentUris.withAppendedId(
                                                            CalendarContract.Events.CONTENT_URI, eventId);
                                                    getContentResolver().update(updateUri, vals, null, null);
                                                }
                                                // 2) Actualizar Firestore
                                                FirebaseFirestore.getInstance()
                                                        .collection("reservations")
                                                        .document(reservationId)
                                                        .update("startDate", start, "endDate", end)
                                                        .addOnSuccessListener(a -> {
                                                            Toast.makeText(this, "Reserva actualizada", Toast.LENGTH_SHORT).show();
                                                            TextView txtFecha = findViewById(R.id.txtReservaDateDetail);
                                                            txtFecha.setText("Del " + start + " al " + end);
                                                        })
                                                        .addOnFailureListener(e ->
                                                                Toast.makeText(this, "Error al actualizar reserva", Toast.LENGTH_SHORT).show()
                                                        );
                                            });
                                })
                                .setNegativeButton("No", null)
                                .show();
                    });
                });
    }

    /** Asegura que tenemos WRITE/READ_CALENDAR en tiempo de ejecución. */
    private boolean ensureCalendarPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR },
                    REQUEST_CALENDAR);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALENDAR) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                Toast.makeText(this, "Permiso de calendario denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
