package edu.example.daniellopezjarilloproyecto2025.ui.concesionario;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Arrays;
import java.util.List;

import edu.example.daniellopezjarilloproyecto2025.databinding.FragmentConcesionarioBinding;
import edu.example.daniellopezjarilloproyecto2025.ui.concesionario.Car;
import edu.example.daniellopezjarilloproyecto2025.adapters.CarAdapter;
import edu.example.daniellopezjarilloproyecto2025.utils.FakeCarData;

public class Concesionario extends Fragment {

    private FragmentConcesionarioBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentConcesionarioBinding.inflate(inflater, container, false);

        List<Car> cars = FakeCarData.getCars(requireContext()); // Esto lo definiremos ahora
        CarAdapter adapter = new CarAdapter(cars, getContext());

        binding.recyclerViewCars.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewCars.setAdapter(adapter);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
