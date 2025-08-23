package de.waldorfaugsburg.mensamax.server.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public record ApiKeyAuthManager(String apiKey) implements AuthenticationManager {

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        final String principal = (String) authentication.getPrincipal();
        if (apiKey.equals(principal)) {
            authentication.setAuthenticated(true);
        }
        return authentication;
    }
}
