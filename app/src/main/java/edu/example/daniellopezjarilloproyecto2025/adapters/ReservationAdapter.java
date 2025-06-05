// ReservationAdapter.java
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

    // Lista de reservas cargadas desde Firestore
    private final List<Reserva> reservas;
    private final Context context;

    public ReservationAdapter(List<Reserva> reservas, Context context) {
        this.reservas = reservas;
        this.context  = context;
    }

    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_reserva, parent, false);
        return new ReservationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int pos) {
        Reserva r = reservas.get(pos);

        // Mostramos la información directamente desde la propia reserva:
        holder.txtBrand.setText("Marca: " + r.brand);
        holder.txtModel.setText("Modelo: " + r.model);
        holder.txtDate.setText("Fecha: " + r.date);
        holder.txtLocation.setText("Ubicación: " + r.location);

        // Si existe al menos una URL de imagen, cargamos la primera con Glide
        if (r.images != null && !r.images.isEmpty()) {
            Glide.with(context).load(r.images.get(0)).into(holder.imageCar);
        }

        // Cuando el usuario pulsa en una tarjeta, abre ReservationDetailActivity
        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(context, ReservationDetailActivity.class);
            // Enviamos el ID de la reserva y los datos del coche para mostrar en detalle:
            i.putExtra("reservationId", r.reservationId);
            i.putExtra("carId",         r.carId);
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
