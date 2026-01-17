package online.ityura.springdigitallibrary.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "book_analytics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "book_title")
    private String bookTitle;

    @Column(name = "book_genre")
    private String bookGenre;

    @Column(name = "view_count")
    private Long viewCount;

    @Column(name = "download_count")
    private Long downloadCount;

    @Column(name = "purchase_count")
    private Long purchaseCount;

    @Column(name = "review_count")
    private Long reviewCount;

    @Column(name = "rating_count")
    private Long ratingCount;

    @Column(name = "average_rating", precision = 4, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "total_revenue", precision = 10, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "unique_viewers")
    private Integer uniqueViewers;

    @Column(name = "unique_downloaders")
    private Integer uniqueDownloaders;

    @Column(name = "unique_purchasers")
    private Integer uniquePurchasers;

    @Column(name = "aggregated_at", nullable = false)
    private LocalDateTime aggregatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
