package online.ityura.analytics.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.ityura.analytics.service.AnalyticsService;
import online.ityura.springdigitallibrary.dto.event.BookViewEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookViewConsumer {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper = new ObjectMapper() {{
        registerModule(new JavaTimeModule());
    }};

    @KafkaListener(
        topics = "book.views",
        groupId = "analytics-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBookView(@Payload java.util.Map<String, Object> payload) {
        log.info("Received book view event (raw): {}", payload);
        try {
            // Преобразуем Map в BookViewEvent через ObjectMapper
            BookViewEvent event = objectMapper.convertValue(payload, BookViewEvent.class);
            log.info("Received book view event: {}", event);
            analyticsService.processBookView(event);
        } catch (Exception e) {
            log.error("Error processing book view event: {}", payload, e);
        }
    }
}
