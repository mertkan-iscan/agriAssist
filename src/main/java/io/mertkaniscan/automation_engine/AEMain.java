package io.mertkaniscan.automation_engine;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = "io.mertkaniscan.automation_engine.models")
@EnableJpaRepositories(basePackages = "io.mertkaniscan.automation_engine.repositories")
public class AEMain {
    public static void main(String[] args) {
        SpringApplication.run(AEMain.class, args);
    }
}