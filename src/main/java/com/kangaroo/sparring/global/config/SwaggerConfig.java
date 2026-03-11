package com.kangaroo.sparring.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public Info apiInfo() {
        return new Info()
                .title("Sparring API")
                .description("Sparring API 명세서")
                .version("1.0.0");
    }

    @Bean
    public OpenAPI openAPI() {
        // JWT 보안 설정
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("JWT");

        Components components = new Components()
                .addSecuritySchemes("JWT", new SecurityScheme()
                        .name("JWT")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .description("JWT 토큰을 입력해주세요 (Bearer 제외)"));

        // 서버 설정
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("로컬 서버");

        return new OpenAPI()
                .servers(List.of(localServer))
                .components(components)
                .info(apiInfo())
                .addSecurityItem(securityRequirement);
    }
}