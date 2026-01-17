package online.ityura.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.ityura.analytics.service.AnalyticsService;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemOverviewResponse {
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
    private MostPopularBook mostPopularBook;
    private TopGenre topGenre;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MostPopularBook {
        private Long bookId;
        private String bookTitle;
        private Long viewCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopGenre {
        private String genre;
        private Integer bookCount;
        private Long totalViews;
    }

    public static SystemOverviewResponse from(AnalyticsService.SystemOverview overview) {
        if (overview == null) {
            return null;
        }

        MostPopularBook mostPopular = null;
        if (overview.getMostPopularBookId() != null) {
            mostPopular = MostPopularBook.builder()
                .bookId(overview.getMostPopularBookId())
                .bookTitle(overview.getMostPopularBookTitle())
                .viewCount(null) // Можно добавить, если нужно
                .build();
        }

        TopGenre topGenre = null;
        if (overview.getTopGenre() != null) {
            topGenre = TopGenre.builder()
                .genre(overview.getTopGenre())
                .bookCount(overview.getTopGenreBookCount())
                .totalViews(overview.getTopGenreTotalViews())
                .build();
        }

        return SystemOverviewResponse.builder()
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
            .mostPopularBook(mostPopular)
            .topGenre(topGenre)
            .build();
    }
}
