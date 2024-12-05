package io.mertkaniscan.automation_engine.core;

import io.mertkaniscan.automation_engine.config.ServerProperties;
import io.mertkaniscan.automation_engine.services.device_services.DeviceJoinService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AEServer implements CommandLineRunner {

    private final DeviceJoinService deviceJoinService;
    private final ServerProperties serverProperties;

    @Value("${server.join.port}")
    private int joinPort;

    public AEServer(DeviceJoinService deviceJoinService, ServerProperties serverProperties) {
        this.deviceJoinService = deviceJoinService;
        this.serverProperties = serverProperties;
    }

    @Override
    public void run(String... args) throws Exception {

        new Thread(() -> deviceJoinService.startJoinServer(joinPort)).start();
    }
}