package io.mertkaniscan.automation_engine.core;

import io.mertkaniscan.automation_engine.config.ServerProperties;
import io.mertkaniscan.automation_engine.services.device_services.DeviceJoinService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AEServer implements CommandLineRunner {

    private final DeviceJoinService deviceJoinService;
    private final ServerProperties serverProperties;

    public AEServer(DeviceJoinService deviceJoinService, ServerProperties serverProperties) {
        this.deviceJoinService = deviceJoinService;
        this.serverProperties = serverProperties;
    }

    @Override
    public void run(String... args) throws Exception {

        int joinPort = serverProperties.getJoin().getPort();

        // Start client join server thread
        new Thread(() -> deviceJoinService.startJoinServer(joinPort)).start();

    }
}