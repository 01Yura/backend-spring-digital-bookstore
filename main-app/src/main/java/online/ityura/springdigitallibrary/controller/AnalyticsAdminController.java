package online.ityura.springdigitallibrary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import online.ityura.springdigitallibrary.dto.response.*;
import online.ityura.springdigitallibrary.model.BookAnalytics;
import online.ityura.springdigitallibrary.model.SystemAnalytics;
import online.ityura.springdigitallibrary.repository.BookAnalyticsRepository;
import online.ityura.springdigitallibrary.repository.SystemAnalyticsRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Админ - Аналитика", description = "API для просмотра аналитики в админ панели")
@SecurityRequirement(name = "Bearer Authentication")
public class AnalyticsAdminController {

    private final BookAnalyticsRepository bookAnalyticsRepository;
    private final SystemAnalyticsRepository systemAnalyticsRepository;

    @Operation(summary = "Получить статистику по книге")
    @GetMapping("/books/{bookId}")
    public ResponseEntity<BookAnalyticsResponse> getBookAnalytics(@PathVariable Long bookId) {
        BookAnalytics analytics = bookAnalyticsRepository
            .findFirstByBookIdOrderByAggregatedAtDesc(bookId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Analytics not found for book: " + bookId
            ));

        return ResponseEntity.ok(BookAnalyticsResponse.from(analytics));
    }

    @Operation(summary = "Получить историю статистики по книге")
    @GetMapping("/books/{bookId}/history")
    public ResponseEntity<BookAnalyticsHistoryResponse> getBookAnalyticsHistory(
            @PathVariable Long bookId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        List<BookAnalytics> history = bookAnalyticsRepository
            .findByBookIdAndAggregatedAtBetween(bookId, start, end);

        if (history.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No analytics history found for book: " + bookId
            );
        }

        String bookTitle = history.get(0).getBookTitle();
        return ResponseEntity.ok(BookAnalyticsHistoryResponse.from(bookId, bookTitle, history));
    }

    @Operation(summary = "Получить общую статистику системы")
    @GetMapping("/overview")
    public ResponseEntity<SystemAnalyticsResponse> getSystemOverview() {
        SystemAnalytics analytics = systemAnalyticsRepository
            .findFirstByOrderByAggregatedAtDesc()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "System analytics not found"
            ));

        return ResponseEntity.ok(SystemAnalyticsResponse.from(analytics));
    }

    @Operation(summary = "Получить популярные книги")
    @GetMapping("/popular")
    public ResponseEntity<PopularBooksResponse> getPopularBooks(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "views") String sortBy) {

        List<BookAnalytics> allBooks = bookAnalyticsRepository.findLatestForAllBooks();

        // Сортировка и фильтрация
        List<BookAnalytics> sorted = allBooks.stream()
            .sorted(getComparator(sortBy))
            .limit(limit)
            .collect(Collectors.toList());

        return ResponseEntity.ok(PopularBooksResponse.from(sorted, sortBy));
    }

    @Operation(summary = "Получить историю общей статистики")
    @GetMapping("/overview/history")
    public ResponseEntity<SystemAnalyticsHistoryResponse> getSystemOverviewHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        List<SystemAnalytics> history = systemAnalyticsRepository
            .findByAggregatedAtBetween(start, end);

        return ResponseEntity.ok(SystemAnalyticsHistoryResponse.from(history));
    }

    private Comparator<BookAnalytics> getComparator(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "downloads" -> Comparator.comparing(BookAnalytics::getDownloadCount, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            case "purchases" -> Comparator.comparing(BookAnalytics::getPurchaseCount, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            case "revenue" -> Comparator.comparing(BookAnalytics::getTotalRevenue, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            default -> Comparator.comparing(BookAnalytics::getViewCount, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        };
    }
}
