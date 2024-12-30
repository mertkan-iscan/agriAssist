package io.mertkaniscan.automation_engine.services;


import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.models.Hour;
import io.mertkaniscan.automation_engine.models.Plant;
import io.mertkaniscan.automation_engine.models.SensorData;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.main_services.SensorDataService;
import io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service.SolarResponse;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherResponse;
import io.mertkaniscan.automation_engine.utils.calculators.Calculators;

import io.mertkaniscan.automation_engine.utils.calculators.DailyEToCalculator;
import io.mertkaniscan.automation_engine.utils.calculators.HourlyEToCalculator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Service
public class CalculatorService {

    private static final int    GRID_SIZE        = 50;
    private static final double ROTATION_STEPS   = 360.0;
    private static final double POWER            = 2.5;
    private static final double MIN_DISTANCE     = 0.0001;
    private static final double HORIZONTAL_FACTOR = 1;
    private static final double VERTICAL_FACTOR   = 1.0;


    private final FieldService fieldService;
    private final SensorDataService sensorDataService;

    public CalculatorService(FieldService fieldService, SensorDataService sensorDataService) {
        this.fieldService = fieldService;
        this.sensorDataService = sensorDataService;
    }

    public static double calculateSphericalMoisture(double radius,
                                                    double[][] sensorReadings2D) {
        // 1) Grid'i kürenin geometrisine göre tanımlayalım:
        //
        //    x: [-R, +R]
        //    y: [ 0, 2R] (tepe=0, taban=2R)
        //
        // 2D IDW interpolasyonu bu x,y aralığında yapılacak.

        double[][] moistureGrid = new double[GRID_SIZE][GRID_SIZE];

        // Adım boyları (dx, dy):
        double xMin = -radius,  xMax =  radius;
        double yMin =  0,       yMax =  2 * radius;

        double dx = (xMax - xMin) / (GRID_SIZE - 1);
        double dy = (yMax - yMin) / (GRID_SIZE - 1);

        // ------------ 2D IDW (Inverse Distance Weighting) -----------
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {

                // Grid noktası (xCoord, yCoord)
                double xCoord = xMin + i * dx;
                double yCoord = yMin + j * dy;

                double weightedSum = 0.0;
                double weightSum   = 0.0;

                // Bütün sensörlerle IDW hesapla
                for (double[] sensor : sensorReadings2D) {
                    double sx = sensor[0];
                    double sy = sensor[1];
                    double sNem = sensor[2];

                    // Mesafe
                    double dx_ = (xCoord - sx) * HORIZONTAL_FACTOR;
                    double dy_ = (yCoord - sy) * VERTICAL_FACTOR;
                    double dist = Math.sqrt(dx_ * dx_ + dy_ * dy_);

                    if (dist < MIN_DISTANCE) {
                        // Sensöre çok yakınsa direkt sensör değerini al
                        moistureGrid[i][j] = sNem;
                        weightedSum = sNem;
                        weightSum   = 1.0;
                        break;
                    }

                    // IDW ağırlığı
                    double w = 1.0 / Math.pow(dist, POWER);
                    weightedSum += sNem * w;
                    weightSum   += w;
                }

                // Interpolasyon sonucu
                if (weightSum > 0) {
                    moistureGrid[i][j] = weightedSum / weightSum;
                } else {
                    moistureGrid[i][j] = 0.0;
                }
            }
        }

        // ------------- 3B Hacim Hesabı -------------
        // Dönme ekseni = y ekseni
        // x-z düzlemini y etrafında 0..360 derece döndürüyoruz.
        double angleStepRad = (2.0 * Math.PI) / ROTATION_STEPS;

        double totalMoisture = 0.0;
        double totalVolume   = 0.0;

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                // Grid'in 2D noktasındaki nem
                double xCoord = xMin + i * dx;
                double yCoord = yMin + j * dy;
                double cellMoisture = moistureGrid[i][j];

