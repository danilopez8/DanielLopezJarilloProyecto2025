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
import edu.example.daniellopezjarilloproyecto2025.ui.concesionario.Car;
import edu.example.daniellopezjarilloproyecto2025.ui.concesionario.CarDetailActivity;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    private final List<Car> carList;
    private final Context context;

    public CarAdapter(List<Car> carList, Context context) {
        this.carList = carList;
        this.context = context;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_car, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = carList.get(position);
        holder.txtBrand.setText(car.brand);
        holder.txtModel.setText(car.model);
        holder.txtPrice.setText("Precio: " + car.rental_price + "€/día");

        if (car.images != null && !car.images.isEmpty()) {
            Glide.with(context).load(car.images.get(0)).into(holder.imageCar);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CarDetailActivity.class);
            intent.putExtra("car_id", car.id);
            intent.putExtra("car_brand", car.brand);
            intent.putExtra("car_model", car.model);
            intent.putExtra("car_year", car.year);
            intent.putExtra("car_price", car.rental_price);
            intent.putStringArrayListExtra("car_images", new ArrayList<>(car.images));


            if (car.location != null) {
                intent.putExtra("car_lat", car.location.lat);
                intent.putExtra("car_lng", car.location.lng);
                intent.putExtra("car_city", car.location.city);
            }

            context.startActivity(intent);
        });
    }


        @Override
    public int getItemCount() {
        return carList.size();
    }

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
