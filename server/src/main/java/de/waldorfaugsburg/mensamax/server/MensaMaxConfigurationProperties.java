package de.waldorfaugsburg.mensamax.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.Map;
import java.util.Set;

@ConstructorBinding
@ConfigurationProperties("mensamax")
public record MensaMaxConfigurationProperties(String projectId, String facilityId,
                                              String username, String password,
                                              int clientCount, int identityClientCount,
                                              Map<String, Set<String>> restrictedRoles,
                                              Set<Long> restrictedProducts) {
}
