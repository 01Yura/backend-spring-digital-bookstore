package online.ityura.analytics.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.ityura.analytics.service.AnalyticsService;
import online.ityura.springdigitallibrary.dto.event.BookRatingEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookRatingConsumer {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper = new ObjectMapper() {{
        registerModule(new JavaTimeModule());
    }};

    @KafkaListener(
        topics = "book.ratings",
        groupId = "analytics-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBookRating(@Payload java.util.Map<String, Object> payload) {
        log.info("Received book rating event (raw): {}", payload);
        try {
            BookRatingEvent event = objectMapper.convertValue(payload, BookRatingEvent.class);
            log.info("Received book rating event: {}", event);
            analyticsService.processBookRating(event);
        } catch (Exception e) {
            log.error("Error processing book rating event: {}", payload, e);
        }
    }
}
