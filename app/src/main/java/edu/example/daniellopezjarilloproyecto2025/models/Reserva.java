// Reserva.java
package edu.example.daniellopezjarilloproyecto2025.models;

import java.util.List;

public class Reserva {
    public String reservationId;   // ID del documento en Firestore ("reservas" collection)
    public String carId;           // <-- nuevo campo: ID del coche en la colección "coches"
    public String brand;
    public String model;
    public String date;            // Ej: "Del 2/6/2025 al 4/6/2025"
    public String location;        // Ej: "Málaga", "Sevilla", etc.
    public int price;
    public List<String> images;    // URLs de imágenes del coche

    // Constructor original: (sin carId, para compatibilidad)
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
