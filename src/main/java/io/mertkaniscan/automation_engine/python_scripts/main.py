import os
import threading
import time
from tcp_server import TCPServer
from soil_sensor_calibration import calibrate_soil_sensor
from flower_stage_detection import detect_flower_stage

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
    # Start the monitor thread
    #threading.Thread(target=monitor_parent, daemon=True).start()

    server = TCPServer(host='localhost', port=5432)

    # Register different tasks
    server.register_handler('soil_sensor_calibration', calibrate_soil_sensor)
    server.register_handler('flower_stage_detection', detect_flower_stage)

    # Start the server
    server.start()