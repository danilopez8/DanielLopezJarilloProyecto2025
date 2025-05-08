package edu.example.daniellopezjarilloproyecto2025.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import edu.example.daniellopezjarilloproyecto2025.R;
import edu.example.daniellopezjarilloproyecto2025.models.Car;
import edu.example.daniellopezjarilloproyecto2025.ui.concesionario.CarDetailActivity;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    // Declaramos las variables
    private final List<Car> carList;
    // Contexto para poder lanzar actividades y cargar vistas.
    private final Context context;

    public CarAdapter(List<Car> carList, Context context) {
        this.carList = carList;
        this.context = context;
    }

    // Método que infla el layout de cada ítem del RecyclerView.
    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_car, parent, false);
        // Se devuelve una nueva instancia del ViewHolder.
        return new CarViewHolder(view);
    }

    // Método que enlaza los datos de cada coche con las vistas del ítem.
    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        // Se obtiene el coche correspondiente a la posición actual.
        Car car = carList.get(position);
        // Mostramos los datos básicos del coche.
        holder.txtBrand.setText(car.brand);
        holder.txtModel.setText(car.model);
        holder.txtPrice.setText("Precio: " + car.rental_price + "€/día");
        // Si el coche tiene imágenes, se carga la primera imagen con Glide.
        if (car.images != null && !car.images.isEmpty()) {
            Glide.with(context).load(car.images.get(0)).into(holder.imageCar);
        }

        // Configuramos el evento al hacer clic sobre el ítem.
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CarDetailActivity.class);
            // Añadimos los datos del coche como extra intent
            intent.putExtra("car_id", car.id);
            intent.putExtra("car_brand", car.brand);
            intent.putExtra("car_model", car.model);
            intent.putExtra("car_year", car.year);
            intent.putExtra("car_price", car.rental_price);
            intent.putStringArrayListExtra("car_images", new ArrayList<>(car.images));

            // Añadimos la ubicación (Si la tiene?
            if (car.location != null) {
                intent.putExtra("car_lat", car.location.lat);
                intent.putExtra("car_lng", car.location.lng);
                intent.putExtra("car_city", car.location.city);
            }

            // Lanzamos la actividad del detalle
            context.startActivity(intent);
        });
    }

    // Devuelve la cantidad total de elementos en la lista.
    @Override
    public int getItemCount() {
        return carList.size();
    }

    // ViewHolder que contiene las vistas de cada ítem del RecyclerView.
    public static class CarViewHolder extends RecyclerView.ViewHolder {
        ImageView imageCar;
        TextView txtBrand, txtModel, txtPrice;

        public CarViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCar = itemView.findViewById(R.id.imageCar);
            txtBrand = itemView.findViewById(R.id.txtBrand);
            txtModel = itemView.findViewById(R.id.txtModel);
            txtPrice = itemView.findViewById(R.id.txtPrice);
        }
    }
}
