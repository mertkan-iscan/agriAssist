package io.mertkaniscan.automation_engine.repositories;

import io.mertkaniscan.automation_engine.models.IrrigationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IrrigationRepository extends JpaRepository<IrrigationRequest, Integer> {
    List<IrrigationRequest> findByFieldFieldID(int fieldID);
    List<IrrigationRequest> findByStatus(IrrigationRequest.IrrigationStatus status);
}
