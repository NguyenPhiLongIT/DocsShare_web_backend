package com.docsshare_web_backend.commons.configs;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import com.docsshare_web_backend.commons.utils.JwtUtils;

@Configuration
@SecurityScheme(
        name = "cookieAuth",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = JwtUtils.TOKEN_NAME
)
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DocsShare Web Backend API")
                        .version("1.0")
                        .description("API documentation for DocsShare Web Backend"))
                .servers(List.of(
                        new Server().url("https://925520bd8f2f.ngrok-free.app"),
                        new Server().url("http://localhost:8080")
                ));
    }
}
