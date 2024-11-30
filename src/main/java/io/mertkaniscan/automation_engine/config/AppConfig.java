package io.mertkaniscan.automation_engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@ComponentScan(basePackages = "io.mertkaniscan.automation_engine")
public class AppConfig {

    @Bean
    public ExecutorService clientHandlerPool() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public AtomicInteger activeDeviceTCPConnections() {
        return new AtomicInteger(0);
    }
}
