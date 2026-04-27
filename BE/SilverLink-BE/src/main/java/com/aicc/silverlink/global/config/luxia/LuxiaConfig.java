package com.aicc.silverlink.global.config.luxia;

import com.aicc.silverlink.infra.external.luxia.LuxiaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LuxiaProperties.class)
public class LuxiaConfig {
}
