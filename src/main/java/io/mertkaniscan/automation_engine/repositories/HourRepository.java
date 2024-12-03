package io.mertkaniscan.automation_engine.repositories;

import io.mertkaniscan.automation_engine.models.Hour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HourRepository extends JpaRepository<Hour, Integer> {
}
