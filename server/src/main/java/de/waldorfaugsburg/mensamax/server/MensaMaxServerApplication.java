package de.waldorfaugsburg.mensamax.server;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Collections;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class, JacksonAutoConfiguration.class})
@EnableConfigurationProperties({MensaMaxConfigurationProperties.class})
public class MensaMaxServerApplication {

    public MensaMaxServerApplication() {

    }

    public static void main(final String[] args) {
        SpringApplication.run(MensaMaxServerApplication.class, args);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("MensaMax")
                .version(getClass().getPackage().getImplementationVersion())
                .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
                .servers(Collections.singletonList(new Server().url("https://mensamax.waldorf-augsburg.de/")));
    }
}
