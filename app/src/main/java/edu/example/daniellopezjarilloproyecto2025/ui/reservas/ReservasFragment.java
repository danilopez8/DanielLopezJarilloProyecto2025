package edu.example.daniellopezjarilloproyecto2025.ui.reservas;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.example.daniellopezjarilloproyecto2025.adapters.ReservationAdapter;
import edu.example.daniellopezjarilloproyecto2025.databinding.FragmentReservasBinding;
import edu.example.daniellopezjarilloproyecto2025.models.Reserva;

public class ReservasFragment extends Fragment {

    private FragmentReservasBinding binding;
    private ReservationAdapter adapter;
    private final List<Reserva> reservasList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReservasBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Cambiamos a GridLayoutManager
        binding.recyclerViewReservas.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new ReservationAdapter(reservasList, requireContext());
        binding.recyclerViewReservas.setAdapter(adapter);

        cargarReservas();

        return root;
    }
    @Override
    public void onResume() {
        super.onResume();
        cargarReservas();  // vuelve a hacer la consulta y refresca la lista
    }


    private void cargarReservas() {
        String userEmail = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;

        if (userEmail == null) {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("reservations")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Agrupar reservas por mismo rango de fechas
                    Map<String, Reserva> grouped = new LinkedHashMap<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String start = doc.getString("startDate");
                        String end   = doc.getString("endDate");
                        if (start == null || end == null || !rangoValido(start, end)) {
                            continue;
                        }

                        // Key única para el mismo rango
                        String key = start + "|" + end;
                        if (grouped.containsKey(key)) continue;

                        // Obtener imágenes
                        List<String> images = (List<String>) doc.get("car_images");
                        if (images == null) images = new ArrayList<>();

                        // Precio robusto
                        Object priceObj = doc.get("car_price");
                        int price = 0;
                        if (priceObj instanceof Number) {
                            price = ((Number) priceObj).intValue();
                        } else if (priceObj instanceof String) {
                            try { price = Integer.parseInt((String) priceObj); }
                            catch (NumberFormatException ignored) {}
                        }

                        Reserva reserva = new Reserva(
                                doc.getId(),
                                doc.getString("carBrand"),
                                doc.getString("carModel"),
                                "Del " + start + " al " + end,
                                doc.getString("location"),
                                price,
                                images
                        );
                        grouped.put(key, reserva);
                    }

                    reservasList.clear();
                    reservasList.addAll(grouped.values());
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al cargar reservas", Toast.LENGTH_SHORT).show();
                    Log.e("ReservasFragment", "Error al obtener reservas", e);
                });
    }

    private boolean rangoValido(String startStr, String endStr) {
        try {
            String[] p1 = startStr.split("/");
            String[] p2 = endStr.split("/");

            Calendar start = Calendar.getInstance();
            start.set(Integer.parseInt(p1[2]), Integer.parseInt(p1[1]) - 1, Integer.parseInt(p1[0]), 0, 0, 0);
            start.set(Calendar.MILLISECOND, 0);

            Calendar end = Calendar.getInstance();
            end.set(Integer.parseInt(p2[2]), Integer.parseInt(p2[1]) - 1, Integer.parseInt(p2[0]), 0, 0, 0);
            end.set(Calendar.MILLISECOND, 0);

            Calendar hoy = Calendar.getInstance();
            hoy.set(Calendar.HOUR_OF_DAY, 0);
            hoy.set(Calendar.MINUTE, 0);
            hoy.set(Calendar.SECOND, 0);
            hoy.set(Calendar.MILLISECOND, 0);

            return !end.before(hoy);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
