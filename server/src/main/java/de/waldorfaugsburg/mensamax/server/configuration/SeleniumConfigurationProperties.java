package de.waldorfaugsburg.mensamax.server.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties("selenium")
public record SeleniumConfigurationProperties(String driverPath, String profilePath) {
}
