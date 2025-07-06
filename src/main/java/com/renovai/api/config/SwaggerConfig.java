package com.renovai.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI renovaiOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("Renovaí API")
                .description("""
                    API REST da plataforma **Renovaí** — Sistema de Gestão para Cooperativas de Reciclagem.
                    
                    ## Perfis de Acesso
                    - **ADMIN_SITE** — Acesso total ao sistema
                    - **ADMIN_COOPERATIVA** — Gestão institucional da cooperativa
                    - **GESTOR_COOPERATIVA** — Operações, estoque, pedidos e rateio
                    - **FUNCIONARIO_COOPERATIVA** — Registro de coletas e triagens
                    - **GESTOR_EMPRESA** — Busca de materiais e envio de pedidos
                    
                    ## Autenticação
                    Use o endpoint `/auth/login` para obter um token JWT e clique em **Authorize** para usá-lo.
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Equipe Renovaí")
                    .email("dev@renovai.com.br")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                    .name(securitySchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Insira o token JWT obtido via /auth/login")));
    }
}
