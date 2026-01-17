package online.ityura.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {
    private String status;
    private Long eventsProcessed;
    private Integer booksTracked;
    private Integer usersTracked;
    private Long uptime; // в миллисекундах

    public static HealthResponse from(String status, long eventsProcessed, int booksTracked, 
                                     int usersTracked, LocalDateTime startTime) {
        long uptime = Duration.between(startTime, LocalDateTime.now()).toMillis();
        return HealthResponse.builder()
            .status(status)
            .eventsProcessed(eventsProcessed)
            .booksTracked(booksTracked)
            .usersTracked(usersTracked)
            .uptime(uptime)
            .build();
    }
}
