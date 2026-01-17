package online.ityura.springdigitallibrary.testinfra.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

public class CustomLoggingFilter implements Filter {

    private final ObjectWriter prettyPrinter = new ObjectMapper().writerWithDefaultPrettyPrinter();

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {

        System.out.println("---------- REQUEST >>>>>>>>>>");
        System.out.println(requestSpec.getMethod() + " " + requestSpec.getURI());
        System.out.println();

        System.out.println("Request Headers:");
        requestSpec.getHeaders().forEach(header ->
                System.out.println(header.getName() + ": " + header.getValue())
        );

        if (requestSpec.getBody() != null) {
            System.out.println();
            System.out.println("Body:");
            try {
                String prettyBody = prettyPrinter.writeValueAsString(
                        new ObjectMapper().readTree(requestSpec.getBody().toString()));
                System.out.println(prettyBody);
            } catch (Exception e) {
                System.out.println("Raw body (could not format as JSON):\n" + requestSpec.getBody());
            }
        }


        Response response = ctx.next(requestSpec, responseSpec);

        System.out.println();
        System.out.println("<<<<<<<<<< RESPONSE ----------");
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println();

        System.out.println("Response Headers:");
        Headers responseHeaders = response.getHeaders();
        responseHeaders.forEach(header ->
                System.out.println(header.getName() + ": " + header.getValue())
        );
        System.out.println();
        System.out.println("Body:");
        System.out.println(response.getBody().asPrettyString());
        System.out.println("------------------------------");
        System.out.println();
        System.out.println();

        return response;
    }
}
