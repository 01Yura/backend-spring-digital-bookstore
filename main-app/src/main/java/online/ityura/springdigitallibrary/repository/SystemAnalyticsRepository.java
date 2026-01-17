package online.ityura.springdigitallibrary.repository;

import online.ityura.springdigitallibrary.model.SystemAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SystemAnalyticsRepository extends JpaRepository<SystemAnalytics, Long> {

    // Получить последнюю статистику системы
    Optional<SystemAnalytics> findFirstByOrderByAggregatedAtDesc();

    // Получить статистику за период
    List<SystemAnalytics> findByAggregatedAtBetween(
        LocalDateTime start,
        LocalDateTime end
    );
}
