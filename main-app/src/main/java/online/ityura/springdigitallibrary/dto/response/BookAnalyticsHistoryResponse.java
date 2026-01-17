package online.ityura.springdigitallibrary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.ityura.springdigitallibrary.model.BookAnalytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAnalyticsHistoryResponse {
    private Long bookId;
    private String bookTitle;
    private List<HistoryItem> history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private Long viewCount;
        private Long downloadCount;
        private Long purchaseCount;
        private BigDecimal totalRevenue;
        private LocalDateTime aggregatedAt;
    }

    public static BookAnalyticsHistoryResponse from(Long bookId, String bookTitle, List<BookAnalytics> history) {
        List<HistoryItem> items = history.stream()
            .map(analytics -> HistoryItem.builder()
                .viewCount(analytics.getViewCount())
                .downloadCount(analytics.getDownloadCount())
                .purchaseCount(analytics.getPurchaseCount())
                .totalRevenue(analytics.getTotalRevenue())
                .aggregatedAt(analytics.getAggregatedAt())
                .build())
            .collect(Collectors.toList());

        return BookAnalyticsHistoryResponse.builder()
            .bookId(bookId)
            .bookTitle(bookTitle)
            .history(items)
            .build();
    }
}
