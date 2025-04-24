package edu.example.daniellopezjarilloproyecto2025.ui.concesionario;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import edu.example.daniellopezjarilloproyecto2025.R;

public class CarDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ViewPager2 viewPager;
    private TextView brandText, modelText, yearText, priceText;
    private String brand, model, carCity;
    private double carLat, carLng;
    private Set<String> fechasReservadas = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_detail);

        viewPager  = findViewById(R.id.viewPagerImages);
        brandText  = findViewById(R.id.txtDetailBrand);
        modelText  = findViewById(R.id.txtDetailModel);
        yearText   = findViewById(R.id.txtDetailYear);
        priceText  = findViewById(R.id.txtDetailPrice);

        // Leo datos del Intent
        brand   = getIntent().getStringExtra("car_brand");
        model   = getIntent().getStringExtra("car_model");
        int year = getIntent().getIntExtra("car_year",  0);
        int price= getIntent().getIntExtra("car_price", 0);
        List<String> images = getIntent().getStringArrayListExtra("car_images");
        carLat  = getIntent().getDoubleExtra("car_lat", 0.0);
        carLng  = getIntent().getDoubleExtra("car_lng", 0.0);
        carCity = getIntent().getStringExtra("car_city");

        // Muestro datos en pantalla
        brandText.setText("Marca: " + brand);
        modelText.setText("Modelo: " + model);
        yearText.setText("Año: " + year);
        priceText.setText("Precio: " + price + "€/día");

        // Inicializo el slider de imágenes
        viewPager.setAdapter(new ImageSliderAdapter(this, images));

        // Botón de reserva
        Button btnReserve = findViewById(R.id.btnReserveCar);
        btnReserve.setOnClickListener(v -> mostrarSelectorFecha());

        // Mapa
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Cargo las fechas ya reservadas desde Firestore
        cargarFechasReservadas();
    }

    /**
     * Consulta Firestore y llena 'fechasReservadas' con las fechas ya tomadas (formato "d/M/yyyy")
     */
    private void cargarFechasReservadas() {
        FirebaseFirestore.getInstance()
                .collection("reservations")
                .whereEqualTo("carBrand", brand)
                .whereEqualTo("carModel", model)
                .get()
                .addOnSuccessListener(qs -> {
                    for (QueryDocumentSnapshot doc : qs) {
                        String d = doc.getString("reservationDate");
                        if (d != null) fechasReservadas.add(d);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "No se pudieron cargar reservas previas", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Muestra un MaterialDatePicker con validadores compuestos:
     *  Solo fechas a partir de hoy
     *  Bloquea las que estén en 'fechasReservadas'
     */
    private void mostrarSelectorFecha() {
        long hoyUtc = MaterialDatePicker.todayInUtcMilliseconds();

        CalendarConstraints.DateValidator futuroValidator =
                DateValidatorPointForward.now();
        CalendarConstraints.DateValidator bloqueadasValidator =
                new ReservedAndFutureDateValidator(hoyUtc, fechasReservadas);

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(CompositeDateValidator.allOf(
                        List.of(futuroValidator, bloqueadasValidator)))
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecciona fecha de reserva")
                .setCalendarConstraints(constraints)
                .build();

        picker.show(getSupportFragmentManager(), "RESERVA_PICKER");
        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            c.setTimeInMillis(selection);
            String sel = c.get(Calendar.DAY_OF_MONTH) + "/"
                    + (c.get(Calendar.MONTH) + 1) + "/"
                    + c.get(Calendar.YEAR);
            enviarCorreoReserva(sel);
            guardarReservaEnFirestore(sel);
        });
    }

    private void enviarCorreoReserva(String date) {
        String asunto = "Reserva de coche: " + model;
        String cuerpo = "Has reservado el coche:\n\n" +
                "Marca: "  + brand   + "\n" +
                "Modelo: " + model   + "\n" +
                "Precio: " + priceText.getText() + "\n" +
                "Ubicación: " + carCity + "\n" +
                "Fecha: " + date;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL,   new String[]{ user.getEmail() });
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, asunto);
            emailIntent.putExtra(Intent.EXTRA_TEXT,    cuerpo);
            try {
                startActivity(Intent.createChooser(emailIntent, "Elige app de correo"));
            } catch (Exception ex) {
                Toast.makeText(this, "No hay apps de correo instaladas.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarReservaEnFirestore(String date) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;

        Map<String,Object> reserva = new HashMap<>();
        reserva.put("userId",          u.getUid());
        reserva.put("email",           u.getEmail());
        reserva.put("carBrand",        brand);
        reserva.put("carModel",        model);
        reserva.put("reservationDate", date);
        reserva.put("location",        carCity);
        reserva.put("car_price",       getIntent().getIntExtra("car_price",0));
        reserva.put("car_images",      getIntent().getStringArrayListExtra("car_images"));
        reserva.put("timestamp",       FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("reservations")
                .add(reserva)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Reserva guardada", Toast.LENGTH_SHORT).show();
                    fechasReservadas.add(date);
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

    /**
     * Validador que bloquea fechas antes de hoy y las que ya estén reservadas.
     * Necesario implementar Parcelable para MaterialDatePicker.
     */
    public static class ReservedAndFutureDateValidator implements CalendarConstraints.DateValidator {
        private final long minDateUtc;
        private final Set<String> reserved;

        public ReservedAndFutureDateValidator(long minDateUtc, Set<String> reserved) {
            this.minDateUtc = minDateUtc;
            this.reserved   = reserved;
        }

        @Override
        public boolean isValid(long date) {
            if (date < minDateUtc) return false;
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            c.setTimeInMillis(date);
            String f = c.get(Calendar.DAY_OF_MONTH) + "/"
                    + (c.get(Calendar.MONTH) + 1) + "/"
                    + c.get(Calendar.YEAR);
            return !reserved.contains(f);
        }

        protected ReservedAndFutureDateValidator(Parcel in) {
            minDateUtc = in.readLong();
            List<String> list = in.createStringArrayList();
            reserved = new HashSet<>(list);
        }
        @Override public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(minDateUtc);
            dest.writeStringList(List.copyOf(reserved));
        }
        @Override public int describeContents() { return 0; }
        public static final Parcelable.Creator<ReservedAndFutureDateValidator> CREATOR =
                new Parcelable.Creator<ReservedAndFutureDateValidator>() {
                    @Override
                    public ReservedAndFutureDateValidator createFromParcel(Parcel in) {
                        return new ReservedAndFutureDateValidator(in);
                    }
                    @Override
                    public ReservedAndFutureDateValidator[] newArray(int size) {
                        return new ReservedAndFutureDateValidator[size];
                    }
                };
    }
}
