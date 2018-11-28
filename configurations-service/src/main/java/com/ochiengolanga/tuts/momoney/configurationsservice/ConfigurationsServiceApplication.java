package com.ochiengolanga.tuts.momoney.configurationsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer
@SpringBootApplication
public class ConfigurationsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationsServiceApplication.class, args);
    }
}
