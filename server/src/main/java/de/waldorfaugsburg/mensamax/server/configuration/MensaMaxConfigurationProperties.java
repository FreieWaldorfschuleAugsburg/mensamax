package de.waldorfaugsburg.mensamax.server.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;

@ConfigurationProperties("mensamax")
public record MensaMaxConfigurationProperties(String projectId, String facilityId,
                                              String username, String password,
                                              int clientCount) {
}
