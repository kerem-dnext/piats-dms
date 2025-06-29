package com.piats.dms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Document Management System API")
                        .description("REST API for managing CV and document uploads in the ATS system")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PIATS Team")
                                .url("https://github.com/piats")
                        )
                );
    }
} 