import os
import threading
import time
from tcp_server import TCPServer
from sensor_calibration import soil_moisture_calibration
from interpolation_soil_water_volume_kriging import calculate_soil_water_volume
from percentage_soil_water_volume import calculate_soil_water_volume_from_moisture
from percentage_soil_water_volume import calculate_soil_water_percentage


def is_process_running(pid):
    """Check if a process with the given PID is running."""
    try:
        # On Unix-like systems, sending signal 0 to a PID will raise an OSError if the process doesn't exist
        os.kill(pid, 0)
    except OSError:
        return False
    else:
        return True

def monitor_parent():
    """Monitor the parent process and exit if it is no longer running."""
    parent_pid = os.getppid()
    while True:
        if not is_process_running(parent_pid):
            print("Parent process has died. Exiting.")
            os._exit(1)
        time.sleep(1)  # Check every second


if __name__ == "__main__":

    #threading.Thread(target=monitor_parent, daemon=True).start()

    server = TCPServer(host='localhost', port=5432)

    # Register different tasks
    server.register_handler('soil_sensor_calibrator', soil_moisture_calibration)
    server.register_handler('soil_water_percentage', calculate_soil_water_volume)
    server.register_handler('soil_water_volume_from_calibrated_moisture', calculate_soil_water_volume_from_moisture)
    server.register_handler('soil_water_percentage_from_calibrated_moisture', calculate_soil_water_percentage)

    server.start()