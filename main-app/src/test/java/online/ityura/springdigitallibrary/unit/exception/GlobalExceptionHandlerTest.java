package online.ityura.springdigitallibrary.unit.exception;

import jakarta.servlet.http.HttpServletRequest;
import online.ityura.springdigitallibrary.dto.response.ErrorResponse;
import online.ityura.springdigitallibrary.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {
    
    @Mock
    private HttpServletRequest request;
    
    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;
    
    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }
    
    @Test
    void testHandleResponseStatusException_NotFound_ShouldReturn404() {
        // Given
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResponseStatusException(ex, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("BOOK_NOT_FOUND", response.getBody().getError());
        assertEquals("Book not found", response.getBody().getMessage());
    }
    
    @Test
    void testHandleResponseStatusException_Conflict_ShouldReturn409() {
        // Given
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT, "Book already exists");
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResponseStatusException(ex, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("BOOK_ALREADY_EXISTS", response.getBody().getError());
    }
    
    @Test
    void testHandleRuntimeException_ShouldReturn500() {
        // Given
        RuntimeException ex = new RuntimeException("Internal error");
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntimeException(ex, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getError());
        assertEquals("Internal error", response.getBody().getMessage());
    }
    
    @Test
    void testHandleValidationExceptions_ShouldReturn400() {
        // Given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "error message");
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(fieldError));
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationExceptions(ex, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("VALIDATION_ERROR", response.getBody().getError());
        assertNotNull(response.getBody().getFieldErrors());
        assertTrue(response.getBody().getFieldErrors().containsKey("field"));
    }
    
    @Test
    void testHandleHttpMessageNotReadableException_ShouldReturn400() {
        // Given
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("Invalid format");
        when(ex.getCause()).thenReturn(null);
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleHttpMessageNotReadableException(ex, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("VALIDATION_ERROR", response.getBody().getError());
    }
}

