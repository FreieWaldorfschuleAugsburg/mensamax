package de.waldorfaugsburg.mensamax.server.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
public class AuthConfiguration extends WebSecurityConfigurerAdapter {

    private static final String API_KEY_HEADER_NAME = "X-API-KEY";
    private final String apiKey;

    public AuthConfiguration(@Value("${auth.api-key}") final String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected void configure(final HttpSecurity httpSecurity) throws Exception {
        final ApiKeyAuthFilter filter = new ApiKeyAuthFilter(API_KEY_HEADER_NAME);
        filter.setAuthenticationManager(new ApiKeyAuthManager(apiKey));

        final ApiKeyAuthEntryPoint entryPoint = new ApiKeyAuthEntryPoint();
        httpSecurity.cors().and().csrf().disable()
                .authorizeRequests()
                .antMatchers("/").permitAll()
                .antMatchers("/api-docs/**").permitAll()
                .antMatchers("/swagger-ui/**").permitAll()
                .anyRequest().authenticated().and()
                .addFilter(filter)
                .exceptionHandling().authenticationEntryPoint(entryPoint).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
}
