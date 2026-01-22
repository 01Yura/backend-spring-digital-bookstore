package online.ityura.springdigitallibrary.api;

import io.restassured.common.mapper.TypeRef;
import online.ityura.springdigitallibrary.dto.request.CreateRatingRequest;
import online.ityura.springdigitallibrary.dto.response.BookResponse;
import online.ityura.springdigitallibrary.dto.response.ErrorResponse;
import online.ityura.springdigitallibrary.dto.response.RatingResponse;
import online.ityura.springdigitallibrary.model.Rating;
import online.ityura.springdigitallibrary.testinfra.comparators.UniversalComparator;
import online.ityura.springdigitallibrary.testinfra.database.DataBaseSteps;
import online.ityura.springdigitallibrary.testinfra.requests.clients.CrudRequester;
import online.ityura.springdigitallibrary.testinfra.requests.clients.Endpoint;
import online.ityura.springdigitallibrary.testinfra.requests.steps.UserSteps;
import online.ityura.springdigitallibrary.testinfra.specs.RequestSpecs;
import online.ityura.springdigitallibrary.testinfra.specs.ResponseSpecs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;


public class RatingCreationTest extends BaseApiTest {

    @Test
    void userCanCreateRatingForBook() {
        // Arrange: Register and verify user, get a book
        UserSteps.RegisteredUser registeredUser = UserSteps.registerAndVerifyRandomUser();
        
        // Get first available book via API
        Page<BookResponse> booksPage = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(200),
                Endpoint.BOOKS)
                .get()
                .extract()
                .as(new TypeRef<Page<BookResponse>>() {});
        
        softly.assertThat(booksPage.getContent()).isNotEmpty();
        Long bookId = booksPage.getContent().get(0).getId();
        
        // Create rating request
        CreateRatingRequest ratingRequest = new CreateRatingRequest();
        ratingRequest.setValue((short) 8);
        
        // Act: Create rating
        RatingResponse ratingResponse = new CrudRequester(
                RequestSpecs.userSpec(registeredUser.getRegisterRequest().getEmail(), registeredUser.getRegisterRequest().getPassword()),
                ResponseSpecs.statusCode(201),
                Endpoint.BOOKS_RATINGS)
                .post(ratingRequest, bookId)
                .extract().as(RatingResponse.class);
        
        // Assert: Verify rating response
        softly.assertThat(ratingResponse).isNotNull();
        softly.assertThat(ratingResponse.getId()).isNotNull();
        softly.assertThat(ratingResponse.getBookId()).isEqualTo(bookId);
        softly.assertThat(ratingResponse.getUserId()).isEqualTo(registeredUser.getRegisterResponse().getUserId());
        softly.assertThat(ratingResponse.getCreatedAt()).isNotNull();
        
        // Compare CreateRatingRequest with RatingResponse using UniversalComparator
        UniversalComparator.match(ratingRequest, ratingResponse);
        
