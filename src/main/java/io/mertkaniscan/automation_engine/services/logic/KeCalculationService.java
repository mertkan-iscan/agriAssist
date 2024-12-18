package io.mertkaniscan.automation_engine.services;

import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.models.Plant;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.utils.calculators.Calculators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import io.mertkaniscan.automation_engine.repositories.HourRepository;
import io.mertkaniscan.automation_engine.models.Hour;

@Service
public class KeCalculationService {

    @Autowired
    private HourRepository hoursRepository;
    @Autowired
    private FieldService fieldService;

    @Transactional
    public void calculateAndUpdateKeValues(int fieldID) {

        Field field = fieldService.getFieldById(fieldID);
        Plant plant = field.getPlantInField();

        List<Hour> hoursList = hoursRepository.findAll();

        for (Hour hour : hoursList) {

            // Gather required parameters for the Ke calculation
            double Kr = Calculators.calculateKr(hour.getDeValue(), hour.getTEWValue(), hour.getREWValue());
            double fw = Calculators.calculateFw(hour.get(), hour.getTotalArea());
            double KcMax = Calculators.calculateKcMax(plant.getKcBasal(), hour.getHumidity(), hour.getWindSpeed());

            // Calculate Ke using Calculators utility
            double Ke = Calculators.calculateKe(Kr, fw, KcMax, hour.getDeValue(), hour.getTEWValue(), hour.getREWValue());

            field.setCurrentKeValue();
            field.setCurrentTEWValue();
            field.setCurrentREWValue();

            hour.setKeValue(Ke);
            hour.setLastUpdated(LocalDateTime.now());
        }

        hoursRepository.saveAll(hoursList);
    }
}