package online.ityura.springdigitallibrary.service;

import online.ityura.springdigitallibrary.dto.request.CreateReviewRequest;
import online.ityura.springdigitallibrary.dto.request.UpdateReviewRequest;
import online.ityura.springdigitallibrary.dto.response.ReviewResponse;
import online.ityura.springdigitallibrary.dto.response.UserInfoResponse;
import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.model.Review;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.BookRepository;
import online.ityura.springdigitallibrary.repository.ReviewRepository;
import online.ityura.springdigitallibrary.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewService {
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public ReviewResponse createReview(Long bookId, Long userId, CreateReviewRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Book not found with id: " + bookId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "User not found with id: " + userId));
        
        // Проверка на существующий отзыв
        if (reviewRepository.existsByBookIdAndUserId(bookId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Review already exists for this book");
        }
        
        Review review = Review.builder()
                .book(book)
                .user(user)
                .text(request.getText())
                .build();
        
        review = reviewRepository.save(review);
        return mapToReviewResponse(review);
    }
    
    @Transactional
    public ReviewResponse updateReview(Long bookId, Long userId, UpdateReviewRequest request) {
        Review review = reviewRepository.findByBookIdAndUserId(bookId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Review not found"));
        
        review.setText(request.getText());
        review = reviewRepository.save(review);
        return mapToReviewResponse(review);
    }
    
    public Page<ReviewResponse> getReviewsByBookId(Long bookId, Pageable pageable) {
        return reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId, pageable)
                .map(this::mapToReviewResponse);
    }
    
    public ReviewResponse getMyReview(Long bookId, Long userId) {
        Review review = reviewRepository.findByBookIdAndUserId(bookId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Review not found"));
        return mapToReviewResponse(review);
    }
    
    public Page<ReviewResponse> getMyReviews(Long userId, Pageable pageable) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToReviewResponse);
    }
    
    private ReviewResponse mapToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .bookId(review.getBook().getId())
                .user(UserInfoResponse.builder()
                        .id(review.getUser().getId())
                        .nickname(review.getUser().getNickname())
                        .email(review.getUser().getEmail())
                        .build())
                .text(review.getText())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}

