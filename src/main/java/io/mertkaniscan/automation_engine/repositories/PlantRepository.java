package io.mertkaniscan.automation_engine.repositories;

import io.mertkaniscan.automation_engine.models.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantRepository extends JpaRepository<Plant, Integer> {
}