package com.aicc.silverlink.global.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {

        // JWT Security Scheme 설정
        SecurityScheme securityScheme = new SecurityScheme()
                .name("Authorization")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("JWT");



        String description = """
                ### 회원가입 더미 데이터 예시

                ```json
                {
                  "진님 파이팅!! " 입니다.
                }
                ```
                """;

        return new OpenAPI()
                .info(new Info()
                        .title("SilverLink API")
                        .description(description)
                        .version("v1.0"))
                .components(new Components()
                        .addSecuritySchemes("JWT", securityScheme))
                .addSecurityItem(securityRequirement);
    }
}
