package com.aicc.silverlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
//
@ConfigurationPropertiesScan
@EnableScheduling
@SpringBootApplication
public class SilverLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SilverLinkApplication.class, args);

    }

}
