package online.ityura.springdigitallibrary.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemOverviewAggregated {
    @JsonProperty("aggregationType")
    private String aggregationType = "SYSTEM_OVERVIEW";
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("totalBooks")
    private Integer totalBooks;
    
    @JsonProperty("totalUsers")
    private Integer totalUsers;
    
    @JsonProperty("totalViews")
    private Long totalViews;
    
    @JsonProperty("totalDownloads")
    private Long totalDownloads;
    
    @JsonProperty("totalPurchases")
    private Long totalPurchases;
    
    @JsonProperty("totalRevenue")
    private Double totalRevenue;
    
    @JsonProperty("totalReviews")
    private Long totalReviews;
    
    @JsonProperty("totalRatings")
    private Long totalRatings;
    
    @JsonProperty("averageRating")
    private Double averageRating;
    
    @JsonProperty("averageReviewLength")
    private Double averageReviewLength;
    
    @JsonProperty("mostPopularBookId")
    private Long mostPopularBookId;
    
    @JsonProperty("mostPopularBookTitle")
    private String mostPopularBookTitle;
    
    @JsonProperty("topGenre")
    private String topGenre;
    
    @JsonProperty("topGenreBookCount")
    private Integer topGenreBookCount;
    
    @JsonProperty("topGenreTotalViews")
    private Long topGenreTotalViews;
}
