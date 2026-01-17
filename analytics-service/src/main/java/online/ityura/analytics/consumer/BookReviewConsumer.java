package online.ityura.analytics.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.ityura.analytics.service.AnalyticsService;
import online.ityura.springdigitallibrary.dto.event.BookReviewEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookReviewConsumer {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper = new ObjectMapper() {{
        registerModule(new JavaTimeModule());
    }};

    @KafkaListener(
        topics = "book.reviews",
        groupId = "analytics-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBookReview(@Payload java.util.Map<String, Object> payload) {
        log.info("Received book review event (raw): {}", payload);
        try {
            BookReviewEvent event = objectMapper.convertValue(payload, BookReviewEvent.class);
            log.info("Received book review event: {}", event);
            analyticsService.processBookReview(event);
        } catch (Exception e) {
            log.error("Error processing book review event: {}", payload, e);
        }
    }
}
