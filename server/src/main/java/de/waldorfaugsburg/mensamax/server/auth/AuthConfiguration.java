package de.waldorfaugsburg.mensamax.server.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class AuthConfiguration {
    private static final String API_KEY_HEADER_NAME = "X-API-KEY";
    private final String apiKey;

    public AuthConfiguration(@Value("${auth.api-key}") final String apiKey) {
        this.apiKey = apiKey;
    }

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity httpSecurity) throws Exception {
        final ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_HEADER_NAME);
        filter.setAuthenticationManager(new ApiKeyAuthManager(apiKey));

        final ApiKeyAuthEntryPoint entryPoint = new ApiKeyAuthEntryPoint();
        httpSecurity.cors().and().csrf().disable().authorizeHttpRequests().requestMatchers("/").permitAll().requestMatchers("/api-docs/**").permitAll().requestMatchers("/swagger-ui/**").permitAll().anyRequest().authenticated().and().addFilter(filter).exceptionHandling().authenticationEntryPoint(entryPoint).and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        return httpSecurity.build();
    }
}
