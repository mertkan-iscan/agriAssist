package io.mertkaniscan.automation_engine.repositories;

import io.mertkaniscan.automation_engine.models.Field;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FieldRepository extends JpaRepository<Field, Integer> {
    Field findByPlantInField_PlantID(int plantID);
}