package edu.example.daniellopezjarilloproyecto2025.ui.filtrar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialDatePicker.Builder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.example.daniellopezjarilloproyecto2025.adapters.CarAdapter;
import edu.example.daniellopezjarilloproyecto2025.adapters.MarcaAdapter;
import edu.example.daniellopezjarilloproyecto2025.databinding.FragmentFiltrarBinding;
import edu.example.daniellopezjarilloproyecto2025.models.Marca;
import edu.example.daniellopezjarilloproyecto2025.models.Car;
import edu.example.daniellopezjarilloproyecto2025.utils.FakeCarData;
import androidx.core.util.Pair;

public class FiltrarFragment extends Fragment {

    private FragmentFiltrarBinding binding;

    // Mapa "brand|model" -> fechas ya reservadas
    private final Map<String, Set<String>> reservedDatesMap = new HashMap<>();

    // Fechas seleccionadas por el usuario (rango)
    private List<String> selectedDates = null;
    // Marca seleccionada por el usuario
    private String selectedBrand = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFiltrarBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // LayoutManagers
        binding.recyclerViewMarcas.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerViewCochesFiltrados.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Pre-cargo reservas para el filtro de fechas
        cargarReservas();

        // Botón para elegir rango de fechas
        binding.btnFilterDate.setOnClickListener(v -> {
            long hoyUtc = MaterialDatePicker.todayInUtcMilliseconds();
            CalendarConstraints constraints = new CalendarConstraints.Builder()
                    .setValidator(CompositeDateValidator.allOf(
                            List.of(DateValidatorPointForward.now())
                    ))
                    .build();

            Builder<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker();
            picker.setTitleText("Selecciona rango de fechas");
            picker.setCalendarConstraints(constraints);
            MaterialDatePicker<Pair<Long, Long>> dateRangePicker = picker.build();

            dateRangePicker.show(getParentFragmentManager(), "RANGE_PICKER");
            dateRangePicker.addOnPositiveButtonClickListener(range -> {
                // Calculo todas las fechas entre inicio y fin
                Calendar start = Calendar.getInstance();
                Calendar end   = Calendar.getInstance();
                SimpleDateFormat fmt = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());

                start.setTimeInMillis(range.first);
                end.setTimeInMillis(range.second);

                List<String> fechas = new ArrayList<>();
                Calendar it = (Calendar) start.clone();
                while (!it.after(end)) {
                    fechas.add(fmt.format(it.getTime()));
                    it.add(Calendar.DAY_OF_MONTH, 1);
                }
                selectedDates = fechas;
                selectedBrand = null;      // reseteamos marca
                updateDisplay();
            });
        });

        // Botón volver (deshace filtro de marca y vuelve a mostrar marcas)
        binding.btnVolverMarcas.setOnClickListener(v -> {
            selectedBrand = null;
            updateDisplay();
        });

        // Al principio mostramos TODAS las marcas
        selectedDates = null;
        selectedBrand = null;
        updateDisplay();

        return root;
    }

    /** Carga todas las reservas y rellena reservedDatesMap */
    private void cargarReservas() {
        FirebaseFirestore.getInstance()
                .collection("reservations")
                .get()
                .addOnSuccessListener(qs -> {
                    SimpleDateFormat fmt = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                    for (QueryDocumentSnapshot doc : qs) {
                        String brand = doc.getString("carBrand");
                        String model = doc.getString("carModel");
                        String key   = brand + "|" + model;
                        Set<String> dates = reservedDatesMap.computeIfAbsent(key, k -> new HashSet<>());

                        if (doc.contains("reservationDate")) {
                            String d = doc.getString("reservationDate");
                            if (d != null) dates.add(d);
                        } else if (doc.contains("startDate") && doc.contains("endDate")) {
                            try {
                                Calendar cs = Calendar.getInstance();
                                Calendar ce = Calendar.getInstance();
                                String[] p1 = doc.getString("startDate").split("/");
                                String[] p2 = doc.getString("endDate").split("/");

                                cs.set(Integer.parseInt(p1[2]), Integer.parseInt(p1[1]) - 1, Integer.parseInt(p1[0]), 0,0,0);
                                ce.set(Integer.parseInt(p2[2]), Integer.parseInt(p2[1]) - 1, Integer.parseInt(p2[0]), 0,0,0);

                                Calendar it = (Calendar) cs.clone();
                                while (!it.after(ce)) {
                                    dates.add(fmt.format(it.getTime()));
                                    it.add(Calendar.DAY_OF_MONTH, 1);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    // Tras cargar reservas, refrescamos la vista inicial
                    updateDisplay();
                });
    }

    /** Recalcula y muestra marcas o coches según los filtros activos */
    private void updateDisplay() {
        List<Car> allCars = FakeCarData.getCars(requireContext());
        if (allCars == null) allCars = List.of();

        // Si NO hay marca seleccionada:
        if (selectedBrand == null) {
            //  – Si tampoco hay fecha, muestro todas las marcas
            //  – Si hay fecha, muestro solo las marcas con algún coche disponible en ese rango
            Set<String> marcasVisibles = new LinkedHashSet<>();
            for (Car c : allCars) {
                String key = c.brand + "|" + c.model;
                // comprobar disponibilidad
                boolean ok = true;
                if (selectedDates != null) {
                    Set<String> reserved = reservedDatesMap.getOrDefault(key, Set.of());
                    for (String d : selectedDates) {
                        if (reserved.contains(d)) {
                            ok = false;
                            break;
                        }
                    }
                }
                if (ok) marcasVisibles.add(c.brand);
            }
            List<Marca> listaMarcas = marcasVisibles.stream()
                    .map(name -> new Marca(name, android.R.drawable.ic_menu_gallery))
                    .collect(Collectors.toList());

            MarcaAdapter ma = new MarcaAdapter(
                    listaMarcas,
                    requireContext(),
                    marca -> {
                        selectedBrand = marca;
                        updateDisplay();
                    }
            );
            binding.recyclerViewMarcas.setAdapter(ma);
            binding.recyclerViewMarcas.setVisibility(View.VISIBLE);
            binding.recyclerViewCochesFiltrados.setVisibility(View.GONE);
            binding.btnVolverMarcas.setVisibility(View.GONE);
        }
        // Si hay marca seleccionada, muestro LISTA de COCHES filtrados
        else {
            List<Car> filtrados = allCars.stream()
                    .filter(c -> c.brand.equalsIgnoreCase(selectedBrand))
                    .filter(c -> {
                        if (selectedDates == null) return true;
                        String key = c.brand + "|" + c.model;
                        Set<String> reserved = reservedDatesMap.getOrDefault(key, Set.of());
                        for (String d : selectedDates) {
                            if (reserved.contains(d)) return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            CarAdapter carAdapter = new CarAdapter(filtrados, requireContext());
            binding.recyclerViewCochesFiltrados.setAdapter(carAdapter);

            binding.recyclerViewMarcas.setVisibility(View.GONE);
            binding.recyclerViewCochesFiltrados.setVisibility(View.VISIBLE);
            binding.btnVolverMarcas.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
