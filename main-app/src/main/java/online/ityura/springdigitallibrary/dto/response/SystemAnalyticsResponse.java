package online.ityura.springdigitallibrary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.ityura.springdigitallibrary.model.SystemAnalytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAnalyticsResponse {
    private Integer totalBooks;
    private Integer totalUsers;
    private Long totalViews;
    private Long totalDownloads;
    private Long totalPurchases;
    private BigDecimal totalRevenue;
    private Long totalReviews;
    private Long totalRatings;
    private BigDecimal averageRating;
    private BigDecimal averageReviewLength;
    private Long mostPopularBookId;
    private String mostPopularBookTitle;
    private String topGenre;
    private Integer topGenreBookCount;
    private Long topGenreTotalViews;
    private LocalDateTime aggregatedAt;

    public static SystemAnalyticsResponse from(SystemAnalytics analytics) {
        return SystemAnalyticsResponse.builder()
            .totalBooks(analytics.getTotalBooks())
            .totalUsers(analytics.getTotalUsers())
            .totalViews(analytics.getTotalViews())
            .totalDownloads(analytics.getTotalDownloads())
            .totalPurchases(analytics.getTotalPurchases())
            .totalRevenue(analytics.getTotalRevenue())
            .totalReviews(analytics.getTotalReviews())
            .totalRatings(analytics.getTotalRatings())
            .averageRating(analytics.getAverageRating())
            .averageReviewLength(analytics.getAverageReviewLength())
            .mostPopularBookId(analytics.getMostPopularBookId())
            .mostPopularBookTitle(analytics.getMostPopularBookTitle())
            .topGenre(analytics.getTopGenre())
            .topGenreBookCount(analytics.getTopGenreBookCount())
            .topGenreTotalViews(analytics.getTopGenreTotalViews())
            .aggregatedAt(analytics.getAggregatedAt())
            .build();
    }
}
