package online.ityura.springdigitallibrary.unit.controller;

import online.ityura.springdigitallibrary.controller.RatingController;
import online.ityura.springdigitallibrary.dto.request.CreateRatingRequest;
import online.ityura.springdigitallibrary.dto.request.UpdateRatingRequest;
import online.ityura.springdigitallibrary.dto.response.RatingResponse;
import online.ityura.springdigitallibrary.model.Rating;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.RatingRepository;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.service.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingControllerTest {
    
    @Mock
    private RatingService ratingService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RatingRepository ratingRepository;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private UserDetails userDetails;
    
    @InjectMocks
    private RatingController ratingController;
    
    private User testUser;
    private CreateRatingRequest createRequest;
    private UpdateRatingRequest updateRequest;
    private RatingResponse ratingResponse;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("testuser")
                .passwordHash("encodedPassword")
                .role(Role.USER)
                .build();
        
        createRequest = new CreateRatingRequest();
        createRequest.setValue((short) 5);
        
        updateRequest = new UpdateRatingRequest();
        updateRequest.setValue((short) 4);
        
        ratingResponse = RatingResponse.builder()
                .id(1L)
                .bookId(1L)
                .userId(1L)
                .value((short) 5)
                .build();
        
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }
    
    @Test
    void testCreateRating_Success_ShouldReturn201() {
        // Given
        when(ratingService.createRating(anyLong(), anyLong(), any(CreateRatingRequest.class)))
                .thenReturn(ratingResponse);
        
        // When
        ResponseEntity<RatingResponse> response = ratingController.createRating(1L, createRequest, authentication);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals((short) 5, response.getBody().getValue());
    }
    
    @Test
    void testUpdateMyRating_Success_ShouldReturn200() {
        // Given
        Rating oldRating = Rating.builder()
                .id(1L)
                .value((short) 5)
                .build();
        
        RatingResponse updatedResponse = RatingResponse.builder()
                .id(1L)
                .bookId(1L)
                .userId(1L)
                .value((short) 4)
                .build();
        
        when(ratingRepository.findByBookIdAndUserId(1L, 1L)).thenReturn(Optional.of(oldRating));
        when(ratingService.updateRating(anyLong(), anyLong(), any(UpdateRatingRequest.class)))
                .thenReturn(updatedResponse);
        
        // When
        ResponseEntity<RatingResponse> response = ratingController.updateMyRating(1L, updateRequest, authentication);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals((short) 4, response.getBody().getValue());
        
        verify(ratingRepository).findByBookIdAndUserId(1L, 1L);
        verify(ratingService).updateRating(anyLong(), anyLong(), any(UpdateRatingRequest.class));
    }
}

