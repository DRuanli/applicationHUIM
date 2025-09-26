// File: src/main/java/com/mining/api/config/SwaggerConfig.java
package com.mining.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("PTK-HUIM-U Mining API")
                .version("2.0.0")
                .description("REST API for High-Utility Itemset Mining with Uncertain Databases")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
            );
    }
}