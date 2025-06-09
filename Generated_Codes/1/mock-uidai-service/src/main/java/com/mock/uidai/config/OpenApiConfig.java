package com.mock.uidai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;

/**
* Configuration class for OpenAPI/Swagger documentation.
* This class sets up the OpenAPI documentation for the Mock UIDAI Service API.
*/
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Mock UIDAI Service}")
    private String applicationName;

    @Value("${spring.application.description:A mock service for UIDAI (Aadhaar) verification and authentication}")
    private String applicationDescription;

    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
    * Creates and configures the OpenAPI documentation bean.
    *
    * @return Configured OpenAPI instance
    */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
        .info(createApiInfo())
        .servers(Collections.singletonList(createServer()))
        .components(createSecurityComponents())
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    /**
    * Creates the API information section for the OpenAPI documentation.
    *
    * @return Info object containing API details
    */
    private Info createApiInfo() {
        return new Info()
        .title(applicationName)
        .description(applicationDescription)
        .version(applicationVersion)
        .contact(new Contact()
        .name("Mock UIDAI Service Team")
        .email("support@mockuidai.com")
        .url("https://mockuidai.com"))
        .license(new License()
        .name("Apache 2.0")
        .url("https://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    /**
    * Creates the server information for the OpenAPI documentation.
    *
    * @return Server object with URL and description
    */
    private Server createServer() {
        Server server = new Server();
        server.setUrl(contextPath);
        server.setDescription("Mock UIDAI Service API");
        return server;
    }

    /**
    * Creates security components for the OpenAPI documentation.
    * Configures Bearer token authentication scheme.
    *
    * @return Components object with security schemes
    */
    private Components createSecurityComponents() {
        return new Components()
        .addSecuritySchemes("bearerAuth",
        new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
        .description("JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\""));
    }
}