package online.ityura.analytics.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.ityura.analytics.service.AnalyticsService;
import online.ityura.springdigitallibrary.dto.event.BookPurchaseEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookPurchaseConsumer {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper = new ObjectMapper() {{
        registerModule(new JavaTimeModule());
    }};

    @KafkaListener(
        topics = "book.purchases",
        groupId = "analytics-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBookPurchase(@Payload java.util.Map<String, Object> payload) {
        log.info("Received book purchase event (raw): {}", payload);
        try {
            BookPurchaseEvent event = objectMapper.convertValue(payload, BookPurchaseEvent.class);
            log.info("Received book purchase event: {}", event);
            analyticsService.processBookPurchase(event);
        } catch (Exception e) {
            log.error("Error processing book purchase event: {}", payload, e);
        }
    }
}
