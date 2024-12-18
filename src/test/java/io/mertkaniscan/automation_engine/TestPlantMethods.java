package io.mertkaniscan.automation_engine;

import io.mertkaniscan.automation_engine.models.Day;
import io.mertkaniscan.automation_engine.models.Hour;
import io.mertkaniscan.automation_engine.models.Plant;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TestPlantMethods {
    public static void main(String[] args) {
        // Create test plant
        Plant plant = createTestPlant();

        // Test getToday method
        System.out.println("Testing getToday() method:");
        Day today = plant.getToday();
        if (today != null) {
            System.out.println("Found today's record:");
            System.out.println("Date: " + today.getDate());
            System.out.println("Sunrise: " + today.getSunrise());
            System.out.println("Sunset: " + today.getSunset());
            System.out.println("VPD: " + today.getVpd());
        } else {
            System.out.println("No record found for today");
        }

        // Test getCurrentHour method
        System.out.println("\nTesting getCurrentHour() method:");
        Hour currentHour = plant.getCurrentHour();
        if (currentHour != null) {
            System.out.println("Found current hour record:");
            System.out.println("Hour: " + currentHour.getHour());
            System.out.println("Minute: " + currentHour.getMinute());
            System.out.println("Forecast Humidity: " + currentHour.getForecastHumidity());
            System.out.println("Forecast Temperature: " + currentHour.getForecastTemperature());
        } else {
            System.out.println("No current hour record found");
        }
    }

    private static Plant createTestPlant() {
        // Create a plant
        Plant plant = new Plant(
                1,
                Plant.PlantType.TOMATO,
                Timestamp.valueOf(LocalDateTime.now().minusDays(30)),
                Plant.PlantStage.VEGETATIVE,
                30.0,
                0.5,
                1.0,
                1
        );

        // Create today's record
        Day today = new Day(
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now().with(LocalTime.of(6, 0))),
                Timestamp.valueOf(LocalDateTime.now().with(LocalTime.of(20, 0))),
                0.8,
                plant
        );

        // Create some hour records for today
        List<Hour> hours = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();

        // Create records for the past few hours
        for (int i = Math.max(0, currentHour - 3); i <= currentHour; i++) {
            Hour hour = new Hour(i * 60, 0.5, today);
            hour.setForecastHumidity(65.0 + i); // Sample humidity data
            hour.setForecastTemperature(22.0 + i); // Sample temperature data
            hour.setSensorHumidity(64.0 + i); // Sample sensor humidity
            hour.setSensorTemperature(21.0 + i); // Sample sensor temperature
            hours.add(hour);
        }

        today.setHours(hours);

        // Set up the relationship
        List<Day> days = new ArrayList<>();
        days.add(today);
        plant.setDays(days);

        return plant;
    }
}