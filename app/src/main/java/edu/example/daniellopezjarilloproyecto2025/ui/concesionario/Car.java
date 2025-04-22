package edu.example.daniellopezjarilloproyecto2025.ui.concesionario;

import java.util.List;

public class Car {
    public int id;
    public String brand;
    public String model;
    public int year;
    public int rental_price;
    public List<String> images;
    public Location location;

    public Car(int id, String brand, String model, int year, int rental_price, List<String> images) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.rental_price = rental_price;
        this.images = images;
    }

    public static class Location {
        public double lat;
        public double lng;
        public String city;
    }
}
