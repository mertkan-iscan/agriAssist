import json
import numpy as np
import pandas as pd
from pykrige.ok import OrdinaryKriging
import matplotlib.pyplot as plt

def calculate_soil_water_volume(data):
    try:
        # Extract required values
        sensor_readings = np.array(data['sensor_readings'])
        radius = data['radius']
        height = data['height']
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

        # Perform Ordinary Kriging
        kriging_model = OrdinaryKriging(
            sensor_data["distance"].values,
            sensor_data["depth"].values,
            sensor_data["calibrated_moisture"].values,
            variogram_model="linear"
        )

        grid_z_calibrated, _ = kriging_model.execute("grid", distance_range, depth_range)

        # Mask grid points outside the defined cylindrical volume
        mask = (grid_x**2 + grid_y**2) <= radius**2
        grid_z_calibrated[~mask] = np.nan

        # Integrate moisture content over the defined cylindrical volume
        dx = distance_range[1] - distance_range[0]
        dy = depth_range[1] - depth_range[0]
        volume_element = dx * dy * height

        total_water_volume = np.nansum(grid_z_calibrated * volume_element)

        return {
            "status": "success",
            "message": "Total water volume within the defined area calculated",
            "result": total_water_volume  # Return total water volume
        }

    except Exception as e:
        return {
            "status": "error",
            "message": f"calculation failed: {str(e)}",
            "result": None
        }
