package com.piats.dms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI documentation for the service.
 * <p>
 * This class provides a bean that defines the high-level information for the
 * API, such as title, description, version, and contact details, which is
 * used by Swagger to generate the API documentation.
 * </p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates the {@link OpenAPI} bean with customized API metadata.
     *
     * @return A configured {@code OpenAPI} instance.
     */
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