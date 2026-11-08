package com.snovasys.ledgerline.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

public class DatabaseCredentialsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final List<String> REQUIRED_VARIABLES = List.of(
            "DB_URL",
            "DB_USERNAME",
            "DB_PASSWORD",
            "LEDGERLINE_JWT_SECRET");

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        List<String> missingVariables = new ArrayList<>();
        for (String variable : REQUIRED_VARIABLES) {
            String value = environment.getProperty(variable);
            if (value == null || value.isBlank()) {
                missingVariables.add(variable);
            }
        }

        if (!missingVariables.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required application environment variables: "
                            + String.join(", ", missingVariables)
                            + ". Set them before starting LedgerLine; see the Database setup and "
                            + "Authentication sections in README.md.");
        }
    }
}
