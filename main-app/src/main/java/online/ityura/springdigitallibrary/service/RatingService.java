package online.ityura.springdigitallibrary.service;

import online.ityura.springdigitallibrary.dto.request.CreateRatingRequest;
import online.ityura.springdigitallibrary.dto.request.UpdateRatingRequest;
import online.ityura.springdigitallibrary.dto.response.RatingResponse;
import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.model.Rating;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.BookRepository;
import online.ityura.springdigitallibrary.repository.RatingRepository;
import online.ityura.springdigitallibrary.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RatingService {
    
    @Autowired
    private RatingRepository ratingRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public RatingResponse createRating(Long bookId, Long userId, CreateRatingRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Book not found with id: " + bookId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "User not found with id: " + userId));
        
        // Проверка на существующий рейтинг
        if (ratingRepository.existsByBookIdAndUserId(bookId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Rating already exists for this book");
        }
        
        Rating rating = Rating.builder()
                .book(book)
                .user(user)
                .value(request.getValue())
                .build();
        
        rating = ratingRepository.save(rating);
        updateBookRating(bookId);
        
        return mapToRatingResponse(rating);
    }
    
    @Transactional
    public RatingResponse updateRating(Long bookId, Long userId, UpdateRatingRequest request) {
        Rating rating = ratingRepository.findByBookIdAndUserId(bookId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Rating not found"));
        
        rating.setValue(request.getValue());
        rating = ratingRepository.save(rating);
        updateBookRating(bookId);
        
        return mapToRatingResponse(rating);
    }
    
    private void updateBookRating(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Book not found with id: " + bookId));
        
        Double avgRating = ratingRepository.calculateAverageRating(bookId);
        long count = ratingRepository.countByBookId(bookId);
        
        if (avgRating != null) {
            book.setRatingAvg(BigDecimal.valueOf(avgRating)
                    .setScale(2, RoundingMode.HALF_UP));
        } else {
            book.setRatingAvg(BigDecimal.ZERO);
        }
        book.setRatingCount((int) count);
        
        bookRepository.save(book);
    }
    
    private RatingResponse mapToRatingResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .bookId(rating.getBook().getId())
                .userId(rating.getUser().getId())
                .value(rating.getValue())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }
}

