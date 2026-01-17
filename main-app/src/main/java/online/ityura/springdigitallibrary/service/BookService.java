package online.ityura.springdigitallibrary.service;

import online.ityura.springdigitallibrary.dto.response.AuthorResponse;
import online.ityura.springdigitallibrary.dto.response.BookResponse;
import online.ityura.springdigitallibrary.dto.response.ReviewResponse;
import online.ityura.springdigitallibrary.dto.response.UserInfoResponse;
import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.model.Genre;
import online.ityura.springdigitallibrary.model.Review;
import online.ityura.springdigitallibrary.repository.BookRepository;
import online.ityura.springdigitallibrary.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookService {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    public Page<BookResponse> getAllBooks(Pageable pageable, Genre genre) {
        Page<Book> booksPage;
        
        if (genre != null) {
            booksPage = bookRepository.findByGenre(genre, pageable);
        } else {
            booksPage = bookRepository.findAll(pageable);
        }
        
        // Загружаем все отзывы для всех книг на странице одним запросом (избегаем N+1 проблемы)
        List<Long> bookIds = booksPage.getContent().stream()
                .map(Book::getId)
                .collect(Collectors.toList());
        
        // Загружаем отзывы для всех книг на странице (если есть книги)
        Map<Long, List<ReviewResponse>> reviewsByBookId;
        if (bookIds.isEmpty()) {
            reviewsByBookId = Map.of();
        } else {
            reviewsByBookId = reviewRepository.findByBookIdIn(bookIds).stream()
                    .collect(Collectors.groupingBy(
                            review -> review.getBook().getId(),
                            Collectors.mapping(this::mapToReviewResponse, Collectors.toList())
                    ));
        }
        
        // Маппим книги с отзывами
        return booksPage.map(book -> {
            List<ReviewResponse> reviews = reviewsByBookId.getOrDefault(book.getId(), List.of());
            return mapToBookResponseWithReviews(book, reviews);
        });
    }
    
    public BookResponse getBookById(Long bookId) {
        Book book = bookRepository.findByIdWithAuthor(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + bookId));
        
        // Загружаем отзывы для детальной информации о книге (с JOIN FETCH для избежания N+1 проблем)
        List<Review> reviews = reviewRepository.findByBookIdWithUserOrderByCreatedAtDesc(bookId);
        List<ReviewResponse> reviewResponses = reviews.stream()
                .map(this::mapToReviewResponse)
                .collect(Collectors.toList());
        
        return mapToBookResponseWithReviews(book, reviewResponses);
    }
    
    private BookResponse mapToBookResponse(Book book) {
        boolean hasFile = book.getPdfPath() != null && !book.getPdfPath().isEmpty();
        BigDecimal finalPrice = calculateFinalPrice(book);
        
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(AuthorResponse.builder()
                        .id(book.getAuthor().getId())
                        .fullName(book.getAuthor().getFullName())
                        .build())
                .description(book.getDescription())
                .publishedYear(book.getPublishedYear())
                .genre(book.getGenre())
                .ratingAvg(book.getRatingAvg())
                .ratingCount(book.getRatingCount())
                .hasFile(hasFile)
                .imagePath(book.getImagePath())
                .price(book.getPrice())
                .discountPercent(book.getDiscountPercent())
                .finalPrice(finalPrice)
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }
    
    private BookResponse mapToBookResponseWithReviews(Book book, List<ReviewResponse> reviews) {
        boolean hasFile = book.getPdfPath() != null && !book.getPdfPath().isEmpty();
        BigDecimal finalPrice = calculateFinalPrice(book);
        
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(AuthorResponse.builder()
                        .id(book.getAuthor().getId())
                        .fullName(book.getAuthor().getFullName())
                        .build())
                .description(book.getDescription())
                .publishedYear(book.getPublishedYear())
                .genre(book.getGenre())
                .ratingAvg(book.getRatingAvg())
                .ratingCount(book.getRatingCount())
                .hasFile(hasFile)
                .imagePath(book.getImagePath())
                .price(book.getPrice())
                .discountPercent(book.getDiscountPercent())
                .finalPrice(finalPrice)
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .reviews(reviews)
                .build();
    }
    
    /**
     * Вычисляет финальную цену с учетом скидки
     */
    private BigDecimal calculateFinalPrice(Book book) {
        BigDecimal price = book.getPrice();
        BigDecimal discountPercent = book.getDiscountPercent();
        
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            return price;
        }
        
        BigDecimal discountAmount = price.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        BigDecimal finalPrice = price.subtract(discountAmount);
        return finalPrice.max(BigDecimal.ZERO);
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

