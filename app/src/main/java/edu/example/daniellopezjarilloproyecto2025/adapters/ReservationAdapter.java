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
import edu.example.daniellopezjarilloproyecto2025.ui.reservas.ReservationDetailActivity;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder> {

    // Declaramos las variables
    private final List<Reserva> reservas;
    private final Context context;

    public ReservationAdapter(List<Reserva> reservas, Context context) {
        this.reservas = reservas;
        this.context  = context;
    }

    // Inflamos la vista de cada item del RecycledView
    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_reserva, parent, false);
        return new ReservationViewHolder(v);
    }

    // Asignamos los datos de la reserva a la vista
    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int pos) {
        Reserva r = reservas.get(pos);

        holder.txtBrand.setText("Marca: " + r.brand);
        holder.txtModel.setText("Modelo: " + r.model);
        holder.txtDate.setText("Fecha: " + r.date);
        holder.txtLocation.setText("Ubicación: " + r.location);

        // Asignamos la foto
        if (r.images != null && !r.images.isEmpty()) {
            Glide.with(context).load(r.images.get(0)).into(holder.imageCar);
        }

        // Si hacemos click, se nos abre la vista en detalle
        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(context, ReservationDetailActivity.class);
            i.putExtra("reservationId", r.reservationId);
            i.putExtra("brand",         r.brand);
            i.putExtra("model",         r.model);
            i.putExtra("date",          r.date);
            i.putExtra("location",      r.location);
            i.putExtra("price",         r.price);
            i.putStringArrayListExtra("images", new java.util.ArrayList<>(r.images));
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return reservas.size();
    }

    // Clase interna ViewHolder que contiene las referencias a las vistas de cada ítem.
    static class ReservationViewHolder extends RecyclerView.ViewHolder {
        ImageView imageCar;
        TextView txtBrand, txtModel, txtDate, txtLocation;

        public ReservationViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCar    = itemView.findViewById(R.id.imageCarReserva);
            txtBrand    = itemView.findViewById(R.id.txtReservaBrand);
            txtModel    = itemView.findViewById(R.id.txtReservaModel);
            txtDate     = itemView.findViewById(R.id.txtReservaDate);
            txtLocation = itemView.findViewById(R.id.txtReservaLocation);
        }
    }
}
