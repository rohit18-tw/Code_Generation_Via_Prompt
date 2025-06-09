package com.mock.uidai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Main application class for the Mock UIDAI Service.
* This service simulates the UIDAI (Unique Identification Authority of India) API
* for development and testing purposes.
*
* The application provides endpoints for Aadhaar verification, authentication,
* and other UIDAI-related services.
*
* @author Mock UIDAI Team
* @version 1.0
*/
@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = {"com.mock.uidai"})
public class MockUidaiServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(MockUidaiServiceApplication.class);

    /**
    * Main method that serves as the entry point for the Spring Boot application.
    *
    * @param args Command line arguments passed to the application
    */
    public static void main(String[] args) {
        try {
            logger.info("Starting Mock UIDAI Service Application");
            SpringApplication.run(MockUidaiServiceApplication.class, args);
            logger.info("Mock UIDAI Service Application started successfully");
        } catch (Exception e) {
            logger.error("Failed to start Mock UIDAI Service Application", e);
            System.exit(1);
        }
    }
}