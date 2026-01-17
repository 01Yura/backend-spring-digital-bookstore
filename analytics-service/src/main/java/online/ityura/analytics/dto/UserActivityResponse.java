package online.ityura.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.ityura.analytics.model.UserActivity;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityResponse {
    private Long userId;
    private Long booksViewed;
    private Long booksDownloaded;
    private Long booksPurchased;
    private Long reviewsCreated;
    private Long ratingsCreated;
    private Double totalSpent;
    private Set<Long> viewedBooks;
    private Set<Long> downloadedBooks;
    private Set<Long> purchasedBooks;
    private LocalDateTime firstActivityAt;
    private LocalDateTime lastActivityAt;

    public static UserActivityResponse from(UserActivity activity) {
        if (activity == null) {
            return null;
        }
        return UserActivityResponse.builder()
            .userId(activity.getUserId())
            .booksViewed(activity.getBooksViewed().get())
            .booksDownloaded(activity.getBooksDownloaded().get())
            .booksPurchased(activity.getBooksPurchased().get())
            .reviewsCreated(activity.getReviewsCreated().get())
            .ratingsCreated(activity.getRatingsCreated().get())
            .totalSpent(activity.getTotalSpent().get() / 100.0) // из центов в доллары
            .viewedBooks(activity.getViewedBooks())
            .downloadedBooks(activity.getDownloadedBooks())
            .purchasedBooks(activity.getPurchasedBooks())
            .firstActivityAt(activity.getFirstActivityAt())
            .lastActivityAt(activity.getLastActivityAt())
            .build();
    }
}
