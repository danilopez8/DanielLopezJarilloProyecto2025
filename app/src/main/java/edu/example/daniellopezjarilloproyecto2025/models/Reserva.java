package edu.example.daniellopezjarilloproyecto2025.models;

import java.util.List;

public class Reserva {
    public String reservationId;   // ID de Firestore
    public String brand;
    public String model;
    public String date;
    public String location;
    public int price;
    public List<String> images;

    public Reserva(String reservationId,
                   String brand,
                   String model,
                   String date,
                   String location,
                   int price,
                   List<String> images) {
        this.reservationId = reservationId;
        this.brand         = brand;
        this.model         = model;
        this.date          = date;
        this.location      = location;
        this.price         = price;
        this.images        = images;
    }
}
