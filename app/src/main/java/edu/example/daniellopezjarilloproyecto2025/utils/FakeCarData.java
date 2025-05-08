package edu.example.daniellopezjarilloproyecto2025.utils;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import edu.example.daniellopezjarilloproyecto2025.models.Car;

public class FakeCarData {

    public static List<Car> getCars(Context context) {
        try {
            InputStream inputStream = context.getAssets().open("cars.json");
            return new Gson().fromJson(
                    new InputStreamReader(inputStream),
                    new TypeToken<List<Car>>() {}.getType()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
