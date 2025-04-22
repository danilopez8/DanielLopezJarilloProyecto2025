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

import java.util.List;

import edu.example.daniellopezjarilloproyecto2025.R;
import edu.example.daniellopezjarilloproyecto2025.models.Reserva;
import edu.example.daniellopezjarilloproyecto2025.ui.concesionario.CarDetailActivity;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder> {

    private final List<Reserva> reservas;
    private final Context context;

    public ReservationAdapter(List<Reserva> reservas, Context context) {
        this.reservas = reservas;
        this.context = context;
    }

    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reserva, parent, false);
        return new ReservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position) {
        Reserva reserva = reservas.get(position);

        holder.txtBrand.setText("Marca: " + reserva.brand);
        holder.txtModel.setText("Modelo: " + reserva.model);
        holder.txtDate.setText("Fecha: " + reserva.date);
        holder.txtLocation.setText("Ubicación: " + reserva.location);

        if (reserva.images != null && !reserva.images.isEmpty()) {
            Glide.with(context).load(reserva.images.get(0)).into(holder.imageCar);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CarDetailActivity.class);
            intent.putExtra("car_brand", reserva.brand);
            intent.putExtra("car_model", reserva.model);
            intent.putExtra("car_price", reserva.price);
            intent.putExtra("car_images", new java.util.ArrayList<>(reserva.images));
            intent.putExtra("car_lat", 0.0); // Puedes guardar coordenadas si es necesario
            intent.putExtra("car_lng", 0.0);
            intent.putExtra("car_city", reserva.location);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return reservas.size();
    }

    public static class ReservationViewHolder extends RecyclerView.ViewHolder {
        ImageView imageCar;
        TextView txtBrand, txtModel, txtDate, txtLocation;

        public ReservationViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCar = itemView.findViewById(R.id.imageCarReserva);
            txtBrand = itemView.findViewById(R.id.txtReservaBrand);
            txtModel = itemView.findViewById(R.id.txtReservaModel);
            txtDate = itemView.findViewById(R.id.txtReservaDate);
            txtLocation = itemView.findViewById(R.id.txtReservaLocation);
        }
    }
}
