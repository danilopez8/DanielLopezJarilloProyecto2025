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


        // Leemos el ID de la reserva que vino en el Intent
        reservationId = getIntent().getStringExtra("reservationId");

        // Leemos todos los demás datos que vinieron en el Intent
        String brand    = getIntent().getStringExtra("brand");
        String model    = getIntent().getStringExtra("model");
        String date     = getIntent().getStringExtra("date");
        String location = getIntent().getStringExtra("location");
        int price       = getIntent().getIntExtra("price", 0);
        List<String> images = getIntent().getStringArrayListExtra("images");

        // Enlazamos vistas
        ViewPager2 viewPager    = findViewById(R.id.viewPagerImagesReserva);
        TextView txtMarca       = findViewById(R.id.txtReservaBrandDetail);
        TextView txtModelo      = findViewById(R.id.txtReservaModelDetail);
        TextView txtFecha       = findViewById(R.id.txtReservaDateDetail);
        TextView txtUbicacion   = findViewById(R.id.txtReservaLocationDetail);
        TextView txtPrecio      = findViewById(R.id.txtReservaPriceDetail);
        Button btnCancelar      = findViewById(R.id.btnCancelReserva);
        Button btnEditar        = findViewById(R.id.btnEditarReserva);

        // Poblamos las vistas con los datos recibidos
        txtMarca.setText("Marca: " + brand);
        txtModelo.setText("Modelo: " + model);
        txtFecha.setText("Fecha: " + date);
        txtUbicacion.setText("Ubicación: " + location);
        txtPrecio.setText("Precio/día: " + price + "€");

        // Si hay imágenes, inicializamos el slider
        if (images != null && !images.isEmpty()) {
            viewPager.setAdapter(new ImageSliderAdapter(this, images));
        }

        // Mapa con la ubicación (si se envió lat/lng en el Intent)
        SupportMapFragment mapFrag = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapReserva);
        if (mapFrag != null) {
            mapFrag.getMapAsync(this);
        }

        // Configuramos el botón "Cancelar"
        btnCancelar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Cancelar reserva")
                    .setMessage("¿Seguro que quieres cancelar esta reserva?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        // Borrar del calendario y de Firestore
                        FirebaseFirestore.getInstance()
                                .collection("reservas")                            // colección "reservas"
                                .document(reservationId)
                                .get()
                                .addOnSuccessListener(doc -> {
                                    // Borrar del calendario si tiene eventId y permisos
                                    if (doc.contains("eventId") && ensureCalendarPermission()) {
                                        Long eventId = doc.getLong("eventId");
                                        if (eventId != null) {
                                            Uri deleteUri = ContentUris.withAppendedId(
                                                    CalendarContract.Events.CONTENT_URI, eventId);
                                            getContentResolver().delete(deleteUri, null, null);
                                        }
                                    }
                                    // Borrar el documento en Firestore
                                    FirebaseFirestore.getInstance()
                                            .collection("reservas")                        // colección "reservas"
                                            .document(reservationId)
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(this, "Reserva cancelada", Toast.LENGTH_SHORT).show();
                                                finish(); // Cierra la Activity
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Error al cancelar reserva", Toast.LENGTH_SHORT).show()
                                            );
                                });
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        // Configuramos el botón "Editar"
        btnEditar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Editar reserva")
                    .setMessage("¿Quieres cambiar el rango de fechas de esta reserva?")
                    .setPositiveButton("Sí", (dialog, which) -> showDateRangePickerForEdit())
                    .setNegativeButton("No", null)
                    .show();
        });

        // Pedimos permiso de calendario si no lo tenemos
        ensureCalendarPermission();
    }

    @Override
    public void onMapReady(GoogleMap gm) {
        // Tomamos lat/lng y city del Intent para situar el marcador

        double lat = getIntent().getDoubleExtra("lat", 0.0);
        double lng = getIntent().getDoubleExtra("lng", 0.0);
        String city = getIntent().getStringExtra("location");
        LatLng loc = new LatLng(lat, lng);
        gm.addMarker(new MarkerOptions().position(loc).title("Ubicación: " + city));
        gm.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 12f));
    }

    /**
     * Muestra un DateRangePicker para editar el rango de fechas.
     * Primero carga todas las reservas existentes para este mismo coche
     * (filtrado en Firestore por carBrand y carModel) y bloquea esas fechas.
     */
    private void showDateRangePickerForEdit() {
        long todayUtc = MaterialDatePicker.todayInUtcMilliseconds();

        // Obtenemos brand/model desde el Intent original
        String brandExtra = getIntent().getStringExtra("brand");
        String modelExtra = getIntent().getStringExtra("model");

        // Cargamos todas las reservas para este coche (colección "reservas")
        FirebaseFirestore.getInstance()
                .collection("reservas")                                           // "reservas"
                .whereEqualTo("carBrand", brandExtra)
                .whereEqualTo("carModel", modelExtra)
                .get()
                .addOnSuccessListener(qs -> {
                    // Recolectamos en un Set todas las fechas ya reservadas
                    Set<String> reserved = new HashSet<>();
                    SimpleDateFormat fmt = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : qs) {
                        // Leemos "startDate" y "endDate"
                        String s = doc.getString("startDate");
                        String e = doc.getString("endDate");
                        if (s == null || e == null) continue;

                        // Convertimos a Calendar y añadimos cada día al Set
                        Calendar cStart = Calendar.getInstance();
                        String[] p1 = s.split("/");
                        cStart.set(Integer.parseInt(p1[2]), Integer.parseInt(p1[1]) - 1, Integer.parseInt(p1[0]), 0, 0, 0);

                        Calendar cEnd = Calendar.getInstance();
                        String[] p2 = e.split("/");
                        cEnd.set(Integer.parseInt(p2[2]), Integer.parseInt(p2[1]) - 1, Integer.parseInt(p2[0]), 0, 0, 0);

                        Calendar iter = (Calendar) cStart.clone();
                        while (!iter.after(cEnd)) {
                            reserved.add(fmt.format(iter.getTime()));
                            iter.add(Calendar.DAY_OF_MONTH, 1);
                        }
                    }

                    // Creamos el DatePicker bloqueando fechas pasadas + reservadas
                    CalendarConstraints.DateValidator futuroValidator = DateValidatorPointForward.now();
                    CalendarConstraints.DateValidator bloqueadasValidator =
                            new CarDetailActivity.ReservedAndFutureDateValidator(todayUtc, reserved);

                    CalendarConstraints constraints = new CalendarConstraints.Builder()
                            .setValidator(CompositeDateValidator.allOf(
                                    java.util.List.of(futuroValidator, bloqueadasValidator)))
                            .build();

                    MaterialDatePicker<Pair<Long, Long>> picker =
                            MaterialDatePicker.Builder.dateRangePicker()
                                    .setTitleText("Selecciona nuevo rango de fechas")
                                    .setCalendarConstraints(constraints)
                                    .build();

                    picker.show(getSupportFragmentManager(), "EDITAR_RANGO");
                    picker.addOnPositiveButtonClickListener(selection -> {
                        SimpleDateFormat fmt2 = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                        Calendar cStart2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        Calendar cEnd2   = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

                        cStart2.setTimeInMillis(selection.first);
                        String newStart = fmt2.format(cStart2.getTime());
                        cEnd2.setTimeInMillis(selection.second);
                        String newEnd   = fmt2.format(cEnd2.getTime());

                        new AlertDialog.Builder(this)
                                .setTitle("Confirmar edición")
                                .setMessage("¿Deseas actualizar la reserva al rango de " + newStart + " al " + newEnd + "?")
                                .setPositiveButton("Sí", (d, w) -> {
                                    // Actualizar evento en CalendarProvider si existe eventId
                                    FirebaseFirestore.getInstance()
                                            .collection("reservas")                               // "reservas"
                                            .document(reservationId)
                                            .get()
                                            .addOnSuccessListener(doc -> {
                                                if (doc.contains("eventId") && ensureCalendarPermission()) {
                                                    Long eventId = doc.getLong("eventId");
                                                    if (eventId != null) {
                                                        ContentValues vals = new ContentValues();
                                                        vals.put(CalendarContract.Events.DTSTART, cStart2.getTimeInMillis());
                                                        vals.put(CalendarContract.Events.DTEND,   cEnd2.getTimeInMillis());
                                                        Uri updateUri = ContentUris.withAppendedId(
                                                                CalendarContract.Events.CONTENT_URI, eventId);
                                                        getContentResolver().update(updateUri, vals, null, null);
                                                    }
                                                }
                                                // Actualizar startDate/endDate en Firestore
                                                FirebaseFirestore.getInstance()
                                                        .collection("reservas")                       // "reservas"
                                                        .document(reservationId)
                                                        .update("startDate", newStart, "endDate", newEnd)
                                                        .addOnSuccessListener(a -> {
                                                            Toast.makeText(this, "Reserva actualizada", Toast.LENGTH_SHORT).show();
                                                            TextView txtFecha = findViewById(R.id.txtReservaDateDetail);
                                                            txtFecha.setText("Del " + newStart + " al " + newEnd);
                                                        })
                                                        .addOnFailureListener(e ->
                                                                Toast.makeText(this, "Error al actualizar reserva", Toast.LENGTH_SHORT).show()
                                                        );
                                            })
                                            .addOnFailureListener(e1 ->
                                                    Toast.makeText(this, "Error al leer reserva para editar", Toast.LENGTH_SHORT).show()
                                            );
                                })
                                .setNegativeButton("No", null)
                                .show();
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "No se pudieron cargar reservas para edición", Toast.LENGTH_SHORT).show()
                );
    }

    /** Asegura que tenemos permisos de calendario WRITE/READ en tiempo de ejecución. */
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
