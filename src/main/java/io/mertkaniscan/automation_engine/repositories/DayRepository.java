package io.mertkaniscan.automation_engine.repositories;

import io.mertkaniscan.automation_engine.models.Day;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public interface DayRepository extends JpaRepository<Day, Integer>{
    @Query("SELECT d FROM Day d WHERE d.plant.plantID = :plantID AND DATE(d.date) = DATE(:date)")
    Day findByPlant_PlantIDAndDate(@Param("plantID") int plantID, @Param("date") Timestamp date);

    @Query("SELECT d FROM Day d LEFT JOIN FETCH d.hours WHERE d.plant.plantID = :plantID AND d.date = :date")
    Day findByPlant_PlantIDAndDateWithHours(@Param("plantID") int plantID, @Param("date") Timestamp date);
}