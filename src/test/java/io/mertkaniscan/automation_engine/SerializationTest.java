package io.mertkaniscan.automation_engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.mertkaniscan.automation_engine.models.SolarResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;

public class SerializationTest {

    @Test
    public void testSerialization() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Create test data
        SolarResponse solarResponse = new SolarResponse();
        solarResponse.setId(1L);
        solarResponse.setLat(42.0);
        solarResponse.setLon(-71.0);
        solarResponse.setDate("2024-12-15");
        solarResponse.setTz("UTC");
        solarResponse.setSunrise("06:00");
        solarResponse.setSunset("18:00");

        SolarResponse.Irradiance irradiance = new SolarResponse.Irradiance();
        SolarResponse.Irradiance.DailyIrradiance daily = new SolarResponse.Irradiance.DailyIrradiance();
        daily.setId(101L);
        SolarResponse.Irradiance.SkyIrradiance sky = new SolarResponse.Irradiance.SkyIrradiance();
        sky.setGhi(100.0);
        sky.setDni(50.0);
        sky.setDhi(30.0);
        daily.setClearSky(sky);

        irradiance.setDaily(List.of(daily));
        solarResponse.setIrradiance(irradiance);

        // Serialize
        String json = objectMapper.writeValueAsString(solarResponse);
        System.out.println(json);

        // Deserialize
        SolarResponse deserialized = objectMapper.readValue(json, SolarResponse.class);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getIrradiance());
        assertNotNull(deserialized.getIrradiance().getDaily());
    }
}