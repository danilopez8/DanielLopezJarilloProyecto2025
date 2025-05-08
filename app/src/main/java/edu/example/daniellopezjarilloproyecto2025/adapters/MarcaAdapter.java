package edu.example.daniellopezjarilloproyecto2025.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.example.daniellopezjarilloproyecto2025.R;
import edu.example.daniellopezjarilloproyecto2025.models.Marca;

public class MarcaAdapter extends RecyclerView.Adapter<MarcaAdapter.MarcaViewHolder> {

    // Interfaz para manejar el clic en una marca
    public interface OnMarcaClickListener {
        void onMarcaClick(String nombreMarca);
    }

    // Declaramos las variables
    private final List<Marca> lista;
    private final Context context;
    private final OnMarcaClickListener listener;

    public MarcaAdapter(List<Marca> lista, Context context, OnMarcaClickListener listener) {
        this.lista = lista;
        this.context = context;
        this.listener = listener;
    }

    // Método para inflar el layout del ítem de la marca y devolver el ViewHolder.
    @NonNull
    @Override
    public MarcaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_marca, parent, false);
        return new MarcaViewHolder(v);
    }

    // Asignamos los datos de cada marca a su vista correspondiente.
    @Override
    public void onBindViewHolder(@NonNull MarcaViewHolder holder, int position) {
        Marca m = lista.get(position);
        holder.txt.setText(m.nombre);
        holder.img.setImageResource(m.iconoResId);
        holder.itemView.setOnClickListener(v -> listener.onMarcaClick(m.nombre));
    }

    // Número total de elementos en la lista
    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class MarcaViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView txt;
        public MarcaViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgMarca); // ImageView del logo de la marca.
            txt = itemView.findViewById(R.id.txtMarca); // TextView del nombre de la marca.
        }
    }
}
