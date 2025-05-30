package edu.example.daniellopezjarilloproyecto2025.models;

import java.util.List;

public class Reserva {
    public String reservationId;   // ID de Firestore
    public String carId;           // <- nuevo campo: ID del coche
    public String brand;
    public String model;
    public String date;
    public String location;
    public int price;
    public List<String> images;

    // Constructor original (para compatibilidad)
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

    // Nuevo constructor que incluye carId
    public Reserva(String reservationId,
                   String carId,
                   String brand,
                   String model,
                   String date,
                   String location,
                   int price,
                   List<String> images) {
        this.reservationId = reservationId;
        this.carId         = carId;
        this.brand         = brand;
        this.model         = model;
        this.date          = date;
        this.location      = location;
        this.price         = price;
        this.images        = images;
    }
}
