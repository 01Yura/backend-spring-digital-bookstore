package online.ityura.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.ityura.springdigitallibrary.dto.analytics.BookStatisticsAggregated;
import online.ityura.springdigitallibrary.dto.analytics.PopularBooksAggregated;
import online.ityura.springdigitallibrary.dto.analytics.SystemOverviewAggregated;
import online.ityura.analytics.model.BookStatistics;
import online.ityura.analytics.service.AnalyticsService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsAggregationScheduler {

    private final AnalyticsService analyticsService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String AGGREGATED_STATS_TOPIC = "analytics.aggregated-stats";

    // Агрегация с интервалом из конфигурации (по умолчанию 1 минута)
    @Scheduled(fixedRateString = "${analytics.aggregation.interval-ms:60000}")
    public void aggregateAndSendStatistics() {
        log.info("Starting statistics aggregation...");

        try {
            // 1. Агрегировать статистику по всем книгам
            Map<Long, BookStatistics> allBookStats = analyticsService.getAllBookStatistics();
            for (BookStatistics stats : allBookStats.values()) {
                BookStatisticsAggregated aggregated = convertToAggregated(stats);
                kafkaTemplate.send(
                    AGGREGATED_STATS_TOPIC,
                    "BOOK_STATS",
                    aggregated
                );
                log.debug("Sent aggregated stats for book: {}", stats.getBookId());
            }

            // 2. Отправить общую статистику системы
            AnalyticsService.SystemOverview systemOverview = analyticsService.getSystemOverview();
            SystemOverviewAggregated systemOverviewAggregated = convertToSystemOverviewAggregated(systemOverview);
            kafkaTemplate.send(
                AGGREGATED_STATS_TOPIC,
                "SYSTEM_OVERVIEW",
                systemOverviewAggregated
            );
            log.debug("Sent system overview");

            // 3. Отправить популярные книги
            List<BookStatistics> popularBooks = analyticsService.getPopularBooks(10, "views");
            PopularBooksAggregated popularBooksAggregated = convertToPopularBooksAggregated(popularBooks, 10, "views");
            kafkaTemplate.send(
                AGGREGATED_STATS_TOPIC,
                "POPULAR_BOOKS",
                popularBooksAggregated
            );
            log.debug("Sent popular books");

            log.info("Statistics aggregation completed successfully. Processed {} books", allBookStats.size());
        } catch (Exception e) {
            log.error("Error during statistics aggregation", e);
        }
    }

    private BookStatisticsAggregated convertToAggregated(BookStatistics stats) {
        return BookStatisticsAggregated.builder()
            .aggregationType("BOOK_STATS")
            .timestamp(LocalDateTime.now())
            .bookId(stats.getBookId())
            .bookTitle(stats.getBookTitle())
            .bookGenre(stats.getBookGenre())
            .viewCount(stats.getViewCount().get())
            .downloadCount(stats.getDownloadCount().get())
            .purchaseCount(stats.getPurchaseCount().get())
            .reviewCount(stats.getReviewCount().get())
            .ratingCount(stats.getRatingCount().get())
            .averageRating(stats.getAverageRating())
            .totalRevenue(stats.getTotalRevenue().get() / 100.0) // из центов в доллары
            .uniqueViewers(stats.getUniqueViewers().size())
            .uniqueDownloaders(stats.getUniqueDownloaders().size())
            .uniquePurchasers(stats.getUniquePurchasers().size())
            .firstViewAt(stats.getFirstViewAt())
            .lastViewAt(stats.getLastViewAt())
            .lastPurchaseAt(stats.getLastPurchaseAt())
            .build();
    }

    private SystemOverviewAggregated convertToSystemOverviewAggregated(AnalyticsService.SystemOverview overview) {
        return SystemOverviewAggregated.builder()
            .aggregationType("SYSTEM_OVERVIEW")
            .timestamp(LocalDateTime.now())
            .totalBooks(overview.getTotalBooks())
            .totalUsers(overview.getTotalUsers())
            .totalViews(overview.getTotalViews())
            .totalDownloads(overview.getTotalDownloads())
            .totalPurchases(overview.getTotalPurchases())
            .totalRevenue(overview.getTotalRevenue())
            .totalReviews(overview.getTotalReviews())
            .totalRatings(overview.getTotalRatings())
            .averageRating(overview.getAverageRating())
            .averageReviewLength(overview.getAverageReviewLength())
            .mostPopularBookId(overview.getMostPopularBookId())
            .mostPopularBookTitle(overview.getMostPopularBookTitle())
            .topGenre(overview.getTopGenre())
            .topGenreBookCount(overview.getTopGenreBookCount())
            .topGenreTotalViews(overview.getTopGenreTotalViews())
            .build();
    }

    private PopularBooksAggregated convertToPopularBooksAggregated(List<BookStatistics> books, int limit, String sortBy) {
        List<PopularBooksAggregated.PopularBookItem> items = IntStream.range(0, books.size())
            .mapToObj(i -> {
                BookStatistics stats = books.get(i);
                return PopularBooksAggregated.PopularBookItem.builder()
                    .bookId(stats.getBookId())
                    .bookTitle(stats.getBookTitle())
                    .viewCount(stats.getViewCount().get())
                    .downloadCount(stats.getDownloadCount().get())
                    .purchaseCount(stats.getPurchaseCount().get())
                    .totalRevenue(stats.getTotalRevenue().get() / 100.0) // из центов в доллары
                    .rank(i + 1)
                    .build();
            })
            .toList();

        return PopularBooksAggregated.builder()
            .aggregationType("POPULAR_BOOKS")
            .timestamp(LocalDateTime.now())
            .books(items)
            .limit(limit)
            .sortBy(sortBy)
            .build();
    }
}
