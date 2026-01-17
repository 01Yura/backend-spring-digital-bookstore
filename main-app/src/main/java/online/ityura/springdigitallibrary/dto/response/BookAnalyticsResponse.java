package online.ityura.springdigitallibrary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.ityura.springdigitallibrary.model.BookAnalytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAnalyticsResponse {
    private Long bookId;
    private String bookTitle;
    private String bookGenre;
    private Long viewCount;
    private Long downloadCount;
    private Long purchaseCount;
    private Long reviewCount;
    private Long ratingCount;
    private BigDecimal averageRating;
    private BigDecimal totalRevenue;
    private Integer uniqueViewers;
    private Integer uniqueDownloaders;
    private Integer uniquePurchasers;
    private LocalDateTime aggregatedAt;
    private LocalDateTime createdAt;

    public static BookAnalyticsResponse from(BookAnalytics analytics) {
        return BookAnalyticsResponse.builder()
            .bookId(analytics.getBookId())
            .bookTitle(analytics.getBookTitle())
            .bookGenre(analytics.getBookGenre())
            .viewCount(analytics.getViewCount())
            .downloadCount(analytics.getDownloadCount())
            .purchaseCount(analytics.getPurchaseCount())
            .reviewCount(analytics.getReviewCount())
            .ratingCount(analytics.getRatingCount())
            .averageRating(analytics.getAverageRating())
            .totalRevenue(analytics.getTotalRevenue())
            .uniqueViewers(analytics.getUniqueViewers())
            .uniqueDownloaders(analytics.getUniqueDownloaders())
            .uniquePurchasers(analytics.getUniquePurchasers())
            .aggregatedAt(analytics.getAggregatedAt())
            .createdAt(analytics.getCreatedAt())
            .build();
    }
}
