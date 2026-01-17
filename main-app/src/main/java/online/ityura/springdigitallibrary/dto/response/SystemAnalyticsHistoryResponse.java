package online.ityura.springdigitallibrary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.ityura.springdigitallibrary.model.SystemAnalytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAnalyticsHistoryResponse {
    private List<HistoryItem> history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private Integer totalBooks;
        private Integer totalUsers;
        private Long totalViews;
        private BigDecimal totalRevenue;
        private LocalDateTime aggregatedAt;
    }

    public static SystemAnalyticsHistoryResponse from(List<SystemAnalytics> history) {
        List<HistoryItem> items = history.stream()
            .map(analytics -> HistoryItem.builder()
                .totalBooks(analytics.getTotalBooks())
                .totalUsers(analytics.getTotalUsers())
                .totalViews(analytics.getTotalViews())
                .totalRevenue(analytics.getTotalRevenue())
                .aggregatedAt(analytics.getAggregatedAt())
                .build())
            .collect(Collectors.toList());

        return SystemAnalyticsHistoryResponse.builder()
            .history(items)
            .build();
    }
}
