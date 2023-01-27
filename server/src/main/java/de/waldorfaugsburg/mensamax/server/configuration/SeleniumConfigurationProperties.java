package de.waldorfaugsburg.mensamax.server.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("selenium")
public record SeleniumConfigurationProperties(String driverPath, String profilePath) {
}
