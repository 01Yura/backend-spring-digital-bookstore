package online.ityura.springdigitallibrary.unit.controller;

import online.ityura.springdigitallibrary.controller.ReviewController;
import online.ityura.springdigitallibrary.dto.request.CreateReviewRequest;
import online.ityura.springdigitallibrary.dto.request.UpdateReviewRequest;
import online.ityura.springdigitallibrary.dto.response.ReviewResponse;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {
    
    @Mock
    private ReviewService reviewService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private UserDetails userDetails;
    
    @InjectMocks
    private ReviewController reviewController;
    
    private User testUser;
    private CreateReviewRequest createRequest;
    private UpdateReviewRequest updateRequest;
    private ReviewResponse reviewResponse;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("testuser")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .build();
        
        createRequest = new CreateReviewRequest();
        createRequest.setText("Great book!");
        
        updateRequest = new UpdateReviewRequest();
        updateRequest.setText("Updated review text");
        
        reviewResponse = ReviewResponse.builder()
                .id(1L)
                .bookId(1L)
                .text("Great book!")
                .build();
    }
    
    private void setupAuthentication() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }
    
    @Test
    void testCreateReview_Success_ShouldReturn201() {
        // Given
        setupAuthentication();
        when(reviewService.createReview(anyLong(), anyLong(), any(CreateReviewRequest.class)))
                .thenReturn(reviewResponse);
        
        // When
        ResponseEntity<ReviewResponse> response = reviewController.createReview(1L, createRequest, authentication);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("Great book!", response.getBody().getText());
    }
    
    @Test
    void testUpdateMyReview_Success_ShouldReturn200() {
        // Given
        setupAuthentication();
        ReviewResponse updatedResponse = ReviewResponse.builder()
                .id(1L)
                .bookId(1L)
                .text("Updated review text")
                .build();
        
        when(reviewService.updateReview(anyLong(), anyLong(), any(UpdateReviewRequest.class)))
                .thenReturn(updatedResponse);
        
        // When
        ResponseEntity<ReviewResponse> response = reviewController.updateMyReview(1L, updateRequest, authentication);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated review text", response.getBody().getText());
    }
    
    @Test
    void testGetReviews_Success_ShouldReturn200() {
        // Given
        Page<ReviewResponse> page = new PageImpl<>(
                List.of(reviewResponse),
                PageRequest.of(0, 20),
                1
        );
        
        when(reviewService.getReviewsByBookId(anyLong(), any())).thenReturn(page);
        
        // When
        ResponseEntity<Page<ReviewResponse>> response = reviewController.getReviews(1L, PageRequest.of(0, 20));
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
    }
    
    @Test
    void testGetReviews_EmptyList_ShouldReturn200() {
        // Given
        Page<ReviewResponse> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 20),
                0
        );
        
        when(reviewService.getReviewsByBookId(anyLong(), any())).thenReturn(emptyPage);
        
        // When
        ResponseEntity<Page<ReviewResponse>> response = reviewController.getReviews(1L, PageRequest.of(0, 20));
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getTotalElements());
    }
}

