package io.mertkaniscan.automation_engine.utils;

import io.mertkaniscan.automation_engine.models.Device;

import java.net.Socket;

public class DeviceSocketWrapper {
    private Socket socket;
    private Device device;

    public DeviceSocketWrapper(Socket socket, Device device) {
        this.socket = socket;
        this.device = device;
    }

    public Socket getSocket() {
        return socket;
    }

    public Device getDeviceObj() {
        return device;
    }

    public int getDeviceID() {
        return device.getDeviceID();
    }

    public String getDeviceIp() {
        return device.getDeviceIp();
    }

    public String getDeviceType() {
        return device.getDeviceType();
    }

    public String getDeviceModel() {
        return device.getDeviceModel();
    }

    public Device.DeviceStatus getDeviceStatus() {
        return device.getDeviceStatus();
    }

    public void setDeviceStatus(Device.DeviceStatus deviceStatus) {
        this.device.setDeviceStatus(deviceStatus);
    }
}