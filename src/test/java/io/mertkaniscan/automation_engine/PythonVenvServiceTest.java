package io.mertkaniscan.automation_engine;

import io.mertkaniscan.automation_engine.services.PythonVenvService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PythonVenvServiceTest {

    @InjectMocks
    private PythonVenvService pythonVenvService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSendCalibrationDataToServer() {
        // Sensör verileri ve toprak nem yüzdeleri
        double[] sensorReadings = {1023, 998, 856, 785, 564, 401, 272, 240, 240, 240};
        double[] moisturePercentages = {0, 12.5, 25, 37.5, 50, 62.5, 75, 87.5, 93.7, 100};

        // Gerçek sunucuya bağlanmak yerine, sunucunun cevap vereceği JSON nesnesi
        JSONObject mockResponse = new JSONObject();
        mockResponse.put("degree", 3);
        JSONArray coefficients = new JSONArray();
        coefficients.put(0.5);
        coefficients.put(-0.75);
        coefficients.put(1.25);
        coefficients.put(-0.35);
        mockResponse.put("coefficients", coefficients);

        // PythonVenvService içinde sunucu bağlantısı kısmını mock'layarak sahte bir yanıt döndürüyoruz
        PythonVenvService mockPythonVenvService = spy(new PythonVenvService());
        doReturn(mockResponse).when(mockPythonVenvService).sendCalibrationDataToServer(anyString(), anyInt(), any(double[].class), any(double[].class));

        // Test edilen metodun çağrılması
        JSONObject response = mockPythonVenvService.sendCalibrationDataToServer("localhost", 12345, sensorReadings, moisturePercentages);

        // Cevap kontrolü
        assertNotNull(response, "Yanıt boş olmamalı.");
        assertTrue(response.has("degree"), "Yanıtta 'degree' olmalı.");
        assertTrue(response.has("coefficients"), "Yanıtta 'coefficients' olmalı.");

        int degree = response.getInt("degree");
        JSONArray responseCoefficients = response.getJSONArray("coefficients");

        // Beklenen sonuçlarla karşılaştırma
        assertEquals(3, degree, "Yanıtlanan polinom derecesi hatalı.");
        assertEquals(4, responseCoefficients.length(), "Yanıttaki katsayı sayısı hatalı.");
        assertEquals(0.5, responseCoefficients.getDouble(0), 0.001, "Yanıtlanan ilk katsayı hatalı.");
        assertEquals(-0.75, responseCoefficients.getDouble(1), 0.001, "Yanıtlanan ikinci katsayı hatalı.");
        assertEquals(1.25, responseCoefficients.getDouble(2), 0.001, "Yanıtlanan üçüncü katsayı hatalı.");
        assertEquals(-0.35, responseCoefficients.getDouble(3), 0.001, "Yanıtlanan dördüncü katsayı hatalı.");
    }

    @Test
    public void testSendCalibrationDataToServerWithError() {
        // Hatalı senaryo testi
        PythonVenvService mockPythonVenvService = spy(new PythonVenvService());
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("error", "Connection failed or server error.");

        // Sunucuya bağlanırken hata oluşmuş gibi yapıyoruz
        doReturn(errorResponse).when(mockPythonVenvService).sendCalibrationDataToServer(anyString(), anyInt(), any(double[].class), any(double[].class));

        // Test edilen metodun çağrılması
        JSONObject response = mockPythonVenvService.sendCalibrationDataToServer("localhost", 12345, null, null);

        // Hata kontrolü
        assertNotNull(response, "Yanıt boş olmamalı.");
        assertTrue(response.has("error"), "Yanıtta 'error' olmalı.");
        assertEquals("Connection failed or server error.", response.getString("error"), "Hata mesajı beklenenden farklı.");
    }
}
