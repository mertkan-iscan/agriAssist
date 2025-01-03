package io.mertkaniscan.automation_engine.services;

import org.springframework.stereotype.Component;

@Component
public class SoilMoistureCalculatorService {

    private static final int    GRID_SIZE        = 50;
    private static final double ROTATION_STEPS   = 360.0;
    private static final double POWER            = 2.5;
    private static final double MIN_DISTANCE     = 0.0001;
    private static final double HORIZONTAL_FACTOR = 1;
    private static final double VERTICAL_FACTOR   = 1.0;


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
}
