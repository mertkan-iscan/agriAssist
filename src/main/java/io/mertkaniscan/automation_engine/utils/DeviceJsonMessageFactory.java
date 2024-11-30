package io.mertkaniscan.automation_engine.utils;

public class DeviceJsonMessageFactory {

    public static String createDeviceJoinResponse(String joinResponse) {
        return "{" +
                "\"messageType\": \"" + joinResponse + "\"" +
                "}";
    }

    public static String pullSensorData() {
        return "{" +
                "\"messageType\": \"send_sensordata\"" +
                "}";
    }

    public static String createActuatorCommand(String messageType) {
        return "{" +
                "\"messageType\": \"" + messageType + "\"" +
                "}";
    }

    public static String createValveActuatorCommand(int degree) {
        return "{" +
                "\"messageType\": \"set_valve\"," +
                "\"degree\":" + degree +
                "}";
    }
}