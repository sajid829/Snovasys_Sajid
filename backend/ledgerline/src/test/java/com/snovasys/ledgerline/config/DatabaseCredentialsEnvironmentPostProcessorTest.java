package com.snovasys.ledgerline.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class DatabaseCredentialsEnvironmentPostProcessorTest {

    private final DatabaseCredentialsEnvironmentPostProcessor processor =
            new DatabaseCredentialsEnvironmentPostProcessor();

    @Test
    void missingCredentialsFailWithActionableConfigurationMessage() {
        var exception = assertThrows(
                IllegalStateException.class,
                () -> processor.postProcessEnvironment(new MockEnvironment(), new SpringApplication()));

        assertTrue(exception.getMessage().contains(
                "DB_URL, DB_USERNAME, DB_PASSWORD, LEDGERLINE_JWT_SECRET"));
        assertTrue(exception.getMessage().contains("README.md"));
    }

    @Test
    void blankCredentialsAreTreatedAsMissing() {
        var environment = new MockEnvironment()
                .withProperty("DB_URL", "jdbc:postgresql://localhost:5432/ledgerline_db")
                .withProperty("DB_USERNAME", "ledgerline_app")
                .withProperty("DB_PASSWORD", "  ")
                .withProperty("LEDGERLINE_JWT_SECRET", "test-secret");

        var exception = assertThrows(
                IllegalStateException.class,
                () -> processor.postProcessEnvironment(environment, new SpringApplication()));

        assertTrue(exception.getMessage().contains("DB_PASSWORD"));
    }

    @Test
    void configuredCredentialsAllowStartupConfigurationToContinue() {
        var environment = new MockEnvironment()
                .withProperty("DB_URL", "jdbc:postgresql://localhost:5432/ledgerline_db")
                .withProperty("DB_USERNAME", "ledgerline_app")
                .withProperty("DB_PASSWORD", "configured-in-test")
                .withProperty("LEDGERLINE_JWT_SECRET", "test-secret");

        assertDoesNotThrow(
                () -> processor.postProcessEnvironment(environment, new SpringApplication()));
    }
}
