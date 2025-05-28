package edu.example.daniellopezjarilloproyecto2025.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.example.daniellopezjarilloproyecto2025.R;
import edu.example.daniellopezjarilloproyecto2025.models.Marca;

public class MarcaAdapter extends RecyclerView.Adapter<MarcaAdapter.MarcaViewHolder> {
    private final List<Marca> marcas;
    private final Context context;
    private final Consumer<String> onMarcaClick;

    public MarcaAdapter(List<Marca> marcas, Context context, Consumer<String> onMarcaClick) {
        this.marcas = marcas;
        this.context = context;
        this.onMarcaClick = onMarcaClick;
    }

    @NonNull @Override
    public MarcaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_marca, parent, false);
        return new MarcaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MarcaViewHolder holder, int position) {
        Marca m = marcas.get(position);
        holder.txtNombre.setText(m.nombre);
        holder.imgIcon.setImageResource(m.iconoResId);
        holder.itemView.setOnClickListener(v -> onMarcaClick.accept(m.nombre));
    }

    @Override
    public int getItemCount() {
        return marcas.size();
    }

    static class MarcaViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView txtNombre;

        MarcaViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon    = itemView.findViewById(R.id.imgMarcaIcon);
            txtNombre  = itemView.findViewById(R.id.txtMarcaNombre);
        }
    }
}
