package com.kamran.template;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Template Spring Boot application.
 * Configures component scanning, auto-configuration, and OpenAPI documentation.
 *
 * <p>The application provides REST APIs for user management with full CRUD capabilities.
 * OpenAPI documentation is available at /swagger-ui.html when the application is running.
 *
 * @author Kamran
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "Template API",
                version = "1.0",
                description = "REST API for the Template application providing some default user management capabilities",
                contact = @Contact(
                        name = "Kamran",
                        email = "kamran.rouhani@outlook.com"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                )
        )
)
public class TemplateApplication {

    /**
     * Main method that starts the Spring Boot application.
     *
     * @param args Command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(TemplateApplication.class, args);
    }
}
