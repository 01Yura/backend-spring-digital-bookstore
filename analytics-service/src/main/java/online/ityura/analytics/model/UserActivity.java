package online.ityura.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivity {
    private Long userId;

    // Счетчики действий
    private AtomicLong booksViewed = new AtomicLong(0);
    private AtomicLong booksDownloaded = new AtomicLong(0);
    private AtomicLong booksPurchased = new AtomicLong(0);
    private AtomicLong reviewsCreated = new AtomicLong(0);
    private AtomicLong ratingsCreated = new AtomicLong(0);

    // Список просмотренных книг
    private Set<Long> viewedBooks = ConcurrentHashMap.newKeySet();

    // Список скачанных книг
    private Set<Long> downloadedBooks = ConcurrentHashMap.newKeySet();

    // Список купленных книг
    private Set<Long> purchasedBooks = ConcurrentHashMap.newKeySet();

    // Временные метки
    private LocalDateTime firstActivityAt;
    private LocalDateTime lastActivityAt;

    // Общая сумма покупок (в центах)
    private AtomicLong totalSpent = new AtomicLong(0);

    public UserActivity(Long userId) {
        this.userId = userId;
    }

    public void incrementBooksViewed() {
        booksViewed.incrementAndGet();
    }

    public void incrementBooksDownloaded() {
        booksDownloaded.incrementAndGet();
    }

    public void incrementBooksPurchased() {
        booksPurchased.incrementAndGet();
    }

    public void incrementReviewsCreated() {
        reviewsCreated.incrementAndGet();
    }

    public void incrementRatingsCreated() {
        ratingsCreated.incrementAndGet();
    }

    public void addViewedBook(Long bookId) {
        if (bookId != null) {
            viewedBooks.add(bookId);
        }
    }

    public void addDownloadedBook(Long bookId) {
        if (bookId != null) {
            downloadedBooks.add(bookId);
        }
    }

    public void addPurchasedBook(Long bookId) {
        if (bookId != null) {
            purchasedBooks.add(bookId);
        }
    }

    public void addSpent(long cents) {
        totalSpent.addAndGet(cents);
    }

    public void updateActivity(LocalDateTime timestamp) {
        if (firstActivityAt == null) {
            firstActivityAt = timestamp;
        }
        lastActivityAt = timestamp;
    }
}
