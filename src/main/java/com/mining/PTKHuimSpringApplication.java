package com.mining;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Boot main application class for PTK-HUIM-U.
 * Supports both REST API and command-line modes.
 */
@SpringBootApplication
@EnableAsync
@Slf4j
public class PTKHuimSpringApplication {

    public static void main(String[] args) {
        // Check if running in CLI mode (4+ arguments = database, profit, k, minProb)
        if (args.length >= 4) {
            log.info("Starting in CLI mode with {} arguments", args.length);
            // Run original CLI application
            PTKHuimApplication.main(args);
        } else {
            log.info("Starting Spring Boot application");
            // Run Spring Boot application
            SpringApplication.run(PTKHuimSpringApplication.class, args);
        }
    }

    /**
     * Welcome message on startup
     */
    @Bean
    public CommandLineRunner startup() {
        return (args) -> {
            log.info("=".repeat(60));
            log.info("PTK-HUIM-U Spring Boot Application Started Successfully!");
            log.info("=".repeat(60));
            log.info("REST API available at: http://localhost:8080");
            log.info("API Documentation: http://localhost:8080/swagger-ui.html");
            log.info("H2 Console: http://localhost:8080/h2-console");
            log.info("Health Check: http://localhost:8080/actuator/health");
            log.info("=".repeat(60));
            log.info("For CLI mode, restart with 4 arguments:");
            log.info("java -jar app.jar <database_file> <profit_file> <k> <min_probability>");
            log.info("=".repeat(60));
        };
    }
}