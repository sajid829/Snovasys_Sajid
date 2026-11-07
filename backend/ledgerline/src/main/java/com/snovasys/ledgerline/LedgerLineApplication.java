package com.snovasys.ledgerline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LedgerLineApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerLineApplication.class, args);
    }
}
