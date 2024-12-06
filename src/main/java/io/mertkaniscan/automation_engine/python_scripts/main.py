import os
import threading
import time
from tcp_server import TCPServer
from soil_sensor_calibrator import soil_sensor_calibrator
from soil_water_calculator import soil_water_calculator

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
    server.register_handler('soil_sensor_calibrator', soil_sensor_calibrator)
    server.register_handler('soil_water_calculator', soil_water_calculator)

    #server.register_handler('flower_stage_detection', detect_flower_stage)

    server.start()