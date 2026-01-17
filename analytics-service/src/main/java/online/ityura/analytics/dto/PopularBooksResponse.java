package online.ityura.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.ityura.analytics.model.BookStatistics;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularBooksResponse {
    private List<PopularBookItem> books;
    private Integer total;

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
        private Double totalRevenue;
        private Integer rank;

        public static PopularBookItem from(BookStatistics stats, int rank) {
            return PopularBookItem.builder()
                .bookId(stats.getBookId())
                .bookTitle(stats.getBookTitle())
                .viewCount(stats.getViewCount().get())
                .downloadCount(stats.getDownloadCount().get())
                .purchaseCount(stats.getPurchaseCount().get())
                .totalRevenue(stats.getTotalRevenue().get() / 100.0) // из центов в доллары
                .rank(rank)
                .build();
        }
    }

    public static PopularBooksResponse from(List<BookStatistics> books) {
        List<PopularBookItem> items = books.stream()
            .map((stats) -> PopularBookItem.from(stats, books.indexOf(stats) + 1))
            .collect(Collectors.toList());

        return PopularBooksResponse.builder()
            .books(items)
            .total(items.size())
            .build();
    }
}
