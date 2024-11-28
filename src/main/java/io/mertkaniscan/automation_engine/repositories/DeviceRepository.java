package io.mertkaniscan.automation_engine.repositories;

import io.mertkaniscan.automation_engine.models.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Integer> {

    List<Device> findByFieldFieldID(int fieldID);

    @Query("SELECT d FROM Device d WHERE d.field.fieldID = :fieldID AND d.deviceType = 'SOIL_SENSOR'")
    List<Device> findSoilSensorsByFieldId(@Param("fieldID") int fieldID);
}
