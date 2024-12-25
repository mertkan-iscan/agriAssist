package io.mertkaniscan.automation_engine;

import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherResponse;
import io.mertkaniscan.automation_engine.repositories.FieldRepository;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherForecastService;
import io.mertkaniscan.automation_engine.services.main_services.DeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WeatherForecastTest {

    @Mock
    private FieldRepository fieldRepository;

    @Mock
    private DeviceService deviceService;

    @Mock
    private WeatherForecastService weatherForecastService;

    @InjectMocks
    private FieldService fieldService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFetchWeatherByFieldId() {
        // Arrange
        int fieldID = 1;

        Field mockField = new Field();
        mockField.setFieldID(fieldID);
        mockField.setLatitude(40.7128);
        mockField.setLongitude(-74.0060);

        WeatherResponse mockWeatherResponse = new WeatherResponse();
        // Set other properties of WeatherResponse as needed

        when(fieldRepository.findById(fieldID)).thenReturn(Optional.of(mockField));
        when(weatherForecastService.getAndParseWeatherData(40.7128, -74.0060))
                .thenReturn(mockWeatherResponse);

        // Act
        WeatherResponse result = fieldService.getWeatherDataByFieldId(fieldID);

        // Assert
        assertNotNull(result);
        verify(fieldRepository, times(1)).findById(fieldID);
        verify(weatherForecastService, times(1)).getAndParseWeatherData(40.7128, -74.0060);
    }
}
