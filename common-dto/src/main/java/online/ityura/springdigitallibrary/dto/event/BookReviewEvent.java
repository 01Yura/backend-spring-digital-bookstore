package online.ityura.springdigitallibrary.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookReviewEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private Long bookId;
    private Long userId;
    private Long reviewId;
    private String action; // "CREATED" или "UPDATED"
    private Integer reviewLength;
}
