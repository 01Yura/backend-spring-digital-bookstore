package online.ityura.springdigitallibrary.testinfra.specs;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import online.ityura.springdigitallibrary.dto.request.LoginRequest;
import online.ityura.springdigitallibrary.testinfra.configs.Config;
import online.ityura.springdigitallibrary.testinfra.helpers.CustomLoggingFilter;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class RequestSpecs {
    private static Map<String, String> accessTokens = new HashMap<>();
    private static Map<String, String> refreshTokens = new HashMap<>();

    private RequestSpecs() {
    }

    private static RequestSpecBuilder defaultRequestSpecBuilder() {
        return new RequestSpecBuilder()
                .setBaseUri(Config.getProperty("apiBaseUrl") + Config.getProperty("apiVersion"))
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new CustomLoggingFilter());
    }

    public static RequestSpecification unauthSpec() {
        return defaultRequestSpecBuilder().build();
    }

    public static RequestSpecification adminSpec() {
        return userSpec(Config.getProperty("admin.email"), Config.getProperty(
                        "admin.password"));
    }

    public static RequestSpecification userSpec(String email, String password) {
        return defaultRequestSpecBuilder()
                .setAuth(RestAssured.oauth2(getUserAccessToken(email, password)))
                .build();
    }


    public static String getUserAccessToken(String email, String password) {
        String accessToken;

        if (!accessTokens.containsKey(email)) {
            accessToken =
                    given()
                            .spec(RequestSpecs.unauthSpec())
                            .body(LoginRequest.builder().email(email).password(password).build())
                            .when()
                            .post("auth/login")
                            .then()
                            .statusCode(200)
                            .extract()
                            .path("accessToken");

            accessTokens.put(email, accessToken);
        } else {
            accessToken = accessTokens.get(email);
        }

        return accessToken;
    }
}
