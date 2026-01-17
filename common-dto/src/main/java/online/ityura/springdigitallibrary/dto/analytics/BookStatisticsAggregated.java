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
public class BookStatisticsAggregated {
    @JsonProperty("aggregationType")
    private String aggregationType = "BOOK_STATS";
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("bookId")
    private Long bookId;
    
    @JsonProperty("bookTitle")
    private String bookTitle;
    
    @JsonProperty("bookGenre")
    private String bookGenre;
    
    @JsonProperty("viewCount")
    private Long viewCount;
    
    @JsonProperty("downloadCount")
    private Long downloadCount;
    
    @JsonProperty("purchaseCount")
    private Long purchaseCount;
    
    @JsonProperty("reviewCount")
    private Long reviewCount;
    
    @JsonProperty("ratingCount")
    private Long ratingCount;
    
    @JsonProperty("averageRating")
    private Double averageRating;
    
    @JsonProperty("totalRevenue")
    private Double totalRevenue;
    
    @JsonProperty("uniqueViewers")
    private Integer uniqueViewers;
    
    @JsonProperty("uniqueDownloaders")
    private Integer uniqueDownloaders;
    
    @JsonProperty("uniquePurchasers")
    private Integer uniquePurchasers;
    
    @JsonProperty("firstViewAt")
    private LocalDateTime firstViewAt;
    
    @JsonProperty("lastViewAt")
    private LocalDateTime lastViewAt;
    
    @JsonProperty("lastPurchaseAt")
    private LocalDateTime lastPurchaseAt;
}
