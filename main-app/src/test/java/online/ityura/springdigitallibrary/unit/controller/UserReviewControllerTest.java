package online.ityura.springdigitallibrary.unit.controller;

import online.ityura.springdigitallibrary.controller.UserReviewController;
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
class UserReviewControllerTest {
    
    @Mock
    private ReviewService reviewService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private UserDetails userDetails;
    
    @InjectMocks
    private UserReviewController userReviewController;
    
    private User testUser;
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
        
        reviewResponse = ReviewResponse.builder()
                .id(1L)
                .bookId(1L)
                .text("Great book!")
                .build();
        
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }
    
    @Test
    void testGetMyReviews_Success_ShouldReturn200() {
        // Given
        Page<ReviewResponse> page = new PageImpl<>(
                List.of(reviewResponse),
                PageRequest.of(0, 20),
                1
        );
        
        when(reviewService.getMyReviews(anyLong(), any())).thenReturn(page);
        
        // When
        ResponseEntity<Page<ReviewResponse>> response = userReviewController.getMyReviews(PageRequest.of(0, 20), authentication);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(1, response.getBody().getContent().size());
    }
    
    @Test
    void testGetMyReviews_EmptyList_ShouldReturn200() {
        // Given
        Page<ReviewResponse> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 20),
                0
        );
        
        when(reviewService.getMyReviews(anyLong(), any())).thenReturn(emptyPage);
        
        // When
        ResponseEntity<Page<ReviewResponse>> response = userReviewController.getMyReviews(PageRequest.of(0, 20), authentication);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getTotalElements());
        assertTrue(response.getBody().getContent().isEmpty());
    }
}

