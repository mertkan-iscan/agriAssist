import json
import numpy as np
import pandas as pd
from scipy.interpolate import griddata
import matplotlib.pyplot as plt

def interpolation_soil_water(data):
    try:
        # Extract required values
        sensor_readings = np.array(data['sensor_readings'])
        radius = data['radius']
        height = data['height']
        mode = data['mode']
        calibration_coeffs = data.get('calibration_coeffs')  # Optional

        # Convert sensor readings to a DataFrame
        sensor_data = pd.DataFrame(sensor_readings, columns=["distance", "depth", "sensor_value"])

        # Calibration function for soil moisture
        def calibrate_moisture(sensor_value, coeffs=None):
            # Default coefficients if not provided
            if coeffs is None:
                coeffs = [4.197e-7, -0.0004763, 0.2291, 2.585]  # Default 3rd-degree polynomial

            # Compute polynomial calibration dynamically based on the degree
            degree = len(coeffs) - 1
            return sum(coeff * (sensor_value ** (degree - i)) for i, coeff in enumerate(coeffs))

        # Apply calibration to sensor values
        sensor_data["calibrated_moisture"] = sensor_data["sensor_value"].apply(
            lambda x: calibrate_moisture(x, calibration_coeffs)
        )

        # Create interpolation grid
        depth_range = np.linspace(sensor_data["depth"].min(), sensor_data["depth"].max(), 50)
        distance_range = np.linspace(sensor_data["distance"].min(), sensor_data["distance"].max(), 50)
        grid_x, grid_y = np.meshgrid(distance_range, depth_range)

        # Interpolate calibrated moisture values
        grid_z_calibrated = griddata(
            points=sensor_data[["distance", "depth"]].values,
            values=sensor_data["calibrated_moisture"].values,
            xi=(grid_x, grid_y),
            method="cubic"
        )

        # Calculate the volume based on the mode
        if mode == "cylinder":
            volume = np.pi * (radius**2) * height
        elif mode == "conic":
            volume = (1/3) * np.pi * (radius**2) * height
        elif mode == "sphere":
            volume = (4/3) * np.pi * (radius**3)
        else:
            raise ValueError("Invalid mode. Choose either 'cylinder', 'conic', or 'sphere'.")

        # Compute average soil moisture
        average_moisture = np.nanmean(grid_z_calibrated)

        # Calculate water content in the soil
        soil_density = 1.3  # Typical soil density in g/cm^3
        water_weight = (average_moisture / 100) * soil_density * volume
        total_water_liters = water_weight / 1000  # Convert to liters

        return {
            "status": "success",
            "message": "total water calculated",
            "result": total_water_liters
        }

    except Exception as e:
        return {
            "status": "error",
            "message": f"water calculation failed: {str(e)}",
            "result": None
        }
