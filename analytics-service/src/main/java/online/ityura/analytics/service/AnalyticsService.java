package online.ityura.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.ityura.analytics.model.BookStatistics;
import online.ityura.analytics.model.ReviewStatistics;
import online.ityura.analytics.model.UserActivity;
import online.ityura.springdigitallibrary.dto.event.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    // In-memory storage
    private final ConcurrentHashMap<Long, BookStatistics> bookStats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, UserActivity> userActivity = new ConcurrentHashMap<>();
    private final ReviewStatistics reviewStats = new ReviewStatistics();

    // Счетчик обработанных событий
    private long eventsProcessed = 0;
    private final LocalDateTime startTime = LocalDateTime.now();

    public void processBookView(BookViewEvent event) {
        log.debug("Processing book view event: {}", event);
        eventsProcessed++;

        BookStatistics stats = bookStats.computeIfAbsent(
            event.getBookId(),
            id -> new BookStatistics(id, event.getBookTitle(), event.getBookGenre())
        );

        stats.incrementViewCount();
        stats.addUniqueViewer(event.getUserId());
        stats.updateLastViewAt(event.getTimestamp());

        // Обновление активности пользователя
        if (event.getUserId() != null) {
            UserActivity activity = userActivity.computeIfAbsent(
                event.getUserId(),
                UserActivity::new
            );
            activity.incrementBooksViewed();
            activity.addViewedBook(event.getBookId());
            activity.updateActivity(event.getTimestamp());
        }
    }

    public void processBookDownload(BookDownloadEvent event) {
        log.debug("Processing book download event: {}", event);
        eventsProcessed++;

        BookStatistics stats = bookStats.computeIfAbsent(
            event.getBookId(),
            id -> new BookStatistics(id, event.getBookTitle(), null)
        );

        stats.incrementDownloadCount();
        stats.addUniqueDownloader(event.getUserId());

        // Обновление активности пользователя
        if (event.getUserId() != null) {
            UserActivity activity = userActivity.computeIfAbsent(
                event.getUserId(),
                UserActivity::new
            );
            activity.incrementBooksDownloaded();
            activity.addDownloadedBook(event.getBookId());
            activity.updateActivity(event.getTimestamp());
        }
    }

    public void processBookPurchase(BookPurchaseEvent event) {
        log.debug("Processing book purchase event: {}", event);
        eventsProcessed++;

        BookStatistics stats = bookStats.computeIfAbsent(
            event.getBookId(),
            id -> new BookStatistics(id, event.getBookTitle(), null)
        );

        stats.incrementPurchaseCount();
        stats.addUniquePurchaser(event.getUserId());
        stats.updateLastPurchaseAt(event.getTimestamp());

        // Добавляем выручку (конвертируем доллары в центы)
        if (event.getAmountPaid() != null) {
            long cents = (long) (event.getAmountPaid() * 100);
            stats.addRevenue(cents);
        }

        // Обновление активности пользователя
        if (event.getUserId() != null) {
            UserActivity activity = userActivity.computeIfAbsent(
                event.getUserId(),
                UserActivity::new
            );
            activity.incrementBooksPurchased();
            activity.addPurchasedBook(event.getBookId());
            if (event.getAmountPaid() != null) {
                long cents = (long) (event.getAmountPaid() * 100);
                activity.addSpent(cents);
            }
            activity.updateActivity(event.getTimestamp());
        }
    }

    public void processBookReview(BookReviewEvent event) {
        log.debug("Processing book review event: {}", event);
        eventsProcessed++;

        BookStatistics stats = bookStats.computeIfAbsent(
            event.getBookId(),
            id -> new BookStatistics(id, null, null)
        );

        if ("CREATED".equals(event.getAction())) {
            stats.incrementReviewCount();
            reviewStats.incrementTotalReviews();
            reviewStats.incrementReviewsCreated();
        } else if ("UPDATED".equals(event.getAction())) {
            reviewStats.incrementReviewsUpdated();
        }

        if (event.getReviewLength() != null) {
            if ("CREATED".equals(event.getAction())) {
                reviewStats.addReviewLength(event.getReviewLength());
            } else if ("UPDATED".equals(event.getAction())) {
                // Для обновления нужно знать старую длину, но мы не храним её
                // Поэтому просто обновляем общую длину
                reviewStats.addReviewLength(event.getReviewLength());
            }
        }

        // Обновление активности пользователя
        if (event.getUserId() != null && "CREATED".equals(event.getAction())) {
            UserActivity activity = userActivity.computeIfAbsent(
                event.getUserId(),
                UserActivity::new
            );
            activity.incrementReviewsCreated();
            activity.updateActivity(event.getTimestamp());
        }
    }

    public void processBookRating(BookRatingEvent event) {
        log.debug("Processing book rating event: {}", event);
        eventsProcessed++;

        BookStatistics stats = bookStats.computeIfAbsent(
            event.getBookId(),
            id -> new BookStatistics(id, null, null)
        );

        if ("CREATED".equals(event.getAction())) {
            stats.incrementRatingCount();
            if (event.getRatingValue() != null) {
                stats.addRating(event.getRatingValue());
            }
        } else if ("UPDATED".equals(event.getAction())) {
            // При обновлении вычитаем старое значение и добавляем новое
            if (event.getRatingValue() != null && event.getOldRatingValue() != null) {
                stats.updateRating(event.getOldRatingValue(), event.getRatingValue());
            } else if (event.getRatingValue() != null) {
                // Если старое значение не передано (для обратной совместимости),
                // просто добавляем новое (неправильно, но лучше чем ничего)
                log.warn("Old rating value not provided for UPDATED event, bookId: {}", event.getBookId());
                stats.addRating(event.getRatingValue());
            }
        }

        // Обновление активности пользователя
        if (event.getUserId() != null && "CREATED".equals(event.getAction())) {
            UserActivity activity = userActivity.computeIfAbsent(
                event.getUserId(),
                UserActivity::new
            );
            activity.incrementRatingsCreated();
            activity.updateActivity(event.getTimestamp());
        }
    }

    // Методы для получения статистики

    public BookStatistics getBookStatistics(Long bookId) {
        return bookStats.get(bookId);
    }

    public UserActivity getUserActivity(Long userId) {
        return userActivity.get(userId);
    }

    public ReviewStatistics getReviewStatistics() {
        return reviewStats;
    }

    public Map<Long, BookStatistics> getAllBookStatistics() {
        return new HashMap<>(bookStats);
    }

    public List<BookStatistics> getPopularBooks(int limit, String sortBy) {
        List<BookStatistics> books = new ArrayList<>(bookStats.values());

        Comparator<BookStatistics> comparator;
        switch (sortBy.toLowerCase()) {
            case "downloads":
                comparator = Comparator.comparingLong(b -> b.getDownloadCount().get());
                break;
            case "purchases":
                comparator = Comparator.comparingLong(b -> b.getPurchaseCount().get());
                break;
            case "revenue":
                comparator = Comparator.comparingLong(b -> b.getTotalRevenue().get());
                break;
            case "views":
            default:
                comparator = Comparator.comparingLong(b -> b.getViewCount().get());
                break;
        }

        return books.stream()
            .sorted(comparator.reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    public SystemOverview getSystemOverview() {
        long totalViews = bookStats.values().stream()
            .mapToLong(b -> b.getViewCount().get())
            .sum();
        long totalDownloads = bookStats.values().stream()
            .mapToLong(b -> b.getDownloadCount().get())
            .sum();
        long totalPurchases = bookStats.values().stream()
            .mapToLong(b -> b.getPurchaseCount().get())
            .sum();
        long totalRevenue = bookStats.values().stream()
            .mapToLong(b -> b.getTotalRevenue().get())
            .sum();
        long totalReviews = bookStats.values().stream()
            .mapToLong(b -> b.getReviewCount().get())
            .sum();
        long totalRatings = bookStats.values().stream()
            .mapToLong(b -> b.getRatingCount().get())
            .sum();

        // Находим самую популярную книгу
        BookStatistics mostPopular = bookStats.values().stream()
            .max(Comparator.comparingLong(b -> b.getViewCount().get()))
            .orElse(null);

        // Находим топ жанр
        Map<String, Long> genreViews = new HashMap<>();
        bookStats.values().forEach(b -> {
            if (b.getBookGenre() != null) {
                genreViews.merge(b.getBookGenre(), b.getViewCount().get(), Long::sum);
            }
        });

        Map.Entry<String, Long> topGenre = genreViews.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);

        // Подсчитываем книги по жанру
        Map<String, Long> genreBookCount = new HashMap<>();
        bookStats.values().forEach(b -> {
            if (b.getBookGenre() != null) {
                genreBookCount.merge(b.getBookGenre(), 1L, Long::sum);
            }
        });

        return SystemOverview.builder()
            .totalBooks(bookStats.size())
            .totalUsers(userActivity.size())
            .totalViews(totalViews)
            .totalDownloads(totalDownloads)
            .totalPurchases(totalPurchases)
            .totalRevenue(totalRevenue / 100.0) // из центов в доллары
            .totalReviews(totalReviews)
            .totalRatings(totalRatings)
            .averageRating(calculateAverageRating())
            .averageReviewLength(reviewStats.getAverageReviewLength())
            .mostPopularBookId(mostPopular != null ? mostPopular.getBookId() : null)
            .mostPopularBookTitle(mostPopular != null ? mostPopular.getBookTitle() : null)
            .topGenre(topGenre != null ? topGenre.getKey() : null)
            .topGenreBookCount(topGenre != null ? genreBookCount.get(topGenre.getKey()).intValue() : null)
            .topGenreTotalViews(topGenre != null ? topGenre.getValue() : null)
            .build();
    }

    private double calculateAverageRating() {
        long totalRatings = bookStats.values().stream()
            .mapToLong(b -> b.getRatingCount().get())
            .sum();
        if (totalRatings == 0) {
            return 0.0;
        }
        long totalRatingsSum = bookStats.values().stream()
            .mapToLong(b -> b.getTotalRatingsSum().get())
            .sum();
        return totalRatingsSum / (double) totalRatings;
    }

    public long getEventsProcessed() {
        return eventsProcessed;
    }

    public int getBooksTracked() {
        return bookStats.size();
    }

    public int getUsersTracked() {
        return userActivity.size();
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    // Внутренний класс для SystemOverview
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SystemOverview {
        private Integer totalBooks;
        private Integer totalUsers;
        private Long totalViews;
        private Long totalDownloads;
        private Long totalPurchases;
        private Double totalRevenue;
        private Long totalReviews;
        private Long totalRatings;
        private Double averageRating;
        private Double averageReviewLength;
        private Long mostPopularBookId;
        private String mostPopularBookTitle;
        private String topGenre;
        private Integer topGenreBookCount;
        private Long topGenreTotalViews;
    }
}