                // Bu grid noktasının "dikey" yCoord'u var.
                // Kürenin merkezi y=R noktasında. Dolayısıyla
                // "merkeze uzaklık" hesaplayabilmek için yCoord'u (yCoord - R) yapacağız.
                double yFromCenter = (yCoord - radius);

                // Silindirik yaklaşımla: r ~ |xCoord|
                // (Tabii xCoord negatif de olabilir ama yarıçap olarak pozitif alıyoruz)
                double r = Math.abs(xCoord);

                for (int angleIndex = 0; angleIndex < (int) ROTATION_STEPS; angleIndex++) {
                    double theta = angleIndex * angleStepRad;

                    // x-z düzleminde dönme (y sabit)
                    double rotatedX =  xCoord * Math.cos(theta);
                    double rotatedZ =  xCoord * Math.sin(theta);

                    // Artık 3B nokta: (rotatedX, yCoord, rotatedZ)
                    // Merkez ise (0, R, 0).
                    double distFromCenter = Math.sqrt(
                            (rotatedX * rotatedX) +
                                    (yFromCenter * yFromCenter) +
                                    (rotatedZ * rotatedZ)
                    );

                    // Bu 3B nokta küre içinde mi?
                    if (distFromCenter <= radius) {
                        // Şimdi, bu noktanın temsil ettiği küçük bir hacim dV var.
                        //
                        // dV = r * dr * dθ * dy (silindirik integral approx)
                        //  r  ~ |xCoord|
                        //  dr ~ dx
                        //  dθ = angleStepRad
                        //  dy ~ dy (grid aralığı)
                        //
                        // (Burada HORIZONTAL_FACTOR vs. kullanacaksanız
                        //  r ve dx'i ona göre çarpmalıyız.
                        //  Ama basitlik için 1.0 aldık.)

                        double dVolume = r * dx * angleStepRad * dy;

                        // Hesaba katkı
                        totalMoisture += cellMoisture * dVolume;
                        totalVolume   += dVolume;
                    }
                }
            }
        }

        // Sonuç: Toplam nem / Toplam hacim
        if (totalVolume > 0.0) {
            return totalMoisture / totalVolume;
        } else {
            return 0.0;
        }
    }

    public double calculateEToDaily(WeatherResponse weatherResponse, SolarResponse solarResponse, Field field) {

        double Tmax = weatherResponse.getDaily().get(0).getTemp().getMax();
        double Tmin = weatherResponse.getDaily().get(0).getTemp().getMin();

        double humidity = weatherResponse.getDaily().get(0).getHumidity();
        double latitude = weatherResponse.getLat();
        double pressureHpa = weatherResponse.getDaily().get(0).getPressure();

        int cloudCoverage = weatherResponse.getDaily().get(0).getClouds();
        double clearSkyGHI = solarResponse.getIrradiance().getDaily().get(0).getClearSky().getGhi();
        double cloudySkyGHI = solarResponse.getIrradiance().getDaily().get(0).getCloudySky().getGhi();
        double ghi = calculateGHI(clearSkyGHI, cloudySkyGHI, cloudCoverage);

        double windSpeed = getDailyWindSpeed(field, weatherResponse);

        double elevation = field.getElevation();

        int dayOfYear = LocalDateTime.now().getDayOfYear();


        log.info("Input - Tmax: {}", Tmax);
        log.info("Input - Tmin: {}", Tmin);
        log.info("Input - GHI: {}", ghi);
        log.info("Input - Wind Speed: {}", windSpeed);
        log.info("Input - Humidity: {}", humidity);
        log.info("Input - Latitude: {}", latitude);
        log.info("Input - Elevation: {}", elevation);
        log.info("Input - Pressure (hPa): {}", pressureHpa);
        log.info("Internal - Calculated Day of Year: {}", dayOfYear);

        double eto = DailyEToCalculator.calculateEToDaily(
                Tmax, Tmin, ghi, windSpeed, humidity, latitude, elevation, pressureHpa, dayOfYear);

        if (eto < 0) {
            log.warn("Calculated ETo (Daily) is negative! Setting to 0. Value: {}", eto);
            eto = 0.0;
        }

        return eto;
    }

    public double calculateForecastEToHourly(WeatherResponse weatherResponse, SolarResponse solarResponse, Field field, int hourIndex) {


        double temp = weatherResponse.getHourly().get(hourIndex).getTemp();
        double humidity = weatherResponse.getHourly().get(hourIndex).getHumidity();
        double latitude = field.getLatitude();
        double pressureHpa = weatherResponse.getHourly().get(hourIndex).getPressure();

        int cloudCoverage = weatherResponse.getHourly().get(hourIndex).getClouds();
        double clearSkyGHI = solarResponse.getIrradiance().getHourly().get(hourIndex).getClearSky().getGhi();
        double cloudySkyGHI = solarResponse.getIrradiance().getHourly().get(hourIndex).getCloudySky().getGhi();
        double ghi = calculateGHI(clearSkyGHI, cloudySkyGHI, cloudCoverage);

        double windSpeed = getHourlyWindSpeed(field, weatherResponse, hourIndex);

        double elevation = field.getElevation();

        // Calculate day of the year and the current hour
        int dayOfYear = LocalDateTime.now().getDayOfYear();
        int hour = LocalDateTime.now().getHour();

        // Determine if it's daytime based on solar radiation
        boolean isDaytime = ghi > 0;

        // Use the HourlyEToCalculator to calculate ETo
        double eto = HourlyEToCalculator.calculateEToHourly(
                temp, humidity, ghi, windSpeed, latitude, elevation, pressureHpa, dayOfYear, hour, isDaytime);

        // Ensure value is non-negative
        if (eto < 0) {
            log.warn("Calculated ETo (Hourly) is negative! Setting to 0. Value: {}", eto);
            eto = 0.0;
        }

        return eto;
    }

    public double calculateCurrentEToHourly(WeatherResponse weatherResponse, SolarResponse solarResponse, Field field, int hourIndex) {


        double temp = weatherResponse.getCurrent().getTemp();
        double humidity = weatherResponse.getCurrent().getHumidity();

        double latitude = field.getLatitude();
        double pressureHpa = weatherResponse.getCurrent().getPressure();

        int cloudCoverage = weatherResponse.getCurrent().getClouds();

        double clearSkyGHI = solarResponse.getIrradiance().getHourly().get(hourIndex).getClearSky().getGhi();
        double cloudySkyGHI = solarResponse.getIrradiance().getHourly().get(hourIndex).getCloudySky().getGhi();

        double ghi = calculateGHI(clearSkyGHI, cloudySkyGHI, cloudCoverage);

        double windSpeed = getHourlyWindSpeed(field, weatherResponse, hourIndex);

        double elevation = field.getElevation();

        // Calculate day of the year and the current hour
        int dayOfYear = LocalDateTime.now().getDayOfYear();
        int hour = LocalDateTime.now().getHour();

        // Determine if it's daytime based on solar radiation
        boolean isDaytime = ghi > 0;

        // Use the HourlyEToCalculator to calculate ETo
        double eto = HourlyEToCalculator.calculateEToHourly(
                temp, humidity, ghi, windSpeed, latitude, elevation, pressureHpa, dayOfYear, hour, isDaytime);

        // Ensure value is non-negative
        if (eto < 0) {
            log.warn("Calculated ETo (Hourly) is negative! Setting to 0. Value: {}", eto);
            eto = 0.0;
        }

        return eto;
    }



    public double calculateSensorEToHourly(Double sensorTemp, Double sensorHumidity, WeatherResponse weatherResponse, SolarResponse solarResponse, Field field, int hourIndex) {


        double temp = sensorTemp;
        double humidity = sensorHumidity;

        double latitude = field.getLatitude();
        double pressureHpa = weatherResponse.getCurrent().getPressure();

        double ghi = calculateSolarRadiationHourly(weatherResponse, solarResponse, hourIndex);

        double windSpeed = getHourlyWindSpeed(field, weatherResponse, hourIndex);

        double elevation = field.getElevation();

        // Calculate day of the year and the current hour
        int dayOfYear = LocalDateTime.now().getDayOfYear();
        int hour = LocalDateTime.now().getHour();

        // Determine if it's daytime based on solar radiation
        boolean isDaytime = ghi > 0;

        // Use the HourlyEToCalculator to calculate ETo
        double eto = HourlyEToCalculator.calculateEToHourly(
                temp, humidity, ghi, windSpeed, latitude, elevation, pressureHpa, dayOfYear, hour, isDaytime);

        // Ensure value is non-negative
        if (eto < 0) {
            log.warn("Calculated ETo (Hourly) is negative! Setting to 0. Value: {}", eto);
            eto = 0.0;
        }

        return eto;
    }

    public double calculateSolarRadiationHourly(WeatherResponse weatherResponse, SolarResponse solarResponse, int hourIndex){

        int cloudCoverage = weatherResponse.getCurrent().getClouds();

        double clearSkyGHI = solarResponse.getIrradiance().getHourly().get(hourIndex).getClearSky().getGhi();
        double cloudySkyGHI = solarResponse.getIrradiance().getHourly().get(hourIndex).getCloudySky().getGhi();

        return calculateGHI(clearSkyGHI, cloudySkyGHI, cloudCoverage);
    }

    public double getHourlyWindSpeed(Field field, WeatherResponse weatherResponse, int hourIndex) {
        if (field.getFieldType() == Field.FieldType.GREENHOUSE) {
            return 0.0;
        }
        return weatherResponse.getHourly().get(hourIndex).getWindSpeed();
    }

    public double getDailyWindSpeed(Field field, WeatherResponse weatherResponse) {
        if (field.getFieldType() == Field.FieldType.GREENHOUSE) {
            return 0.0;
        }
        return weatherResponse.getDaily().get(0).getWindSpeed();
    }

    public double calculateKe(Field field) {

        double KcbAdjusted = field.getPlantInField().getCurrentKcValue();

        double humidity = 0;
        double windSpeed = 0;
        double De = 0;
        double TEW = 0;
        double REW = 0;

        double KcMax = Calculators.calculateKcMax(KcbAdjusted, humidity, windSpeed);
        double Kr = Calculators.calculateKr(De, TEW, REW);
        double fw = calculateFw();

        return Calculators.calculateKe(Kr, fw, KcMax, KcbAdjusted);
    }

    public double calculateTEW(Field field){

        double fieldCapacity = field.getFieldCapacity();
        double fieldWiltingPoint = field.getWiltingPoint();
        double plantCurrentRootDepth = field.getPlantInField().getCurrentRootZoneDepth();
        double fieldMaxEvoporationdepth = field.getMaxEvaporationDepth();

        double Ze = Math.min(plantCurrentRootDepth, fieldMaxEvoporationdepth);

        return Calculators.calculateTEW(fieldCapacity, fieldWiltingPoint, Ze);
    }

    private double calculateFwForDripIrrigation(double irrigationDuration, double emitterRate, double totalArea) {
        double waterVolume = irrigationDuration * emitterRate;
        double wettedArea = waterVolume / 10.0;

        return Calculators.calculateFw(wettedArea, totalArea);
    }

    public double calculateFw() {
        return 0.5;
    }

    public double calculateGHI(double clearSkyGHI, double cloudySkyGHI, int cloudCoverage) {
        if (cloudCoverage < 0 || cloudCoverage > 100) {
            throw new IllegalArgumentException("Cloud coverage must be between 0 and 100.");
        }

        // Calculate GHI based on cloud coverage
        double ghi = (1 - (cloudCoverage / 100.0)) * clearSkyGHI + (cloudCoverage / 100.0) * cloudySkyGHI;

        log.info("Calculated GHI: {} (Clear Sky GHI: {}, Cloudy Sky GHI: {}, Cloud Coverage: {}%)",
                ghi, clearSkyGHI, cloudySkyGHI, cloudCoverage);

        return ghi;
    }

    public double calculateSensorTAW(Field field, int minutesBack) {

        double currentRootDepth = field.getPlantInField().getCurrentRootZoneDepth();
        double fieldWiltingPoint = field.getWiltingPoint();

        Timestamp since = new Timestamp(System.currentTimeMillis() - minutesBack * 60 * 1000L);

        List<SensorData> sensorDataList = sensorDataService.findByFieldIdAndTypeAndTimestampAfter(
                field.getFieldID(), "soil_moisture", since);

        if (sensorDataList.isEmpty()) {
            throw new IllegalStateException("No sensor data available for the specified time range.");
        }

        double[][] calibratedMoisture = new double[sensorDataList.size()][];
        for (int i = 0; i < sensorDataList.size(); i++) {
            Map<String, Double> dataValues = sensorDataList.get(i).getDataValues();
            calibratedMoisture[i] = dataValues.values().stream().mapToDouble(Double::doubleValue).toArray();
        }

        double soilWaterPercentage = calculateSphericalMoisture(currentRootDepth, calibratedMoisture);

        return Calculators.calculateSensorTAW(soilWaterPercentage, fieldWiltingPoint, currentRootDepth);
    }

    private double calculateFwForField(Field field) {
        double wettedArea = field.getCurrentWetArea();
        return Calculators.calculateFw(wettedArea, field.getTotalArea());
    }

    public double calculateSensorKr(Field field) {

        double evaporationCoeff = field.getEvaporationCoeff();
        double fieldCurrentDeValue = field.getCurrentDeValue();

        // Calculate TEW and REW
        Double sensorTEWValue = calculateSensorTEW(field, 10);

        double sensorREWValue = sensorTEWValue * evaporationCoeff;

        // Logic for Kr calculation
        if (fieldCurrentDeValue >= sensorTEWValue) {
            return 0.0; // All water depleted
        }
        if (fieldCurrentDeValue <= sensorREWValue) {
            return 1.0; // No reduction in evaporation
        }

        // Linear interpolation for Kr
        return (sensorTEWValue - fieldCurrentDeValue) / (sensorTEWValue - sensorREWValue);
    }

    public double calculateSensorTEW(Field field, int minutesBack) {

        double maxEvaporationDepth = field.getMaxEvaporationDepth();
        double fieldWiltingPoint = field.getWiltingPoint();

        // Calculate the timestamp for the specified time window
        Timestamp since = new Timestamp(System.currentTimeMillis() - minutesBack * 60 * 1000L);

        // Fetch recent sensor data for soil moisture
        List<SensorData> sensorDataList = sensorDataService.findByFieldIdAndTypeAndTimestampAfter(
                field.getFieldID(), "soil_moisture", since);

        if (sensorDataList.isEmpty()) {
            throw new IllegalStateException("No sensor data available for the specified time range.");
        }

        // Prepare the calibrated moisture array
        double[][] calibratedMoisture = new double[sensorDataList.size()][];
        for (int i = 0; i < sensorDataList.size(); i++) {
            Map<String, Double> dataValues = sensorDataList.get(i).getDataValues();
            calibratedMoisture[i] = dataValues.values().stream().mapToDouble(Double::doubleValue).toArray();
        }


        double soilWaterPercentage = calculateSphericalMoisture(maxEvaporationDepth, calibratedMoisture);

        return Calculators.calculateSensorTEW(soilWaterPercentage, fieldWiltingPoint, maxEvaporationDepth);
    }
}
