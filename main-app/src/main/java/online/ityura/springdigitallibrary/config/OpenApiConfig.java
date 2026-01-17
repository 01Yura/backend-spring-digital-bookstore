package online.ityura.springdigitallibrary.config;

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

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Digital Bookstore API")
                        .version("1.0.0")
                        .description("REST API для цифрового книжного магазина.<br>" +
                                "Поддерживает управление книгами, отзывами, рейтингами, пользователями, покупку и скачивание PDF книг.<br><br>" +

                                "Данный проект был создан исключительно в целях самообучения при изучении Spring Boot в далеком 2023 году.<br>" +
                                "Может содержать баги. Активно не поддерживается, однако работает.<br>" +
                                "Может использоваться QA для тренировки тестирования бэкенда в связке с БД.<br><br>" +

                                "Для логина админом используйте: admin@gmail.com , admin<br><br>")
                        .contact(new Contact()
                                .name("Developer contact")
                                .url("https://github.com/01Yura"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }
}

