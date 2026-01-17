package online.ityura.springdigitallibrary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.ityura.springdigitallibrary.model.BookAnalytics;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularBooksResponse {
    private List<PopularBookItem> books;
    private Integer total;
    private String sortBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopularBookItem {
        private Long bookId;
        private String bookTitle;
        private Long viewCount;
        private Long downloadCount;
        private Long purchaseCount;
        private BigDecimal totalRevenue;
        private Integer rank;
    }

    public static PopularBooksResponse from(List<BookAnalytics> analyticsList, String sortBy) {
        List<PopularBookItem> items = analyticsList.stream()
            .map(analytics -> {
                int index = analyticsList.indexOf(analytics);
                return PopularBookItem.builder()
                    .bookId(analytics.getBookId())
                    .bookTitle(analytics.getBookTitle())
                    .viewCount(analytics.getViewCount())
                    .downloadCount(analytics.getDownloadCount())
                    .purchaseCount(analytics.getPurchaseCount())
                    .totalRevenue(analytics.getTotalRevenue())
                    .rank(index + 1)
                    .build();
            })
            .collect(Collectors.toList());

        return PopularBooksResponse.builder()
            .books(items)
            .total(items.size())
            .sortBy(sortBy)
            .build();
    }
}
