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
import java.util.List;

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
                    reservasList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        List<String> images = (List<String>) doc.get("car_images");
                        if (images == null) {
                            images = new ArrayList<>();
                        }
                        String idReserva = doc.getId();
                        Reserva reserva = new Reserva(
                                idReserva,
                                doc.getString("carBrand"),
                                doc.getString("carModel"),
                                doc.getString("reservationDate"),
                                doc.getString("location"),
                                (doc.getLong("car_price") != null) ? doc.getLong("car_price").intValue() : 0,
                                images
                        );
                        reservasList.add(reserva);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al cargar reservas", Toast.LENGTH_SHORT).show();
                    Log.e("ReservasFragment", "Error al obtener reservas", e);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
