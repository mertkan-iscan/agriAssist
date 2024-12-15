package io.mertkaniscan.automation_engine.repositories;

import io.mertkaniscan.automation_engine.models.WeatherResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherResponseRepository extends JpaRepository<WeatherResponse, Long> {
}
