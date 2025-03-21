package io.mertkaniscan.automation_engine.repositories;

import io.mertkaniscan.automation_engine.models.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Integer> {

    @Query("SELECT sd FROM SensorData sd WHERE sd.field.fieldID = :fieldID AND sd.dataType = :dataType")
    List<SensorData> findByFieldIDAndDataType(@Param("fieldID") int fieldID, @Param("dataType") String dataType);

    @Query("SELECT sd FROM SensorData sd WHERE sd.field.fieldID = :fieldID AND sd.dataType = :dataType AND sd.timestamp > :since ORDER BY sd.timestamp ASC")
    List<SensorData> findByFieldIdAndTypeAndTimestampAfter(@Param("fieldID") int fieldID, @Param("dataType") String dataType, @Param("since") Timestamp since);
}