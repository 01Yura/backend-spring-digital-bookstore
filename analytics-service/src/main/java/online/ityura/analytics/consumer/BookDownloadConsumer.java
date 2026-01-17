package online.ityura.analytics.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.ityura.analytics.service.AnalyticsService;
import online.ityura.springdigitallibrary.dto.event.BookDownloadEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookDownloadConsumer {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper = new ObjectMapper() {{
        registerModule(new JavaTimeModule());
    }};

    @KafkaListener(
        topics = "book.downloads",
        groupId = "analytics-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBookDownload(@Payload java.util.Map<String, Object> payload) {
        log.info("Received book download event (raw): {}", payload);
        try {
            BookDownloadEvent event = objectMapper.convertValue(payload, BookDownloadEvent.class);
            log.info("Received book download event: {}", event);
            analyticsService.processBookDownload(event);
        } catch (Exception e) {
            log.error("Error processing book download event: {}", payload, e);
        }
    }
}
