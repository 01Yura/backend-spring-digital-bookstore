package online.ityura.springdigitallibrary.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.ityura.springdigitallibrary.dto.analytics.BookStatisticsAggregated;
import online.ityura.springdigitallibrary.dto.analytics.SystemOverviewAggregated;
import online.ityura.springdigitallibrary.model.BookAnalytics;
import online.ityura.springdigitallibrary.model.SystemAnalytics;
import online.ityura.springdigitallibrary.repository.BookAnalyticsRepository;
import online.ityura.springdigitallibrary.repository.SystemAnalyticsRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class AnalyticsStatsConsumer {

    private final BookAnalyticsRepository bookAnalyticsRepository;
    private final SystemAnalyticsRepository systemAnalyticsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper() {{
        registerModule(new JavaTimeModule());
    }};

    @KafkaListener(
        topics = "analytics.aggregated-stats",
        groupId = "main-app-analytics-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAggregatedStats(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        log.info("Received aggregated stats with key: {}", key);

        try {
            switch (key) {
                case "BOOK_STATS":
                    BookStatisticsAggregated bookStats = objectMapper.convertValue(
                        payload,
                        BookStatisticsAggregated.class
                    );
                    saveBookAnalytics(bookStats);
                    break;

                case "SYSTEM_OVERVIEW":
                    SystemOverviewAggregated systemOverview = objectMapper.convertValue(
                        payload,
                        SystemOverviewAggregated.class
                    );
                    saveSystemAnalytics(systemOverview);
                    break;

                case "POPULAR_BOOKS":
                    // Можно сохранить в отдельную таблицу или пропустить
                    // (данные уже есть в book_analytics)
                    log.debug("Received popular books, skipping save");
                    break;

                default:
                    log.warn("Unknown aggregation type: {}", key);
            }
        } catch (Exception e) {
            log.error("Error processing aggregated stats", e);
            // Можно отправить в DLQ (Dead Letter Queue)
        }
    }

    private void saveBookAnalytics(BookStatisticsAggregated stats) {
        BookAnalytics analytics = BookAnalytics.builder()
            .bookId(stats.getBookId())
            .bookTitle(stats.getBookTitle())
            .bookGenre(stats.getBookGenre())
            .viewCount(stats.getViewCount())
            .downloadCount(stats.getDownloadCount())
            .purchaseCount(stats.getPurchaseCount())
            .reviewCount(stats.getReviewCount())
            .ratingCount(stats.getRatingCount())
            .averageRating(stats.getAverageRating() != null 
                ? BigDecimal.valueOf(stats.getAverageRating()) 
                : null)
            .totalRevenue(stats.getTotalRevenue() != null 
                ? BigDecimal.valueOf(stats.getTotalRevenue()) 
                : null)
            .uniqueViewers(stats.getUniqueViewers())
            .uniqueDownloaders(stats.getUniqueDownloaders())
            .uniquePurchasers(stats.getUniquePurchasers())
            .aggregatedAt(stats.getTimestamp())
            .build();

        // Сохраняем новую запись (можно добавить логику обновления последней)
        bookAnalyticsRepository.save(analytics);
        log.info("Saved book analytics for bookId: {}", stats.getBookId());
    }

    private void saveSystemAnalytics(SystemOverviewAggregated stats) {
        SystemAnalytics analytics = SystemAnalytics.builder()
            .totalBooks(stats.getTotalBooks())
            .totalUsers(stats.getTotalUsers())
            .totalViews(stats.getTotalViews())
            .totalDownloads(stats.getTotalDownloads())
            .totalPurchases(stats.getTotalPurchases())
            .totalRevenue(stats.getTotalRevenue() != null 
                ? BigDecimal.valueOf(stats.getTotalRevenue()) 
                : null)
            .totalReviews(stats.getTotalReviews())
            .totalRatings(stats.getTotalRatings())
            .averageRating(stats.getAverageRating() != null 
                ? BigDecimal.valueOf(stats.getAverageRating()) 
                : null)
            .averageReviewLength(stats.getAverageReviewLength() != null 
                ? BigDecimal.valueOf(stats.getAverageReviewLength()) 
                : null)
            .mostPopularBookId(stats.getMostPopularBookId())
            .mostPopularBookTitle(stats.getMostPopularBookTitle())
            .topGenre(stats.getTopGenre())
            .topGenreBookCount(stats.getTopGenreBookCount())
            .topGenreTotalViews(stats.getTopGenreTotalViews())
            .aggregatedAt(stats.getTimestamp())
            .build();

        systemAnalyticsRepository.save(analytics);
        log.info("Saved system analytics");
    }
}
