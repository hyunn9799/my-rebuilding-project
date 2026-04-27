package com.aicc.silverlink.global.dev;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHashPrinter implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;

    public PasswordHashPrinter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        System.out.println("admin01 bcrypt = " + passwordEncoder.encode("admin01"));
    }
}
