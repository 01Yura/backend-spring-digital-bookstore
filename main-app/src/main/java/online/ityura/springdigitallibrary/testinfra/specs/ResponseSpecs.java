package online.ityura.springdigitallibrary.testinfra.specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

public class ResponseSpecs {
    private ResponseSpecs() {
    }

    private static ResponseSpecBuilder defaultResponseSpecBuilder() {
        return new ResponseSpecBuilder();
    }

    public static ResponseSpecification statusCode(int code) {
        return defaultResponseSpecBuilder()
                .expectStatusCode(code)
                .build();
    }

    public static ResponseSpecification statusCodeAndSchema(int code, String schemaClasspath) {
        return defaultResponseSpecBuilder()
                .expectStatusCode(code)
                .expectBody(matchesJsonSchemaInClasspath(schemaClasspath))
                .build();
    }
}
