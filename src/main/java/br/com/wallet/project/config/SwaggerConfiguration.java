package br.com.wallet.project.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

  private static final String DESCRIPTION =
      "Wallet system.";

  @Bean
  public OpenAPI springShopOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Wallet 1.0.0")
                .description(DESCRIPTION)
                .version("1.0.0"));
  }
}
