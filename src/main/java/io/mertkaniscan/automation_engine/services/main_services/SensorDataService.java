package io.mertkaniscan.automation_engine.services.main_services;

import io.mertkaniscan.automation_engine.models.SensorData;
import io.mertkaniscan.automation_engine.repositories.SensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class SensorDataService {

    private final SensorDataRepository sensorDataRepository;

    public SensorDataService(SensorDataRepository sensorDataRepository) {
        this.sensorDataRepository = sensorDataRepository;
    }

    public SensorData saveSensorData(SensorData sensorData) {
        return sensorDataRepository.save(sensorData);
    }

    public List<SensorData> getAllSensorData() {
        return sensorDataRepository.findAll();
    }

    public SensorData getSensorDataById(int id) {
        return sensorDataRepository.findById(id).orElse(null);
    }


    public boolean deleteSensorData(int id) {
        if (sensorDataRepository.existsById(id)) {
            sensorDataRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<SensorData> getSensorDataByFieldIDAndTypeFromDb(int fieldID, String dataType) {
        return sensorDataRepository.findByFieldIDAndDataType(fieldID, dataType);
    }

    public List<SensorData> findByFieldIdAndTypeAndTimestampAfter(int fieldID, String dataType, Timestamp since) {
        return sensorDataRepository.findByFieldIdAndTypeAndTimestampAfter(fieldID, dataType, since);
    }

    public List<SensorData> getSensorDataByFieldIDAndTypeWithinLastDaysFromDb(int fieldID, String dataType, int days) {
        Timestamp since = new Timestamp(System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L));
        return sensorDataRepository.findByFieldIdAndTypeAndTimestampAfter(fieldID, dataType, since);
    }
}
