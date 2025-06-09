package edu.example.daniellopezjarilloproyecto2025.ui.filtrar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
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

import edu.example.daniellopezjarilloproyecto2025.R;
import edu.example.daniellopezjarilloproyecto2025.adapters.CarAdapter;
import edu.example.daniellopezjarilloproyecto2025.adapters.MarcaAdapter;
import edu.example.daniellopezjarilloproyecto2025.databinding.FragmentFiltrarBinding;
import edu.example.daniellopezjarilloproyecto2025.models.Car;
import edu.example.daniellopezjarilloproyecto2025.models.Marca;
import edu.example.daniellopezjarilloproyecto2025.utils.FakeCarData;
import androidx.core.util.Pair;

public class FiltrarFragment extends Fragment {

    private FragmentFiltrarBinding binding;

    // "brand|model" → fechas reservadas
    private final Map<String, Set<String>> reservedDatesMap = new HashMap<>();

    // Filtros activos
    private List<String> selectedDates = null;
    private String selectedBrand = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFiltrarBinding.inflate(inflater, container, false);

        // Grid de marcas / lista de coches
        binding.recyclerViewMarcas
                .setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerViewCochesFiltrados
                .setLayoutManager(new LinearLayoutManager(requireContext()));

        cargarReservas();

        // Botón “Filtrar por fecha”
        binding.btnFilterDate.setOnClickListener(v -> {
            long hoyUtc = MaterialDatePicker.todayInUtcMilliseconds();
            CalendarConstraints cons = new CalendarConstraints.Builder()
                    .setValidator(CompositeDateValidator.allOf(
                            List.of(DateValidatorPointForward.now())
                    ))
                    .build();

            MaterialDatePicker<Pair<Long,Long>> picker =
                    MaterialDatePicker.Builder.dateRangePicker()
                            .setTitleText("Selecciona rango de fechas")
                            .setCalendarConstraints(cons)
                            .build();

            picker.show(getParentFragmentManager(), "RANGE_PICKER");
            picker.addOnPositiveButtonClickListener(range -> {
                SimpleDateFormat fmt = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                Calendar start = Calendar.getInstance();
                Calendar end   = Calendar.getInstance();
                start.setTimeInMillis(range.first);
                end.setTimeInMillis(range.second);

                List<String> fechas = new ArrayList<>();
                Calendar it = (Calendar) start.clone();
                while (!it.after(end)) {
                    fechas.add(fmt.format(it.getTime()));
                    it.add(Calendar.DAY_OF_MONTH, 1);
                }
                selectedDates = fechas;
                selectedBrand = null;
                updateDisplay();
            });
        });

        // Botón “Volver a marcas”
        binding.btnVolverMarcas.setOnClickListener(v -> {
            selectedBrand = null;
            updateDisplay();
        });

        // Estado inicial
        selectedDates = null;
        selectedBrand = null;
        updateDisplay();

        return binding.getRoot();
    }

    private void cargarReservas() {
        FirebaseFirestore.getInstance()
                .collection("reservas") // <-- aquí se cambió de "reservations" a "reservas"
                .get()
                .addOnSuccessListener(qs -> {
                    if (!isAdded() || binding == null) return;
                    SimpleDateFormat fmt = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                    for (QueryDocumentSnapshot doc : qs) {
                        String brand = doc.getString("carBrand");
                        String model = doc.getString("carModel");
                        String key   = brand + "|" + model;
                        Set<String> dates = reservedDatesMap
                                .computeIfAbsent(key, k -> new HashSet<>());

                        if (doc.contains("startDate") && doc.contains("endDate")) {
                            try {
                                Calendar cs = Calendar.getInstance();
                                Calendar ce = Calendar.getInstance();
                                String[] p1 = doc.getString("startDate").split("/");
                                String[] p2 = doc.getString("endDate").split("/");
                                cs.set(Integer.parseInt(p1[2]), Integer.parseInt(p1[1]) - 1, Integer.parseInt(p1[0]));
                                ce.set(Integer.parseInt(p2[2]), Integer.parseInt(p2[1]) - 1, Integer.parseInt(p2[0]));
                                Calendar it = (Calendar) cs.clone();
                                while (!it.after(ce)) {
                                    dates.add(fmt.format(it.getTime()));
                                    it.add(Calendar.DAY_OF_MONTH, 1);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    updateDisplay();
                });
    }

    private void updateDisplay() {
        List<Car> allCars = FakeCarData.getCars(requireContext());
        if (allCars == null) allCars = List.of();

        if (selectedBrand == null) {
            // Mostrar marcas disponibles
            Set<String> marcas = new LinkedHashSet<>();
            for (Car c : allCars) {
                String key = c.brand + "|" + c.model;
                boolean ok = selectedDates == null
                        || reservedDatesMap.getOrDefault(key, Set.of())
                        .stream().noneMatch(selectedDates::contains);
                if (ok) marcas.add(c.brand);
            }
            List<Marca> lista = marcas.stream()
                    .map(name -> new Marca(name, getIconForBrand(name)))
                    .collect(Collectors.toList());

            binding.recyclerViewMarcas
                    .setAdapter(new MarcaAdapter(lista, requireContext(), marca -> {
                        selectedBrand = marca;
                        updateDisplay();
                    }));
            binding.recyclerViewMarcas.setVisibility(View.VISIBLE);
            binding.recyclerViewCochesFiltrados.setVisibility(View.GONE);
            binding.btnVolverMarcas.setVisibility(View.GONE);

        } else {
            // Mostrar lista de coches filtrados
            List<Car> filtrados = allCars.stream()
                    .filter(c -> c.brand.equalsIgnoreCase(selectedBrand))
                    .filter(c -> {
                        if (selectedDates == null) return true;
                        String key = c.brand + "|" + c.model;
                        return reservedDatesMap.getOrDefault(key, Set.of())
                                .stream().noneMatch(selectedDates::contains);
                    })
                    .collect(Collectors.toList());

            binding.recyclerViewCochesFiltrados
                    .setAdapter(new CarAdapter(filtrados, requireContext()));
            binding.recyclerViewMarcas.setVisibility(View.GONE);
            binding.recyclerViewCochesFiltrados.setVisibility(View.VISIBLE);
            binding.btnVolverMarcas.setVisibility(View.VISIBLE);
        }
    }

    /** Switch explícito de nombre → JPG sin extensión */
    private @DrawableRes int getIconForBrand(String brandName) {
        switch (brandName.toLowerCase(Locale.ROOT)) {
            case "ferrari":       return R.drawable.ferrari;
            case "lamborghini":   return R.drawable.lambo;
            case "porsche":       return R.drawable.porsche;
            case "mclaren":       return R.drawable.mclaren;
            case "aston martin":  return R.drawable.aston;
            case "audi":          return R.drawable.audi;
            case "mercedes-benz": return R.drawable.mercedes;
            case "bmw":           return R.drawable.bmw;
            case "tesla":         return R.drawable.tesla;
            case "bentley":       return R.drawable.bentley;
            case "rolls-royce":   return R.drawable.rolls;
            case "bugatti":       return R.drawable.bugatti;
            case "jaguar":        return R.drawable.jaguar;
            case "maserati":      return R.drawable.maseratti;
            case "koenigsegg":    return R.drawable.koen;
            case "pagani":        return R.drawable.pagani;
            case "ford":          return R.drawable.ford;
            default:              return R.drawable.ic_launcher_background;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
