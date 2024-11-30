import numpy as np

def calibrate_soil_sensor(data):
    """Toprak sensörü kalibrasyonu için polinom uydurma işlemi."""
    sensor_readings = np.array(data['sensor_readings'])
    moisture_percentages = np.array(data['moisture_percentages'])
    sensor_diff = 1023 - sensor_readings
    degrees = [1, 2, 3, 4]

    best_polynomial = None
    best_r_squared = -np.inf
    best_degree = None
    best_aic = np.inf
    best_bic = np.inf

    n = len(sensor_readings)  # Veri sayısı

    for degree in degrees:
        coefficients = np.polyfit(sensor_diff, moisture_percentages, degree)
        polynomial = np.poly1d(coefficients)
        predicted_moisture = polynomial(sensor_diff)
        residuals = moisture_percentages - predicted_moisture
        rss = np.sum(residuals ** 2)
        ss_tot = np.sum((moisture_percentages - np.mean(moisture_percentages)) ** 2)
        r_squared = 1 - (rss / ss_tot)

        # AIC ve BIC hesaplama
        k = degree + 1  # Polinom derecesi + sabit terim
        aic = n * np.log(rss / n) + 2 * k
        bic = n * np.log(rss / n) + k * np.log(n)

        # En iyi modelin seçimi
        if aic < best_aic and bic < best_bic:
            best_r_squared = r_squared
            best_polynomial = coefficients
            best_degree = degree
            best_aic = aic
            best_bic = bic

    return {
        'degree': best_degree,
        'coefficients': best_polynomial.tolist(),
        'r_squared': best_r_squared,
        'aic': best_aic,
        'bic': best_bic
    }
