import socket
import struct
import json
import threading


class TCPServer:
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.handlers = {}

    def register_handler(self, task_name, handler_func):
        """Registers a module that handles a specific task."""
        self.handlers[task_name] = handler_func

    def start(self):
        """Starts the server and listens for incoming connections."""

        # Port kullanımını kontrol et
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as test_socket:
            test_socket.settimeout(1)
            if test_socket.connect_ex((self.host, self.port)) == 0:
                print(f"Port {self.port} is already in use. Server will not start.")
                return  # Port kullanılıyorsa başlatma


        # Enable SO_REUSEADDR to reuse the port
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.bind((self.host, self.port))
        server_socket.listen(5)
        print(f"Server is listening on {self.host}:{self.port}...")

        try:
            while True:
                client_socket, addr = server_socket.accept()
                print(f"Connection received from: {addr}")
                client_thread = threading.Thread(target=self.handle_client, args=(client_socket,))
                client_thread.start()

        except KeyboardInterrupt:
            print("Server shutting down gracefully.")
        finally:
            server_socket.close()

    def handle_client(self, client_socket):
        """Processes data received from the client."""
        try:
            # Receiving data
            task_name, data = self.receive_data(client_socket)

            # Execute the registered module for the specific task
            if task_name in self.handlers:
                response = self.handlers[task_name](data)
                self.send_response(client_socket, response)
            else:
                print(f"Unknown task: {task_name}")
                self.send_response(client_socket, {"error": "Unknown task"})

        except ConnectionError as e:
            print(f"Connection error: {e}")
        except Exception as e:
            print(f"Unexpected error: {e}")
        finally:
            client_socket.close()

    def receive_data(self, client_socket):
        """Receives and parses the data packet from the client."""
        try:
            data_length_bytes = client_socket.recv(4)
            if not data_length_bytes:
                raise ConnectionError("No data received for length prefix.")

            data_length = struct.unpack('>I', data_length_bytes)[0]
            data_bytes = b''
            while len(data_bytes) < data_length:
                packet = client_socket.recv(data_length - len(data_bytes))
                if not packet:
                    raise ConnectionError("Incomplete data received.")
                data_bytes += packet

            data_str = data_bytes.decode('utf-8')
            data_json = json.loads(data_str)
            task_name = data_json['task']
            task_data = data_json['data']
            return task_name, task_data

        except json.JSONDecodeError as e:
            raise ConnectionError(f"Failed to decode JSON: {e}")
        except struct.error as e:
            raise ConnectionError(f"Error unpacking data length: {e}")

    def send_response(self, client_socket, response):
        """Sends a response to the client in JSON format."""
        response_str = json.dumps(response)
        response_bytes = response_str.encode('utf-8')
        response_length = struct.pack('>I', len(response_bytes))
        client_socket.sendall(response_length)
        client_socket.sendall(response_bytes)
