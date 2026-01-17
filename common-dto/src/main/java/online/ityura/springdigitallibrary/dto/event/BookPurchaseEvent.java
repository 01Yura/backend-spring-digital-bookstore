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
public class BookPurchaseEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private Long bookId;
    private Long userId;
    private String bookTitle;
    private Double amountPaid;
    private Double originalPrice;
    private Double discountPercent;
    private String stripeSessionId;
}
