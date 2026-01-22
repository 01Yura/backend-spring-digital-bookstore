package online.ityura.springdigitallibrary.api;

import online.ityura.springdigitallibrary.dto.response.ErrorResponse;
import online.ityura.springdigitallibrary.dto.response.UserInfoResponse;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.testinfra.comparators.UniversalComparator;
import online.ityura.springdigitallibrary.testinfra.database.DataBaseSteps;
import online.ityura.springdigitallibrary.testinfra.requests.clients.CrudRequester;
import online.ityura.springdigitallibrary.testinfra.requests.clients.Endpoint;
import online.ityura.springdigitallibrary.testinfra.requests.steps.UserSteps;
import online.ityura.springdigitallibrary.testinfra.specs.RequestSpecs;
import online.ityura.springdigitallibrary.testinfra.specs.ResponseSpecs;
import org.junit.jupiter.api.Test;

public class UserInfoTest extends BaseApiTest {

    @Test
    void userCanGetOwnInfoWithValidToken() {
        // Arrange: Register and verify user
        UserSteps.RegisteredUser registeredUser = UserSteps.registerAndVerifyRandomUser();
        
        // Act: Get user info with access token
        UserInfoResponse userInfo = new CrudRequester(
                RequestSpecs.userSpec(registeredUser.getRegisterRequest().getEmail(), registeredUser.getRegisterRequest().getPassword()),
                ResponseSpecs.statusCode(200),
                Endpoint.USERS_ME)
                .get()
                .extract().as(UserInfoResponse.class);
        
        // Assert: Verify user info
        softly.assertThat(userInfo).isNotNull();
        softly.assertThat(userInfo.getId()).isEqualTo(registeredUser.getRegisterResponse().getUserId());
        softly.assertThat(userInfo.getRole()).isEqualTo("USER");
        
        // Compare UserInfoResponse with RegisterRequest using UniversalComparator
        UniversalComparator.match(userInfo, registeredUser.getRegisterRequest());
        
        // Verify user in database
        User user = DataBaseSteps.getUserByEmail(registeredUser.getRegisterRequest().getEmail());
        softly.assertThat(user).isNotNull();
        UniversalComparator.match(user, registeredUser.getRegisterRequest());
    }

    @Test
    void userCannotGetInfoWithoutToken() {
        // Arrange: No authentication
        
        // Act: Try to get user info without token
        ErrorResponse errorResponse = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(401),
                Endpoint.USERS_ME)
                .get()
                .extract().as(ErrorResponse.class);
        
        // Assert: Verify error response
        softly.assertThat(errorResponse).isNotNull();
        softly.assertThat(errorResponse.getStatus()).isEqualTo(401);
        softly.assertThat(errorResponse.getError()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void userCannotGetInfoWithInvalidToken() {
        // Arrange: Invalid token
        String invalidToken = "invalid.token.here";
        
        // Act: Try to get user info with invalid token
        ErrorResponse errorResponse = new CrudRequester(
                RequestSpecs.unauthSpec().header("Authorization", "Bearer " + invalidToken),
                ResponseSpecs.statusCode(401),
                Endpoint.USERS_ME)
                .get()
                .extract().as(ErrorResponse.class);
        
        // Assert: Verify error response
        softly.assertThat(errorResponse).isNotNull();
        softly.assertThat(errorResponse.getStatus()).isEqualTo(401);
        softly.assertThat(errorResponse.getError()).isEqualTo("UNAUTHORIZED");
    }
}
