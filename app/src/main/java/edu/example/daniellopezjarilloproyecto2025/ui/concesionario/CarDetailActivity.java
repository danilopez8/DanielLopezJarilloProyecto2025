package edu.example.daniellopezjarilloproyecto2025.ui.concesionario;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import edu.example.daniellopezjarilloproyecto2025.R;

public class CarDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_CALENDAR = 42;

    private ViewPager2 viewPager;
    private TextView brandText, modelText, yearText, priceText, fechasText;
    private String brand, model, carCity;
    private double carLat, carLng;
    private Set<String> fechasReservadas = new HashSet<>();
    private int carPrice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_detail);

        // Pedimos permiso de calendario tan pronto abrimos la pantalla
        ensureCalendarPermission();

        viewPager  = findViewById(R.id.viewPagerImages);
        brandText  = findViewById(R.id.txtDetailBrand);
        modelText  = findViewById(R.id.txtDetailModel);
        yearText   = findViewById(R.id.txtDetailYear);
        priceText  = findViewById(R.id.txtDetailPrice);
        fechasText = findViewById(R.id.txtDetailFechas); // nuevo TextView para rango

        // Leo datos del Intent
        brand   = getIntent().getStringExtra("car_brand");
        model   = getIntent().getStringExtra("car_model");
        int year = getIntent().getIntExtra("car_year",  0);
        carPrice = getIntent().getIntExtra("car_price", 0);
        List<String> images = getIntent().getStringArrayListExtra("car_images");
        carLat  = getIntent().getDoubleExtra("car_lat", 0.0);
        carLng  = getIntent().getDoubleExtra("car_lng", 0.0);
        carCity = getIntent().getStringExtra("car_city");

        // Muestro datos en pantalla
        brandText.setText("Marca: " + brand);
        modelText.setText("Modelo: " + model);
        yearText.setText("Año: " + year);
        priceText.setText("Precio: " + carPrice + "€/día");

        // Inicializo el slider de imágenes
        viewPager.setAdapter(new ImageSliderAdapter(this, images));

        // Botón de reserva
        Button btnReserve = findViewById(R.id.btnReserveCar);
        btnReserve.setEnabled(false);
        btnReserve.setOnClickListener(v -> mostrarSelectorRango());

        // Mapa
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // Cargo las fechas ya reservadas desde Firestore
        cargarFechasReservadas();
    }

    private void cargarFechasReservadas() {
        FirebaseFirestore.getInstance()
                .collection("reservations")
                .whereEqualTo("carBrand", brand)
                .whereEqualTo("carModel", model)
                .get()
                .addOnSuccessListener(qs -> {
                    SimpleDateFormat fmt = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                    for (QueryDocumentSnapshot doc : qs) {
                        if (doc.contains("reservationDate")) {
                            String d = doc.getString("reservationDate");
                            if (d != null) fechasReservadas.add(d);
                        } else if (doc.contains("startDate") && doc.contains("endDate")) {
                            String start = doc.getString("startDate");
                            String end   = doc.getString("endDate");
                            Calendar cStart = Calendar.getInstance();
                            String[] p1 = start.split("/");
                            cStart.set(Integer.parseInt(p1[2]), Integer.parseInt(p1[1]) - 1, Integer.parseInt(p1[0]), 0, 0, 0);
                            Calendar cEnd = Calendar.getInstance();
                            String[] p2 = end.split("/");
                            cEnd.set(Integer.parseInt(p2[2]), Integer.parseInt(p2[1]) - 1, Integer.parseInt(p2[0]), 0, 0, 0);
                            Calendar iter = (Calendar) cStart.clone();
                            while (!iter.after(cEnd)) {
                                fechasReservadas.add(fmt.format(iter.getTime()));
                                iter.add(Calendar.DAY_OF_MONTH, 1);
                            }
                        }
                    }
                    findViewById(R.id.btnReserveCar).setEnabled(true);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "No se pudieron cargar reservas previas", Toast.LENGTH_SHORT).show()
                );
    }

    private void mostrarSelectorRango() {
        long hoyUtc = MaterialDatePicker.todayInUtcMilliseconds();
        CalendarConstraints.DateValidator futuroValidator = DateValidatorPointForward.now();
        CalendarConstraints.DateValidator bloqueadasValidator =
                new CarDetailActivity.ReservedAndFutureDateValidator(hoyUtc, fechasReservadas);

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(CompositeDateValidator.allOf(List.of(futuroValidator, bloqueadasValidator)))
                .build();

        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Selecciona rango de fechas")
                        .setCalendarConstraints(constraints)
                        .build();

        picker.show(getSupportFragmentManager(), "RANGO_PICKER");
        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat fmt = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
            Calendar cStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            Calendar cEnd   = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

            cStart.setTimeInMillis(selection.first);
            String start = fmt.format(cStart.getTime());
            cEnd.setTimeInMillis(selection.second);
            String end   = fmt.format(cEnd.getTime());

            Calendar check = (Calendar) cStart.clone();
            while (!check.after(cEnd)) {
                String day = fmt.format(check.getTime());
                if (fechasReservadas.contains(day)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Rango inválido")
                            .setMessage("El rango incluye una fecha ya reservada:\n" + day)
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
                check.add(Calendar.DAY_OF_MONTH, 1);
            }

            new AlertDialog.Builder(this)
                    .setTitle("Confirmar reserva")
                    .setMessage("¿Deseas reservar del " + start + " al " + end + "?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        fechasText.setText("Del " + start + " al " + end);
                        enviarCorreoReserva(start, end);
                        guardarReservaEnFirestore(start, end);
                        Calendar iter = (Calendar) cStart.clone();
                        while (!iter.after(cEnd)) {
                            fechasReservadas.add(fmt.format(iter.getTime()));
                            iter.add(Calendar.DAY_OF_MONTH, 1);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void enviarCorreoReserva(String start, String end) {
        String asunto = "Reserva coche: " + model;
        String cuerpo = "Has reservado el coche:\n" +
                "Marca: " + brand + "\n" +
                "Modelo: " + model + "\n" +
                "Precio: " + priceText.getText() + "\n" +
                "Ubicación: " + carCity + "\n" +
                "Fechas: Del " + start + " al " + end;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{ user.getEmail() });
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, asunto);
            emailIntent.putExtra(Intent.EXTRA_TEXT, cuerpo);
            try {
                startActivity(Intent.createChooser(emailIntent, "Elige app correo"));
            } catch (Exception ex) {
                Toast.makeText(this, "No hay apps de correo.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarReservaEnFirestore(String start, String end) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;

        Map<String,Object> reserva = new HashMap<>();
        reserva.put("userId",    u.getUid());
        reserva.put("email",     u.getEmail());
        reserva.put("carBrand",  brand);
        reserva.put("carModel",  model);
        reserva.put("startDate", start);
        reserva.put("endDate",   end);
        reserva.put("location",  carCity);
        reserva.put("car_price", carPrice);
        reserva.put("car_images", getIntent().getStringArrayListExtra("car_images"));
        reserva.put("timestamp", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("reservations")
                .add(reserva)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Reserva guardada correctamente", Toast.LENGTH_SHORT).show();
                    // Insertar en calendario sólo si ya tenemos permiso
                    if (!ensureCalendarPermission()) return;
                    try {
                        SimpleDateFormat fmt = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                        Date dStart = fmt.parse(start);
                        Date dEnd   = fmt.parse(end);
                        if (dStart != null && dEnd != null) {
                            long eventId = insertEventToCalendar(
                                    dStart.getTime(),
                                    dEnd.getTime(),
                                    "LuxeDrive: " + brand + " " + model,
                                    carCity
                            );
                            if (eventId != -1) {
                                // Guardar el eventId en Firestore para luego poder borrarlo o editarlo
                                FirebaseFirestore.getInstance()
                                        .collection("reservations")
                                        .document(docRef.getId())
                                        .update("eventId", eventId);
                            }
                        }
                    } catch (ParseException ignored) {}
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error guardando reserva", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onMapReady(GoogleMap gm) {
        LatLng loc = new LatLng(carLat, carLng);
        gm.addMarker(new MarkerOptions().position(loc).title("Ubicación: " + carCity));
        gm.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 12f));
    }

    /** Inserta directamente en el CalendarProvider y devuelve el eventId. */
    private long insertEventToCalendar(long beginMillis, long endMillis, String title, String location) {
        long calId = getPrimaryCalendarId();
        if (calId == -1) {
            Toast.makeText(this, "No hay calendario válido", Toast.LENGTH_SHORT).show();
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, beginMillis);
        values.put(CalendarContract.Events.DTEND,   endMillis);
        values.put(CalendarContract.Events.TITLE,   title);
        values.put(CalendarContract.Events.EVENT_LOCATION, location);
        values.put(CalendarContract.Events.CALENDAR_ID, calId);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        Uri uri = getContentResolver().insert(CalendarContract.Events.CONTENT_URI, values);
        return uri == null ? -1 : Long.parseLong(uri.getLastPathSegment());
    }

    /** Obtiene un CALENDAR_ID válido del dispositivo. */
    private long getPrimaryCalendarId() {
        String[] proj = new String[]{ CalendarContract.Calendars._ID };
        Cursor cursor = getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                proj,
                CalendarContract.Calendars.VISIBLE + " = 1",
                null, null);
        if (cursor != null && cursor.moveToFirst()) {
            long id = cursor.getLong(0);
            cursor.close();
            return id;
        }
        if (cursor != null) cursor.close();
        return -1;
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

    /**
     * Validador que bloquea fechas antes de hoy y las ya reservadas.
     * Implementa Parcelable para DatePicker.
     */
    public static class ReservedAndFutureDateValidator implements CalendarConstraints.DateValidator {
        private final long minDateUtc;
        private final Set<String> reserved;
        public ReservedAndFutureDateValidator(long minDateUtc, Set<String> reserved) {
            this.minDateUtc = minDateUtc;
            this.reserved   = reserved;
        }
        @Override public boolean isValid(long date) {
            if (date < minDateUtc) return false;
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            c.setTimeInMillis(date);
            String f = c.get(Calendar.DAY_OF_MONTH)
                    + "/" + (c.get(Calendar.MONTH) + 1)
                    + "/" + c.get(Calendar.YEAR);
            return !reserved.contains(f);
        }
        protected ReservedAndFutureDateValidator(Parcel in) {
            minDateUtc = in.readLong();
            List<String> list = in.createStringArrayList();
            reserved = new HashSet<>(list);
        }
        @Override public void writeToParcel(Parcel dest, int flags){
            dest.writeLong(minDateUtc);
            dest.writeStringList(List.copyOf(reserved));
        }
        @Override public int describeContents(){ return 0; }
        public static final Parcelable.Creator<ReservedAndFutureDateValidator> CREATOR =
                new Parcelable.Creator<ReservedAndFutureDateValidator>(){
                    @Override public ReservedAndFutureDateValidator createFromParcel(Parcel in){
                        return new ReservedAndFutureDateValidator(in);
                    }
                    @Override public ReservedAndFutureDateValidator[] newArray(int size){
                        return new ReservedAndFutureDateValidator[size];
                    }
                };
    }
}
