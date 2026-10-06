package com.nextworks.unextwebservices.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI unextOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Unext WebServices API")
                        .description("API REST para la plataforma interuniversitaria y laboral Unext. " +
                                "Permite la gestión de perfiles (Postulantes, Reclutadores, Instituciones), " +
                                "ofertas de empleo, postulaciones, validaciones académicas, mensajería y notificaciones.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("NextWorks Team")
                                .email("soporte@unext.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingrese el token JWT obtenido en /api/auth/login")));
    }
}
