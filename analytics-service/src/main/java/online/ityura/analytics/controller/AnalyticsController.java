package online.ityura.analytics.controller;

import lombok.RequiredArgsConstructor;
import online.ityura.analytics.dto.*;
import online.ityura.analytics.model.BookStatistics;
import online.ityura.analytics.model.UserActivity;
import online.ityura.analytics.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/books/{bookId}/stats")
    public ResponseEntity<BookStatisticsResponse> getBookStats(@PathVariable Long bookId) {
        BookStatistics stats = analyticsService.getBookStatistics(bookId);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(BookStatisticsResponse.from(stats));
    }

    @GetMapping("/books/popular")
    public ResponseEntity<PopularBooksResponse> getPopularBooks(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "views") String sortBy) {
        List<BookStatistics> books = analyticsService.getPopularBooks(limit, sortBy);
        return ResponseEntity.ok(PopularBooksResponse.from(books));
    }

    @GetMapping("/users/{userId}/activity")
    public ResponseEntity<UserActivityResponse> getUserActivity(@PathVariable Long userId) {
        UserActivity activity = analyticsService.getUserActivity(userId);
        if (activity == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(UserActivityResponse.from(activity));
    }

    @GetMapping("/overview")
    public ResponseEntity<SystemOverviewResponse> getOverview() {
        AnalyticsService.SystemOverview overview = analyticsService.getSystemOverview();
        return ResponseEntity.ok(SystemOverviewResponse.from(overview));
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        return ResponseEntity.ok(HealthResponse.from(
            "UP",
            analyticsService.getEventsProcessed(),
            analyticsService.getBooksTracked(),
            analyticsService.getUsersTracked(),
            analyticsService.getStartTime()
        ));
    }
}
