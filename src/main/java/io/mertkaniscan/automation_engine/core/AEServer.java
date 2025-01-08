package io.mertkaniscan.automation_engine.core;

import io.mertkaniscan.automation_engine.config.ServerProperties;
import io.mertkaniscan.automation_engine.services.device_services.DeviceJoinService;
import io.mertkaniscan.automation_engine.services.device_services.UdpDeviceJoinService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AEServer implements CommandLineRunner {

    private final DeviceJoinService deviceJoinService;
    private final UdpDeviceJoinService udpDeviceJoinService;
    private final ServerProperties serverProperties;

    @Value("${server.join.port}")
    private int joinPort;

    public AEServer(DeviceJoinService deviceJoinService, UdpDeviceJoinService udpDeviceJoinService, ServerProperties serverProperties) {
        this.deviceJoinService = deviceJoinService;
        this.udpDeviceJoinService = udpDeviceJoinService;
        this.serverProperties = serverProperties;
    }

    @Override
    public void run(String... args) throws Exception {

        //new Thread(() -> deviceJoinService.startJoinServer(joinPort)).start();
        new Thread(() -> udpDeviceJoinService.startJoinServer(12324)).start();
    }
}