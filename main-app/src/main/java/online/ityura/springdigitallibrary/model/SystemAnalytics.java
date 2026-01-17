package online.ityura.springdigitallibrary.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_analytics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_books")
    private Integer totalBooks;

    @Column(name = "total_users")
    private Integer totalUsers;

    @Column(name = "total_views")
    private Long totalViews;

    @Column(name = "total_downloads")
    private Long totalDownloads;

    @Column(name = "total_purchases")
    private Long totalPurchases;

    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "total_reviews")
    private Long totalReviews;

    @Column(name = "total_ratings")
    private Long totalRatings;

    @Column(name = "average_rating", precision = 4, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "average_review_length", precision = 6, scale = 2)
    private BigDecimal averageReviewLength;

    @Column(name = "most_popular_book_id")
    private Long mostPopularBookId;

    @Column(name = "most_popular_book_title")
    private String mostPopularBookTitle;

    @Column(name = "top_genre")
    private String topGenre;

    @Column(name = "top_genre_book_count")
    private Integer topGenreBookCount;

    @Column(name = "top_genre_total_views")
    private Long topGenreTotalViews;

    @Column(name = "aggregated_at", nullable = false)
    private LocalDateTime aggregatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
