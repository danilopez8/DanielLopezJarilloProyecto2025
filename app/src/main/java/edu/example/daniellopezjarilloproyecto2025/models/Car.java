package edu.example.daniellopezjarilloproyecto2025.models;

import java.util.ArrayList;
import java.util.List;

public class Car {
    public String id;
    public String brand;
    public String model;
    public int year;
    public int rental_price;
    public List<String> images;
    public Location location;

    // Nuevo campo para las fechas disponibles (formato "d/M/yyyy")
    public List<String> availableDates = new ArrayList<>();

    public Car(String id, String brand, String model, int year, int rental_price, List<String> images) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.rental_price = rental_price;
        this.images = images;
        // availableDates ya inicializado como lista vacía
    }

    /**
     * Si tus datos vienen de un JSON que ya incluye "availableDates",
     * puedes añadir otro constructor que reciba ese listado:
     */
    public Car(String id, String brand, String model, int year, int rental_price,
               List<String> images, List<String> availableDates) {
        this(id, brand, model, year, rental_price, images);
        if (availableDates != null) {
            this.availableDates = availableDates;
        }
    }

    public static class Location {
        public double lat;
        public double lng;
        public String city;
    }
}
