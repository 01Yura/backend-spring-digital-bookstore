package online.ityura.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.ityura.analytics.model.BookStatistics;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookStatisticsResponse {
    private Long bookId;
    private String bookTitle;
    private String bookGenre;
    private Long viewCount;
    private Long downloadCount;
    private Long purchaseCount;
    private Long reviewCount;
    private Long ratingCount;
    private Double averageRating;
    private Double totalRevenue;
    private Integer uniqueViewers;
    private Integer uniqueDownloaders;
    private Integer uniquePurchasers;
    private LocalDateTime firstViewAt;
    private LocalDateTime lastViewAt;
    private LocalDateTime lastPurchaseAt;

    public static BookStatisticsResponse from(BookStatistics stats) {
        if (stats == null) {
            return null;
        }
        return BookStatisticsResponse.builder()
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
}
