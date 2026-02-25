package com.ski.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SkiInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkiInventoryApplication.class, args);
    }
}