        // Verify rating in database
        Rating rating = DataBaseSteps.getRatingById(ratingResponse.getId());
        softly.assertThat(rating).isNotNull();
        softly.assertThat(rating.getValue()).isEqualTo((short) 8);
        softly.assertThat(rating.getBook().getId()).isEqualTo(bookId);
        softly.assertThat(rating.getUser().getId()).isEqualTo(registeredUser.getRegisterResponse().getUserId());
    }

    @ParameterizedTest
    @ValueSource(shorts = {1, 5, 10})
    void userCanCreateRatingWithValidValues(short ratingValue) {
        // Arrange: Register and verify user, get a book
        UserSteps.RegisteredUser registeredUser = UserSteps.registerAndVerifyRandomUser();
        
        // Get first available book via API
        Page<BookResponse> booksPage = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(200),
                Endpoint.BOOKS)
                .get()
                .extract()
                .as(new TypeRef<Page<BookResponse>>() {});
        
        softly.assertThat(booksPage.getContent()).isNotEmpty();
        Long bookId = booksPage.getContent().get(0).getId();
        
        CreateRatingRequest ratingRequest = new CreateRatingRequest();
        ratingRequest.setValue(ratingValue);
        
        // Act: Create rating
        RatingResponse ratingResponse = new CrudRequester(
                RequestSpecs.userSpec(registeredUser.getRegisterRequest().getEmail(), registeredUser.getRegisterRequest().getPassword()),
                ResponseSpecs.statusCode(201),
                Endpoint.BOOKS_RATINGS)
                .post(ratingRequest, bookId)
                .extract().as(RatingResponse.class);
        
        // Assert: Verify rating value using UniversalComparator
        UniversalComparator.match(ratingRequest, ratingResponse);
    }

    @Test
    void userCannotCreateRatingWithoutAuthentication() {
        // Arrange: Get a book via API, no authentication
        Page<BookResponse> booksPage = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(200),
                Endpoint.BOOKS)
                .get()
                .extract()
                .as(new TypeRef<Page<BookResponse>>() {});
        
        softly.assertThat(booksPage.getContent()).isNotEmpty();
        Long bookId = booksPage.getContent().get(0).getId();
        
        CreateRatingRequest ratingRequest = new CreateRatingRequest();
        ratingRequest.setValue((short) 8);
        
        // Act: Try to create rating without authentication
        ErrorResponse errorResponse = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(401),
                Endpoint.BOOKS_RATINGS)
                .post(ratingRequest, bookId)
                .extract().as(ErrorResponse.class);
        
        // Assert: Verify error response
        softly.assertThat(errorResponse).isNotNull();
        softly.assertThat(errorResponse.getStatus()).isEqualTo(401);
        softly.assertThat(errorResponse.getError()).isEqualTo("UNAUTHORIZED");
    }

    @ParameterizedTest
    @ValueSource(shorts = {0, 11, -1})
    void userCannotCreateRatingWithInvalidValues(short invalidValue) {
        // Arrange: Register and verify user, get a book
        UserSteps.RegisteredUser registeredUser = UserSteps.registerAndVerifyRandomUser();
        
        // Get first available book via API
        Page<BookResponse> booksPage = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(200),
                Endpoint.BOOKS)
                .get()
                .extract()
                .as(new TypeRef<Page<BookResponse>>() {});
        
        softly.assertThat(booksPage.getContent()).isNotEmpty();
        Long bookId = booksPage.getContent().get(0).getId();
        
        CreateRatingRequest ratingRequest = new CreateRatingRequest();
        ratingRequest.setValue(invalidValue);
        
        // Act: Try to create rating with invalid value
        ErrorResponse errorResponse = new CrudRequester(
                RequestSpecs.userSpec(registeredUser.getRegisterRequest().getEmail(), registeredUser.getRegisterRequest().getPassword()),
                ResponseSpecs.statusCode(400),
                Endpoint.BOOKS_RATINGS)
                .post(ratingRequest, bookId)
                .extract().as(ErrorResponse.class);
        
        // Assert: Verify validation error
        softly.assertThat(errorResponse).isNotNull();
        softly.assertThat(errorResponse.getStatus()).isEqualTo(400);
        softly.assertThat(errorResponse.getError()).isEqualTo("VALIDATION_ERROR");
        softly.assertThat(errorResponse.getFieldErrors()).containsKey("value");
    }

    @Test
    void userCannotCreateRatingForNonExistentBook() {
        // Arrange: Register and verify user
        UserSteps.RegisteredUser registeredUser = UserSteps.registerAndVerifyRandomUser();
        
        CreateRatingRequest ratingRequest = new CreateRatingRequest();
        ratingRequest.setValue((short) 8);
        Long nonExistentBookId = 999999L;
        
        // Act: Try to create rating for non-existent book
        ErrorResponse errorResponse = new CrudRequester(
                RequestSpecs.userSpec(registeredUser.getRegisterRequest().getEmail(), registeredUser.getRegisterRequest().getPassword()),
                ResponseSpecs.statusCode(404),
                Endpoint.BOOKS_RATINGS)
                .post(ratingRequest, nonExistentBookId)
                .extract().as(ErrorResponse.class);
        
        // Assert: Verify error response
        softly.assertThat(errorResponse).isNotNull();
        softly.assertThat(errorResponse.getStatus()).isEqualTo(404);
        softly.assertThat(errorResponse.getError()).isEqualTo("BOOK_NOT_FOUND");
    }
}
