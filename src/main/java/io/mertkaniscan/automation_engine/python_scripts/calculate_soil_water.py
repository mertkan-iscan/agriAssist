import numpy as np
import pandas as pd
from scipy.interpolate import griddata
import matplotlib.pyplot as plt

def calculate_soil_water(sensor_readings, radius, height, mode="cylinder"):
    """
    Calculate the total water content in a soil volume based on sensor readings.

    Parameters:
        sensor_readings (list of lists): Sensor data in the format [[distance, depth, sensor_value], ...].
        radius (float): Radius of the area (in cm).
        height (float): Height of the area (in cm).
        mode (str): Calculation mode, either "cylinder", "conic", or "sphere".

    Returns:
        float: Total water content in the given soil volume (in liters).
    """
    # Convert sensor readings to a DataFrame
    sensor_data = pd.DataFrame(sensor_readings, columns=["distance", "depth", "sensor_value"])

    # Calibration function for soil moisture
    def calibrate_moisture(sensor_value):
        return (
            4.197e-7 * sensor_value**3
            - 0.0004763 * sensor_value**2
            + 0.2291 * sensor_value
            + 2.585
        )

    # Apply calibration to sensor values
    sensor_data["calibrated_moisture"] = calibrate_moisture(sensor_data["sensor_value"])

    # Create interpolation grid
    depth_range = np.linspace(sensor_data["depth"].min(), sensor_data["depth"].max(), 50)
    distance_range = np.linspace(sensor_data["distance"].min(), sensor_data["distance"].max(), 50)
    grid_x, grid_y = np.meshgrid(distance_range, depth_range)

    # Interpolate calibrated moisture values
    grid_z_calibrated = griddata(
        points=sensor_data[["distance", "depth"].values],
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

    # Plot interpolated moisture distribution
    plt.figure(figsize=(10, 6))
    plt.contourf(grid_x, grid_y, grid_z_calibrated, levels=50, cmap="viridis")
    plt.colorbar(label="Soil Moisture (%)")
    plt.title("Interpolated Soil Moisture Distribution (%)")
    plt.gca().invert_yaxis()
    plt.xlabel("Distance (cm)")
    plt.ylabel("Depth (cm)")
    plt.gca().xaxis.tick_top()
    plt.gca().xaxis.set_label_position('top')
    plt.show()

    return total_water_liters