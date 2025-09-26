// File: src/main/java/com/mining/SpringBootMiningApplication.java
package com.mining;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableScheduling
public class SpringBootMiningApplication {
    
    public static void main(String[] args) {
        // Can run both CLI and API modes
        if (args.length > 0 && args[0].equals("--cli")) {
            // Run existing CLI application
            PTKHuimApplication.main(Arrays.copyOfRange(args, 1, args.length));
        } else {
            // Run Spring Boot API
            SpringApplication.run(SpringBootMiningApplication.class, args);
        }
    }
}