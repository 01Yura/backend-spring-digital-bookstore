package online.ityura.springdigitallibrary.unit.controller;

import online.ityura.springdigitallibrary.controller.HealthController;
import online.ityura.springdigitallibrary.dto.response.HealthResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {
    
    @InjectMocks
    private HealthController healthController;
    
    @Test
    void testHealth_ShouldReturnUpStatus() {
        // When
        ResponseEntity<HealthResponse> response = healthController.health();
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().getStatus());
        assertNotNull(response.getBody().getUptime());
        assertNotNull(response.getBody().getTimestamp());
        assertNotNull(response.getHeaders().getCacheControl());
    }
}

