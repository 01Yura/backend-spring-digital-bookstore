package online.ityura.springdigitallibrary.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularBooksAggregated {
    @JsonProperty("aggregationType")
    private String aggregationType = "POPULAR_BOOKS";
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("books")
    private List<PopularBookItem> books;
    
    @JsonProperty("limit")
    private Integer limit;
    
    @JsonProperty("sortBy")
    private String sortBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopularBookItem {
        @JsonProperty("bookId")
        private Long bookId;
        
        @JsonProperty("bookTitle")
        private String bookTitle;
        
        @JsonProperty("viewCount")
        private Long viewCount;
        
        @JsonProperty("downloadCount")
        private Long downloadCount;
        
        @JsonProperty("purchaseCount")
        private Long purchaseCount;
        
        @JsonProperty("totalRevenue")
        private Double totalRevenue;
        
        @JsonProperty("rank")
        private Integer rank;
    }
}
