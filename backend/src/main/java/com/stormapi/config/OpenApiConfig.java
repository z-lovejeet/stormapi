package com.stormapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration for auto-generated API documentation.
 * Access at: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stormApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("StormAPI")
                        .description("API Performance Testing Platform — REST API")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("StormAPI")
                                .url("https://github.com/lovejeetsingh1/stormapi"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }

}
