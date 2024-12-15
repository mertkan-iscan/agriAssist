package io.mertkaniscan.automation_engine.repositories;

import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.models.SolarResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SolarResponseRepository extends JpaRepository<SolarResponse, Long> {
    Optional<SolarResponse> findByFieldAndDate(Field fieldID, String startOfDay);
}