package online.ityura.analytics.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicLong;

@Data
@NoArgsConstructor
public class ReviewStatistics {
    private AtomicLong totalReviews = new AtomicLong(0);
    private AtomicLong totalReviewLength = new AtomicLong(0);
    private volatile double averageReviewLength = 0.0;

    // Статистика по действиям
    private AtomicLong reviewsCreated = new AtomicLong(0);
    private AtomicLong reviewsUpdated = new AtomicLong(0);

    public void incrementTotalReviews() {
        totalReviews.incrementAndGet();
    }

    public void addReviewLength(int length) {
        totalReviewLength.addAndGet(length);
        updateAverageReviewLength();
    }

    public void updateReviewLength(int oldLength, int newLength) {
        totalReviewLength.addAndGet(newLength - oldLength);
        updateAverageReviewLength();
    }

    public void incrementReviewsCreated() {
        reviewsCreated.incrementAndGet();
    }

    public void incrementReviewsUpdated() {
        reviewsUpdated.incrementAndGet();
    }

    private void updateAverageReviewLength() {
        long count = totalReviews.get();
        if (count > 0) {
            averageReviewLength = totalReviewLength.get() / (double) count;
        } else {
            averageReviewLength = 0.0;
        }
    }
}
