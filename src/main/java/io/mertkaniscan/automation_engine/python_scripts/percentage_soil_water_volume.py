import numpy as np
import pandas as pd
from pykrige.ok import OrdinaryKriging

def calculate_soil_water_volume_from_moisture(data):
    try:
        # Extract required values
        calibrated_moisture = np.array(data['calibrated_moisture'])  # Already calibrated values
        radius = data['radius']
        height = data['height']

        # Convert calibrated moisture readings to a DataFrame
        moisture_data = pd.DataFrame(calibrated_moisture, columns=["distance", "depth", "moisture"])

        # Create interpolation grid
        depth_range = np.linspace(moisture_data["depth"].min(), moisture_data["depth"].max(), 50)
        distance_range = np.linspace(moisture_data["distance"].min(), moisture_data["distance"].max(), 50)
        grid_x, grid_y = np.meshgrid(distance_range, depth_range)

        # Perform Ordinary Kriging
        kriging_model = OrdinaryKriging(
            moisture_data["distance"].values,
            moisture_data["depth"].values,
            moisture_data["moisture"].values,
            variogram_model="linear"
        )

        grid_z_moisture, _ = kriging_model.execute("grid", distance_range, depth_range)

        # Mask grid points outside the defined cylindrical volume
        mask = (grid_x**2 + grid_y**2) <= radius**2
        grid_z_moisture[~mask] = np.nan

        # Integrate moisture content over the defined cylindrical volume
        dx = distance_range[1] - distance_range[0]
        dy = depth_range[1] - depth_range[0]
        volume_element = dx * dy * height

        total_water_volume = np.nansum(grid_z_moisture * volume_element)

        return {
            "status": "success",
            "message": "Total water volume calculated from calibrated moisture",
            "result": total_water_volume
        }

    except Exception as e:
        return {
            "status": "error",
            "message": f"calculation failed: {str(e)}",
            "result": None
        }


def calculate_soil_water_percentage(data):
    try:
        # Extract required values
        calibrated_moisture = np.array(data['calibrated_moisture'])  # Already calibrated values
        radius = data['radius']
        height = data['height']

        # Convert calibrated moisture readings to a DataFrame
        moisture_data = pd.DataFrame(calibrated_moisture, columns=["distance", "depth", "moisture"])

        # Create interpolation grid
        depth_range = np.linspace(moisture_data["depth"].min(), moisture_data["depth"].max(), 50)
        distance_range = np.linspace(moisture_data["distance"].min(), moisture_data["distance"].max(), 50)
        grid_x, grid_y = np.meshgrid(distance_range, depth_range)

        # Perform Ordinary Kriging
        kriging_model = OrdinaryKriging(
            moisture_data["distance"].values,
            moisture_data["depth"].values,
            moisture_data["moisture"].values,
            variogram_model="linear"
        )

        grid_z_moisture, _ = kriging_model.execute("grid", distance_range, depth_range)

        # Mask grid points outside the defined cylindrical volume
        mask = (grid_x**2 + grid_y**2) <= radius**2
        grid_z_moisture[~mask] = np.nan

        # Calculate soil water percentage
        dx = distance_range[1] - distance_range[0]
        dy = depth_range[1] - depth_range[0]
        area_element = dx * dy

        total_area = np.sum(mask) * area_element
        average_moisture_percentage = np.nanmean(grid_z_moisture) * 100

        return {
            "status": "success",
            "message": "Average soil water percentage calculated from calibrated moisture",
            "result": average_moisture_percentage
        }

    except Exception as e:
        return {
            "status": "error",
            "message": f"calculation failed: {str(e)}",
            "result": None
        }
