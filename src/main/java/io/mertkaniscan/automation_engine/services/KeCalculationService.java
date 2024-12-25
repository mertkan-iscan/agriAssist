package io.mertkaniscan.automation_engine.services;

import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.models.Plant;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import io.mertkaniscan.automation_engine.repositories.HourRepository;
import io.mertkaniscan.automation_engine.models.Hour;

@Service
public class KeCalculationService {

    @Autowired
    private HourRepository hoursRepository;

    @Autowired
    private FieldService fieldService;

    @Autowired
    private CalculatorService calculatorService;

    @Transactional
    public void calculateAndUpdateKeValues(int fieldID) {

        Field field = fieldService.getFieldById(fieldID);
        Plant plant = field.getPlantInField();

        List<Hour> hoursList = hoursRepository.findAll();

        //for (Hour hour : hoursList) {

            // Gather required parameters for the Ke calculation
            //double fw = calculatorService.calculateFw(fieldID);
            //double KcMax = Calculators.calculateKcMax(plant.getCurrentKcValue(), hour.getSensorHumidity(), hour.getSensorWindSpeed());
//
            //// Calculate Ke using Calculators utility
            //double Ke = calculatorService.calculateKe(Kr, fw, KcMax, hour.getDeValue(), hour.getTEWValue(), hour.getREWValue());

            //field.setCurrentKeValue(Ke);
            //field.setCurrentTEWValue();
            //field.setCurrentREWValue();
//
            //hour.setKeValue(Ke);
            //hour.setLastUpdated(LocalDateTime.now());
        //}

        hoursRepository.saveAll(hoursList);
    }
}