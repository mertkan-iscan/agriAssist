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

}