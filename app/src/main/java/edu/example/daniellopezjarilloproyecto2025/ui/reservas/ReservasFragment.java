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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import edu.example.daniellopezjarilloproyecto2025.adapters.ReservationAdapter;
import edu.example.daniellopezjarilloproyecto2025.databinding.FragmentReservasBinding;
import edu.example.daniellopezjarilloproyecto2025.models.Reserva;

public class ReservasFragment extends Fragment {

    private FragmentReservasBinding binding;
    private ReservationAdapter adapter;

    /** Lista que alimenta el RecyclerView **/
    private final List<Reserva> reservasList = new ArrayList<>();

    /** Set para marcar qué car_id ya hemos añadido **/
    private final Set<String> processedCarIds = new HashSet<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentReservasBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Configuramos RecyclerView con 2 columnas
        binding.recyclerViewReservas.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new ReservationAdapter(reservasList, requireContext());
        binding.recyclerViewReservas.setAdapter(adapter);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cada vez que volvemos al fragment, recargamos
        cargarReservas();
    }

    /**
     * 1) Limpiar estado previo (lista + set)
     * 2) Recuperar todas las reservas de este usuario (colección "reservas")
     *
     */
    private void cargarReservas() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Limpiamos estado previo
        reservasList.clear();
        processedCarIds.clear();
        adapter.notifyDataSetChanged();

        // Recuperamos todas las reservas de este usuario
        FirebaseFirestore.getInstance()
                .collection("reservas")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded() || binding == null) return;
                    // Si no hay documentos, dejamos el RecyclerView vacío
                    if (querySnapshot.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    // Recorremos cada documento de reserva encontrado
                    for (QueryDocumentSnapshot docReserva : querySnapshot) {
                        final String reservaId     = docReserva.getId();
                        final String carIdGuardado = docReserva.getString("car_id");
                        final String start         = docReserva.getString("startDate");
                        final String end           = docReserva.getString("endDate");
                        final String ciudad        = docReserva.getString("location");
                        Object precioObj           = docReserva.get("car_price");
                        @SuppressWarnings("unchecked")
                        final List<String> imgs    = (List<String>) docReserva.get("car_images");

                        // Si no hay car_id o ya procesamos este car_id, lo ignoramos
                        if (carIdGuardado == null || processedCarIds.contains(carIdGuardado)) {
                            continue;
                        }

                        // Si faltan fechas o el rango ya expiró, lo ignoramos
                        if (start == null || end == null || !rangoValido(start, end)) {
                            continue;
                        }

                        // Convertimos precio en variable final para capturarla en el lambda
                        int precio;
                        if (precioObj instanceof Number) {
                            precio = ((Number) precioObj).intValue();
                        } else {
                            try {
                                precio = Integer.parseInt(precioObj.toString());
                            } catch (Exception e) {
                                precio = 0;
                            }
                        }
                        final int precioFinal = precio;
                        final List<String> imgsFinal = (imgs != null ? imgs : new ArrayList<>());

                        // Marcamos este car_id como procesado para evitar duplicados futuros
                        processedCarIds.add(carIdGuardado);

                        // Subconsulta para obtener marca y modelo desde "coches/{car_id}"
                        FirebaseFirestore.getInstance()
                                .collection("coches")
                                .document(carIdGuardado)
                                .get()
                                .addOnSuccessListener(carSnap -> {
                                    if (carSnap.exists()) {
                                        final String marca  = carSnap.getString("brand");
                                        final String modelo = carSnap.getString("model");

                                        // Creamos el objeto Reserva completo
                                        Reserva r = new Reserva(
                                                reservaId,              // ID del documento reserva
                                                carIdGuardado,          // ID del documento coche
                                                (marca  != null) ? marca  : "",
                                                (modelo != null) ? modelo : "",
                                                "Del " + start + " al " + end,
                                                (ciudad != null) ? ciudad : "",
                                                precioFinal,
                                                imgsFinal
                                        );

                                        // Insertamos en la lista y notificamos al adapter
                                        reservasList.add(r);
                                        adapter.notifyItemInserted(reservasList.size() - 1);
                                    } else {
                                        Log.w("ReservasFragment",
                                                "Documento 'coches/" + carIdGuardado + "' no encontrado.");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("ReservasFragment",
                                            "Error al leer 'coches/" + carIdGuardado + "'", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al cargar reservas", Toast.LENGTH_SHORT).show();
                    Log.e("ReservasFragment", "Error en la consulta de 'reservas'", e);
                });
    }

    /**
     * Verifica que la fecha de fin no sea anterior al día de hoy.
     * Para no mostrar reservas ya vencidas.
     */
    private boolean rangoValido(String startStr, String endStr) {
        try {
            String[] p1 = startStr.split("/");
            String[] p2 = endStr.split("/");

            Calendar start = Calendar.getInstance();
            start.set(
                    Integer.parseInt(p1[2]),
                    Integer.parseInt(p1[1]) - 1,
                    Integer.parseInt(p1[0]),
                    0, 0, 0
            );
            start.set(Calendar.MILLISECOND, 0);

            Calendar end = Calendar.getInstance();
            end.set(
                    Integer.parseInt(p2[2]),
                    Integer.parseInt(p2[1]) - 1,
                    Integer.parseInt(p2[0]),
                    0, 0, 0
            );
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
