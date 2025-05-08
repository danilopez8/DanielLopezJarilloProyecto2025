package edu.example.daniellopezjarilloproyecto2025.models;

public class Marca {
    public String nombre;
    public int iconoResId;

    public Marca(String nombre, int iconoResId) {
        this.nombre = nombre;
        this.iconoResId = iconoResId;
    }

    public String getNombre() {
        return nombre;
    }

    public int getIconoResId() {
        return iconoResId;
    }
}
