package online.ityura.springdigitallibrary.repository;

import online.ityura.springdigitallibrary.model.BookAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookAnalyticsRepository extends JpaRepository<BookAnalytics, Long> {

    // Получить последнюю статистику по книге
    Optional<BookAnalytics> findFirstByBookIdOrderByAggregatedAtDesc(Long bookId);

    // Получить статистику за период
    List<BookAnalytics> findByBookIdAndAggregatedAtBetween(
        Long bookId,
        LocalDateTime start,
        LocalDateTime end
    );

    // Получить все книги с последней статистикой
    @Query("SELECT ba FROM BookAnalytics ba " +
           "WHERE ba.aggregatedAt = (SELECT MAX(ba2.aggregatedAt) " +
           "FROM BookAnalytics ba2 WHERE ba2.bookId = ba.bookId)")
    List<BookAnalytics> findLatestForAllBooks();
}
