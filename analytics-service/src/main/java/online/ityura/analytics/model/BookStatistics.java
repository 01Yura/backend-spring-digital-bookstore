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
public class BookStatistics {
    private Long bookId;
    private String bookTitle;
    private String bookGenre;

    // Счетчики
    private AtomicLong viewCount = new AtomicLong(0);
    private AtomicLong downloadCount = new AtomicLong(0);
    private AtomicLong purchaseCount = new AtomicLong(0);
    private AtomicLong reviewCount = new AtomicLong(0);
    private AtomicLong ratingCount = new AtomicLong(0);

    // Агрегированные данные
    private AtomicLong totalRevenue = new AtomicLong(0); // в центах
    private AtomicLong totalRatingsSum = new AtomicLong(0);
    private volatile double averageRating = 0.0;

    // Временные метки
    private LocalDateTime firstViewAt;
    private LocalDateTime lastViewAt;
    private LocalDateTime lastPurchaseAt;

    // Уникальные пользователи (Set для дедупликации)
    private Set<Long> uniqueViewers = ConcurrentHashMap.newKeySet();
    private Set<Long> uniqueDownloaders = ConcurrentHashMap.newKeySet();
    private Set<Long> uniquePurchasers = ConcurrentHashMap.newKeySet();

    public BookStatistics(Long bookId, String bookTitle, String bookGenre) {
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookGenre = bookGenre;
    }

    public void incrementViewCount() {
        viewCount.incrementAndGet();
    }

    public void incrementDownloadCount() {
        downloadCount.incrementAndGet();
    }

    public void incrementPurchaseCount() {
        purchaseCount.incrementAndGet();
    }

    public void incrementReviewCount() {
        reviewCount.incrementAndGet();
    }

    public void incrementRatingCount() {
        ratingCount.incrementAndGet();
    }

    public void addUniqueViewer(Long userId) {
        if (userId != null) {
            uniqueViewers.add(userId);
        }
    }

    public void addUniqueDownloader(Long userId) {
        if (userId != null) {
            uniqueDownloaders.add(userId);
        }
    }

    public void addUniquePurchaser(Long userId) {
        if (userId != null) {
            uniquePurchasers.add(userId);
        }
    }

    public void addRevenue(long cents) {
        totalRevenue.addAndGet(cents);
    }

    public void addRating(short ratingValue) {
        totalRatingsSum.addAndGet(ratingValue);
        updateAverageRating();
    }

    public void updateRating(short oldRating, short newRating) {
        totalRatingsSum.addAndGet(newRating - oldRating);
        updateAverageRating();
    }

    private void updateAverageRating() {
        long count = ratingCount.get();
        if (count > 0) {
            averageRating = totalRatingsSum.get() / (double) count;
        } else {
            averageRating = 0.0;
        }
    }

    public void updateLastViewAt(LocalDateTime timestamp) {
        if (firstViewAt == null) {
            firstViewAt = timestamp;
        }
        lastViewAt = timestamp;
    }

    public void updateLastPurchaseAt(LocalDateTime timestamp) {
        lastPurchaseAt = timestamp;
    }
}
