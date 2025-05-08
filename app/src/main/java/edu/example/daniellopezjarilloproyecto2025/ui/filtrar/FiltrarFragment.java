package edu.example.daniellopezjarilloproyecto2025.ui.filtrar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.example.daniellopezjarilloproyecto2025.adapters.CarAdapter;
import edu.example.daniellopezjarilloproyecto2025.adapters.MarcaAdapter;
import edu.example.daniellopezjarilloproyecto2025.databinding.FragmentFiltrarBinding;
import edu.example.daniellopezjarilloproyecto2025.models.Marca;
import edu.example.daniellopezjarilloproyecto2025.models.Car;
import edu.example.daniellopezjarilloproyecto2025.utils.FakeCarData;

public class FiltrarFragment extends Fragment {

    private FragmentFiltrarBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFiltrarBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        // Grid de 2 columnas para las marcas
        binding.recyclerViewMarcas.setLayoutManager(
                new GridLayoutManager(requireContext(), 2)
        );
        // Lista vertical para los coches filtrados
        binding.recyclerViewCochesFiltrados.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        // Cargo todos los coches desde el JSON
        List<Car> coches = FakeCarData.getCars(requireContext());
        if (coches == null) {
            coches = List.of();
        }

        // Extraigo marcas únicas en orden de aparición
        Set<String> marcasUnicas = coches.stream()
                .map(c -> c.brand)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Construyo lista de Marca (ahora con icono placeholder)
        List<Marca> marcas = marcasUnicas.stream()
                .map(nombre -> new Marca(
                        nombre,
                        android.R.drawable.ic_menu_gallery // placeholder de sistema
                ))
                .collect(Collectors.toList());

        // Adapter de marcas con callback a mostrarCochesFiltrados(...)
        MarcaAdapter marcaAdapter = new MarcaAdapter(
                marcas,
                requireContext(),
                this::mostrarCochesFiltrados
        );
        binding.recyclerViewMarcas.setAdapter(marcaAdapter);

        // Botón «Volver»
        binding.btnVolverMarcas.setOnClickListener(v -> {
            binding.recyclerViewCochesFiltrados.setVisibility(View.GONE);
            binding.btnVolverMarcas.setVisibility(View.GONE);
            binding.recyclerViewMarcas.setVisibility(View.VISIBLE);
        });

        // Al arrancar: mostramos solo marcas
        binding.recyclerViewMarcas.setVisibility(View.VISIBLE);
        binding.recyclerViewCochesFiltrados.setVisibility(View.GONE);
        binding.btnVolverMarcas.setVisibility(View.GONE);

        return root;
    }

    /**
     * Filtra la lista de coches por la marca seleccionada y la muestra.
     */
    private void mostrarCochesFiltrados(String marcaSeleccionada) {
        List<Car> coches = FakeCarData.getCars(requireContext());
        if (coches == null) {
            coches = List.of();
        }
        List<Car> filtrados = coches.stream()
                .filter(c -> c.brand.equalsIgnoreCase(marcaSeleccionada))
                .collect(Collectors.toList());

        CarAdapter carAdapter = new CarAdapter(filtrados, requireContext());
        binding.recyclerViewCochesFiltrados.setAdapter(carAdapter);

        // Cambio visibilidad
        binding.recyclerViewMarcas.setVisibility(View.GONE);
        binding.recyclerViewCochesFiltrados.setVisibility(View.VISIBLE);
        binding.btnVolverMarcas.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
