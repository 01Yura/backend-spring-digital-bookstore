package online.ityura.springdigitallibrary.api;

import online.ityura.springdigitallibrary.dto.request.LoginRequest;
import online.ityura.springdigitallibrary.dto.response.ErrorResponse;
import online.ityura.springdigitallibrary.dto.response.LoginResponse;
import online.ityura.springdigitallibrary.testinfra.comparators.UniversalComparator;
import online.ityura.springdigitallibrary.testinfra.database.DataBaseSteps;
import online.ityura.springdigitallibrary.testinfra.requests.clients.CrudRequester;
import online.ityura.springdigitallibrary.testinfra.requests.clients.Endpoint;
import online.ityura.springdigitallibrary.testinfra.requests.steps.UserSteps;
import online.ityura.springdigitallibrary.testinfra.specs.RequestSpecs;
import online.ityura.springdigitallibrary.testinfra.specs.ResponseSpecs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class UserLoginTest extends BaseApiTest {

    @Test
    void userCanLoginWithValidCredentials() {
        // Arrange: Register and verify a new user
        UserSteps.RegisteredUser registeredUser = UserSteps.registerAndVerifyRandomUser();
        
        // Verify user is registered
        UniversalComparator.match(registeredUser.getRegisterResponse(), registeredUser.getRegisterRequest());
        
        // Verify user in database
        softly.assertThat(DataBaseSteps.userExists(registeredUser.getRegisterRequest().getEmail())).isTrue();
        
        // Act: Login with valid credentials
        LoginRequest loginRequest = LoginRequest.builder()
                .email(registeredUser.getRegisterRequest().getEmail())
                .password(registeredUser.getRegisterRequest().getPassword())
                .build();
        
        LoginResponse loginResponse = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(200),
                Endpoint.AUTH_LOGIN)
                .post(loginRequest)
                .extract().as(LoginResponse.class);
        
        // Assert: Verify login response
        softly.assertThat(loginResponse).isNotNull();
        softly.assertThat(loginResponse.getAccessToken()).isNotNull().isNotEmpty();
        softly.assertThat(loginResponse.getRefreshToken()).isNotNull().isNotEmpty();
        softly.assertThat(loginResponse.getTokenType()).isEqualTo("Bearer");
    }

    @ParameterizedTest
    @MethodSource("argFor_userCannotLoginWithInvalidCredentials")
    void userCannotLoginWithInvalidCredentials(String emailModifier, String passwordModifier, int expectedStatusCode, 
                                                String expectedError, String expectedFieldName) {
        // Arrange: Create a new user for this test case
        UserSteps.RegisteredUser registeredUser = UserSteps.registerAndVerifyRandomUser();
        
        // Apply modifiers using concatenation: "" keeps valid, "X" makes invalid, "EMPTY" makes empty
        String email;
        if ("EMPTY".equals(emailModifier)) {
            email = "";
        } else {
            email = registeredUser.getRegisterRequest().getEmail() + emailModifier;
        }
        
        String password;
        if ("EMPTY".equals(passwordModifier)) {
            password = "";
        } else {
            password = registeredUser.getRegisterRequest().getPassword() + passwordModifier;
        }
        
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
        
        // Act: Attempt to login
        ErrorResponse errorResponse = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(expectedStatusCode),
                Endpoint.AUTH_LOGIN)
                .post(loginRequest)
                .extract().as(ErrorResponse.class);
        
        // Assert: Verify error response
        softly.assertThat(errorResponse).isNotNull();
        softly.assertThat(errorResponse.getStatus()).isEqualTo(expectedStatusCode);
        softly.assertThat(errorResponse.getError()).isEqualTo(expectedError);
        
        // For validation errors, check field errors
        if (expectedFieldName != null) {
            softly.assertThat(errorResponse.getFieldErrors()).containsKey(expectedFieldName);
        }
    }

    static Stream<Arguments> argFor_userCannotLoginWithInvalidCredentials() {
        return Stream.of(
                // Invalid email (non-existent), valid password -> 401 UNAUTHORIZED
                Arguments.of("X", "", 401, "UNAUTHORIZED", null),
                // Valid email, invalid password -> 401 UNAUTHORIZED
                Arguments.of("", "X", 401, "UNAUTHORIZED", null),
                // Empty email, valid password -> 400 VALIDATION_ERROR
                Arguments.of("EMPTY", "", 400, "VALIDATION_ERROR", "email"),
                // Valid email, empty password -> 400 VALIDATION_ERROR
                Arguments.of("", "EMPTY", 400, "VALIDATION_ERROR", "password")
        );
    }
}
