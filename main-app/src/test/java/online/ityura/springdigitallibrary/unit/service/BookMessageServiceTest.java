package online.ityura.springdigitallibrary.unit.service;

import online.ityura.springdigitallibrary.model.Author;
import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.model.Genre;
import online.ityura.springdigitallibrary.repository.BookRepository;
import online.ityura.springdigitallibrary.service.BookMessageService;
import online.ityura.springdigitallibrary.service.OpenAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookMessageServiceTest {
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private OpenAIService openAIService;
    
    @InjectMocks
    private BookMessageService bookMessageService;
    
    private Author testAuthor;
    private Book testBook;
    
    @BeforeEach
    void setUp() {
        testAuthor = Author.builder()
                .id(1L)
                .fullName("Лев Толстой")
                .build();
        
        testBook = Book.builder()
                .id(1L)
                .title("Война и мир")
                .author(testAuthor)
                .description("Эпический роман о войне 1812 года")
                .genre(Genre.FICTION)
                .build();
    }
    
    @Test
    void testSendMessageToReader_Success_ShouldReturnResponse() {
        // Given
        Long bookId = 1L;
        String message = "Что вам больше всего понравилось в этой книге?";
        String expectedResponse = "Мне очень понравился образ Пьера Безухова...";
        
        when(bookRepository.findByIdWithAuthor(bookId)).thenReturn(Optional.of(testBook));
        when(openAIService.sendPromptAndGetResponse(anyString())).thenReturn(expectedResponse);
        
        // When
        String result = bookMessageService.sendMessageToReader(bookId, message);
        
        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        
        verify(bookRepository).findByIdWithAuthor(bookId);
        verify(openAIService).sendPromptAndGetResponse(anyString());
        
        // Проверяем, что промпт содержит нужные данные
        verify(openAIService).sendPromptAndGetResponse(argThat(prompt -> 
                prompt.contains("Война и мир") &&
                prompt.contains("Лев Толстой") &&
                prompt.contains("Эпический роман о войне 1812 года") &&
                prompt.contains(message)
        ));
    }
    
    @Test
    void testSendMessageToReader_BookNotFound_ShouldThrowException() {
        // Given
        Long bookId = 999L;
        String message = "Как вам книга?";
        
        when(bookRepository.findByIdWithAuthor(bookId)).thenReturn(Optional.empty());
        
        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> bookMessageService.sendMessageToReader(bookId, message));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Book not found with id: " + bookId));
        
        verify(bookRepository).findByIdWithAuthor(bookId);
        verify(openAIService, never()).sendPromptAndGetResponse(anyString());
    }
    
    @Test
    void testSendMessageToReader_WithNullDescription_ShouldUseDefaultDescription() {
        // Given
        Book bookWithoutDescription = Book.builder()
                .id(2L)
                .title("Анна Каренина")
                .author(testAuthor)
                .description(null)
                .genre(Genre.FICTION)
                .build();
        
        Long bookId = 2L;
        String message = "Ваше мнение о книге?";
        String expectedResponse = "Отличная книга!";
        
        when(bookRepository.findByIdWithAuthor(bookId)).thenReturn(Optional.of(bookWithoutDescription));
        when(openAIService.sendPromptAndGetResponse(anyString())).thenReturn(expectedResponse);
        
        // When
        String result = bookMessageService.sendMessageToReader(bookId, message);
        
        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        
        // Проверяем, что в промпте используется дефолтное описание
        verify(openAIService).sendPromptAndGetResponse(argThat(prompt -> 
                prompt.contains("Описание отсутствует")
        ));
    }
    
    @Test
    void testSendMessageToReader_WithNullGenre_ShouldUseDefaultGenre() {
        // Given
        Book bookWithoutGenre = Book.builder()
                .id(3L)
                .title("Преступление и наказание")
                .author(testAuthor)
                .description("Психологический роман")
                .genre(null)
                .build();
        
        Long bookId = 3L;
        String message = "Что вы думаете о главном герое?";
        String expectedResponse = "Интересный персонаж!";
        
        when(bookRepository.findByIdWithAuthor(bookId)).thenReturn(Optional.of(bookWithoutGenre));
        when(openAIService.sendPromptAndGetResponse(anyString())).thenReturn(expectedResponse);
        
        // When
        String result = bookMessageService.sendMessageToReader(bookId, message);
        
        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        
        // Проверяем, что в промпте используется дефолтный жанр
        verify(openAIService).sendPromptAndGetResponse(argThat(prompt -> 
                prompt.contains("Не указан")
        ));
    }
    
    @Test
    void testSendMessageToReader_OpenAIServiceThrowsException_ShouldPropagateException() {
        // Given
        Long bookId = 1L;
        String message = "Ваш вопрос";
        RuntimeException openAIException = new RuntimeException("OpenAI API error");
        
        when(bookRepository.findByIdWithAuthor(bookId)).thenReturn(Optional.of(testBook));
        when(openAIService.sendPromptAndGetResponse(anyString())).thenThrow(openAIException);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> bookMessageService.sendMessageToReader(bookId, message));
        
        assertEquals("OpenAI API error", exception.getMessage());
        
        verify(bookRepository).findByIdWithAuthor(bookId);
        verify(openAIService).sendPromptAndGetResponse(anyString());
    }
    
    @Test
    void testSendMessageToReader_PromptContainsAllBookInfo() {
        // Given
        Long bookId = 1L;
        String message = "Тестовый вопрос";
        String expectedResponse = "Ответ";
        
        when(bookRepository.findByIdWithAuthor(bookId)).thenReturn(Optional.of(testBook));
        when(openAIService.sendPromptAndGetResponse(anyString())).thenReturn(expectedResponse);
        
        // When
        bookMessageService.sendMessageToReader(bookId, message);
        
        // Then
        verify(openAIService).sendPromptAndGetResponse(argThat(prompt -> {
            return prompt.contains(testBook.getTitle()) &&
                   prompt.contains(testAuthor.getFullName()) &&
                   prompt.contains(testBook.getDescription()) &&
                   prompt.contains(testBook.getGenre().getDisplayName()) &&
                   prompt.contains(message);
        }));
    }
}

