package io.mertkaniscan.automation_engine.services.python_module_services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Service
public class NetworkService {

    private static final Logger logger = LogManager.getLogger(NetworkService.class);

    public JSONObject sendData(String serverHost, int serverPort, JSONObject jsonData) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(serverHost, serverPort), 5000);
            socket.setSoTimeout(10000);  // 10 seconds timeout

            // Explicit streams with detailed logging
            OutputStream outputStream = socket.getOutputStream();
            DataOutputStream dataOut = new DataOutputStream(outputStream);

            InputStream inputStream = socket.getInputStream();
            DataInputStream dataIn = new DataInputStream(inputStream);

            // Convert JSON to byte array
            String jsonString = jsonData.toString();
            byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

            // Send data length and data
            logger.info("Sending data length: " + jsonBytes.length);
            dataOut.writeInt(jsonBytes.length);
            dataOut.write(jsonBytes);
            dataOut.flush();

            logger.info("Waiting to read response length");

            // Add more detailed exception handling
            try {
                int responseLength = dataIn.readInt();
                logger.info("Response length received: " + responseLength);

                if (responseLength <= 0) {
                    logger.error("Invalid response length: " + responseLength);
                    return new JSONObject().put("error", "Invalid response length.");
                }

                // Read response
                byte[] responseBytes = new byte[responseLength];
                int bytesRead = 0;
                int totalBytesRead = 0;

                while (totalBytesRead < responseLength) {
                    bytesRead = dataIn.read(responseBytes, totalBytesRead, responseLength - totalBytesRead);
                    if (bytesRead == -1) {
                        logger.error("Reached end of stream before reading full response");
                        break;
                    }
                    totalBytesRead += bytesRead;
                }

                String response = new String(responseBytes, StandardCharsets.UTF_8);
                logger.info("Full response received: " + response);

                // Parse and return the response JSON
                return new JSONObject(response);

            } catch (IOException e) {
                logger.error("Error reading response", e);
                return new JSONObject().put("error", "Response reading error: " + e.getMessage());
            }

        } catch (IOException e) {
            logger.error("Network communication error", e);
            return new JSONObject().put("error", "Network error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            return new JSONObject().put("error", "Unexpected error: " + e.getMessage());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                    logger.info("Socket closed");
                } catch (IOException e) {
                    logger.error("Error closing socket", e);
                }
            }
        }
    }
}