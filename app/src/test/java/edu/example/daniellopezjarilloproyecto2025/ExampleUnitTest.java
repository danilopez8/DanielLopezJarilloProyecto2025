package edu.example.daniellopezjarilloproyecto2025;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.example.daniellopezjarilloproyecto2025.models.Car;
import edu.example.daniellopezjarilloproyecto2025.models.Marca;
import edu.example.daniellopezjarilloproyecto2025.models.Reserva;

import static org.junit.Assert.*;

public class ExampleUnitTest {

    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    // 1) Car: constructor básico asigna campos y las fechas disponibles quedan vacías
    @Test
    public void car_basicConstructor_initializesFieldsAndEmptyDates() {
        List<String> images = Arrays.asList("img1.jpg", "img2.jpg");
        Car car = new Car(1, "Toyota", "Corolla", 2020, 50, images);

        assertEquals(1, car.id);
        assertEquals("Toyota", car.brand);
        assertEquals("Corolla", car.model);
        assertEquals(2020, car.year);
        assertEquals(50, car.rental_price);
        assertSame(images, car.images);

        // availableDates fue inicializada como lista vacía
        assertNotNull(car.availableDates);
        assertTrue(car.availableDates.isEmpty());
    }

    // 2) Car: constructor con lista de fechas sobreescribe availableDates
    @Test
    public void car_constructorWithDates_setsAvailableDates() {
        List<String> images = Collections.singletonList("img.jpg");
        List<String> dates = Arrays.asList("1/6/2025", "2/6/2025");
        Car car = new Car(2, "Ford", "Focus", 2019, 45, images, dates);

        assertEquals(2, car.id);
        assertEquals("Ford", car.brand);
        assertEquals("Focus", car.model);
        // compruebo que availableDates es exactamente la lista que pasé
        assertSame(dates, car.availableDates);
    }

    // 3) Marca: getters devuelven los valores correctos
    @Test
    public void marca_gettersReturnCorrectValues() {
        Marca marca = new Marca("BMW", 12345);
        assertEquals("BMW", marca.getNombre());
        assertEquals(12345, marca.getIconoResId());
    }

    // 4) Reserva: constructor asigna todos los campos
    @Test
    public void reserva_constructorAssignsAllFields() {
        List<String> images = Arrays.asList("i1.png", "i2.png");
        Reserva reserva = new Reserva(
                "res123",   // reservationId
                "car456",   // carId
                "Audi",     // brand
                "A4",       // model
                "10/6/2025",// date
                "Madrid",   // location
                120,        // price
                images      // images
        );

        assertEquals("res123", reserva.reservationId);
        assertEquals("car456",   reserva.carId);
        assertEquals("Audi",     reserva.brand);
        assertEquals("A4",       reserva.model);
        assertEquals("10/6/2025",reserva.date);
        assertEquals("Madrid",   reserva.location);
        assertEquals(120,        reserva.price);
        assertSame(images,       reserva.images);
    }

    // 5) Car.Location: compruebo que puedo asignar y leer sus campos
    @Test
    public void carLocation_fieldsAreMutableAndReadable() {
        Car.Location loc = new Car.Location();
        loc.lat = 40.4168;
        loc.lng = -3.7038;
        loc.city = "Madrid";

        assertEquals(40.4168, loc.lat, 0.0001);
        assertEquals(-3.7038, loc.lng, 0.0001);
        assertEquals("Madrid", loc.city);
    }

   /* @Test
    public void failing_test_carHasNoImages() {
        Car car = new Car(1, "Tesla", "Model 3", 2021, 100, Arrays.asList("img1.png"));
        // Exigimos que la lista venga vacía, lo cual es incorrecto:
        assertTrue("El coche no debería tener imágenes, pero sí las tiene",
                car.images.isEmpty());
    }
*/

}
