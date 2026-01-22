package online.ityura.springdigitallibrary.api;

import online.ityura.springdigitallibrary.dto.request.CreateReviewRequest;
import online.ityura.springdigitallibrary.dto.response.ErrorResponse;
import online.ityura.springdigitallibrary.dto.response.ReviewResponse;
import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.model.Review;
import online.ityura.springdigitallibrary.testinfra.comparators.UniversalComparator;
import online.ityura.springdigitallibrary.testinfra.database.DataBaseSteps;
import online.ityura.springdigitallibrary.testinfra.generators.RandomDtoGeneratorWithFaker;
import online.ityura.springdigitallibrary.testinfra.requests.clients.CrudRequester;
import online.ityura.springdigitallibrary.testinfra.requests.clients.Endpoint;
import online.ityura.springdigitallibrary.testinfra.requests.steps.UserSteps;
import online.ityura.springdigitallibrary.testinfra.specs.RequestSpecs;
import online.ityura.springdigitallibrary.testinfra.specs.ResponseSpecs;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ReviewCreationTest extends BaseApiTest {

    @Test
    void userCanCreateReviewForBook() {
        // Arrange: Register and verify user, get a book
        UserSteps.RegisteredUser registeredUser = UserSteps.registerAndVerifyRandomUser();
        
        // Get first available book via API
        // Note: This test still uses DB for simplicity, but could be refactored to use API
        List<Book> books = DataBaseSteps.getAllBooks();
        softly.assertThat(books).isNotEmpty();
        Long bookId = books.get(0).getId();
        
        // Create review request
        CreateReviewRequest reviewRequest = RandomDtoGeneratorWithFaker.generateRandomDtoObject(CreateReviewRequest.class);
        
        // Act: Create review
        ReviewResponse reviewResponse = new CrudRequester(
                RequestSpecs.userSpec(registeredUser.getRegisterRequest().getEmail(), registeredUser.getRegisterRequest().getPassword()),
                ResponseSpecs.statusCode(201),
                Endpoint.BOOKS_REVIEWS)
                .post(reviewRequest, bookId)
                .extract().as(ReviewResponse.class);
        
        // Assert: Verify review response
        softly.assertThat(reviewResponse).isNotNull();
        softly.assertThat(reviewResponse.getId()).isNotNull();
        softly.assertThat(reviewResponse.getBookId()).isEqualTo(bookId);
        softly.assertThat(reviewResponse.getUser()).isNotNull();
        softly.assertThat(reviewResponse.getUser().getEmail()).isEqualTo(registeredUser.getRegisterRequest().getEmail());
        softly.assertThat(reviewResponse.getCreatedAt()).isNotNull();
        
        // Compare CreateReviewRequest with ReviewResponse using UniversalComparator
        UniversalComparator.match(reviewRequest, reviewResponse);
        
        // Verify review in database
        Review review = DataBaseSteps.getReviewById(reviewResponse.getId());
        softly.assertThat(review).isNotNull();
        softly.assertThat(review.getText()).isEqualTo(reviewRequest.getText());
        softly.assertThat(review.getBook().getId()).isEqualTo(bookId);
        softly.assertThat(review.getUser().getId()).isEqualTo(registeredUser.getRegisterResponse().getUserId());
    }

    @Test
    void userCannotCreateReviewWithoutAuthentication() {
        // Arrange: Get a book, no authentication
        List<Book> books = DataBaseSteps.getAllBooks();
        softly.assertThat(books).isNotEmpty();
        Long bookId = books.get(0).getId();
        
        CreateReviewRequest reviewRequest = RandomDtoGeneratorWithFaker.generateRandomDtoObject(CreateReviewRequest.class);
        
        // Act: Try to create review without authentication
        ErrorResponse errorResponse = new CrudRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.statusCode(401),
                Endpoint.BOOKS_REVIEWS)
                .post(reviewRequest, bookId)
                .extract().as(ErrorResponse.class);
        
        // Assert: Verify error response
        softly.assertThat(errorResponse).isNotNull();
        softly.assertThat(errorResponse.getStatus()).isEqualTo(401);
        softly.assertThat(errorResponse.getError()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void userCannotCreateReviewWithEmptyText() {
        // Arrange: Register and verify user, get a book
        UserSteps.RegisteredUser registeredUser = UserSteps.registerAndVerifyRandomUser();
        
        List<Book> books = DataBaseSteps.getAllBooks();
        softly.assertThat(books).isNotEmpty();
        Long bookId = books.get(0).getId();
        
        CreateReviewRequest reviewRequest = new CreateReviewRequest();
        reviewRequest.setText("");
        
        // Act: Try to create review with empty text
        ErrorResponse errorResponse = new CrudRequester(
                RequestSpecs.userSpec(registeredUser.getRegisterRequest().getEmail(), registeredUser.getRegisterRequest().getPassword()),
                ResponseSpecs.statusCode(400),
                Endpoint.BOOKS_REVIEWS)
                .post(reviewRequest, bookId)
                .extract().as(ErrorResponse.class);
        
        // Assert: Verify validation error
        softly.assertThat(errorResponse).isNotNull();
        softly.assertThat(errorResponse.getStatus()).isEqualTo(400);
        softly.assertThat(errorResponse.getError()).isEqualTo("VALIDATION_ERROR");
        softly.assertThat(errorResponse.getFieldErrors()).containsKey("text");
    }

    @Test
    void userCannotCreateReviewForNonExistentBook() {
        // Arrange: Register and verify user
        UserSteps.RegisteredUser registeredUser = UserSteps.registerAndVerifyRandomUser();
        
        CreateReviewRequest reviewRequest = RandomDtoGeneratorWithFaker.generateRandomDtoObject(CreateReviewRequest.class);
        Long nonExistentBookId = 999999L;
        
        // Act: Try to create review for non-existent book
        ErrorResponse errorResponse = new CrudRequester(
                RequestSpecs.userSpec(registeredUser.getRegisterRequest().getEmail(), registeredUser.getRegisterRequest().getPassword()),
                ResponseSpecs.statusCode(404),
                Endpoint.BOOKS_REVIEWS)
                .post(reviewRequest, nonExistentBookId)
                .extract().as(ErrorResponse.class);
        
        // Assert: Verify error response
        softly.assertThat(errorResponse).isNotNull();
        softly.assertThat(errorResponse.getStatus()).isEqualTo(404);
        softly.assertThat(errorResponse.getError()).isEqualTo("BOOK_NOT_FOUND");
    }
}
