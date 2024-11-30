package io.mertkaniscan.automation_engine.repositories;

import io.mertkaniscan.automation_engine.models.Day;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DayRepository extends JpaRepository<Day, Integer>{
}
