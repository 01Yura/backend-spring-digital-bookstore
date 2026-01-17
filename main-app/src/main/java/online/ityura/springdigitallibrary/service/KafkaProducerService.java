package online.ityura.springdigitallibrary.service;

import lombok.extern.slf4j.Slf4j;
import online.ityura.springdigitallibrary.dto.event.BookViewEvent;
import online.ityura.springdigitallibrary.dto.event.BookDownloadEvent;
import online.ityura.springdigitallibrary.dto.event.BookPurchaseEvent;
import online.ityura.springdigitallibrary.dto.event.BookReviewEvent;
import online.ityura.springdigitallibrary.dto.event.BookRatingEvent;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.InterruptException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final AtomicBoolean kafkaAvailable = new AtomicBoolean(true);
    private static volatile long lastErrorLogTime = 0;
    private static final long ERROR_LOG_INTERVAL_MS = 60000; // Логируем ошибки не чаще раза в минуту

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendBookViewEvent(BookViewEvent event) {
        if (!kafkaAvailable.get()) {
            return; // Тихо игнорируем, если Kafka недоступен
        }
        
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate
                .send("book.views", String.valueOf(event.getBookId()), event);
            future.thenAccept(result -> kafkaAvailable.set(true))
                  .exceptionally(ex -> {
                      handleKafkaError(ex, "book view");
                      return null;
                  });
        } catch (Exception e) {
            handleKafkaError(e, "book view");
        }
    }

    public void sendBookDownloadEvent(BookDownloadEvent event) {
        if (!kafkaAvailable.get()) {
            return;
        }
        
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate
                .send("book.downloads", String.valueOf(event.getUserId()), event);
            future.thenAccept(result -> kafkaAvailable.set(true))
                  .exceptionally(ex -> {
                      handleKafkaError(ex, "book download");
                      return null;
                  });
        } catch (Exception e) {
            handleKafkaError(e, "book download");
        }
    }

    public void sendBookPurchaseEvent(BookPurchaseEvent event) {
        if (!kafkaAvailable.get()) {
            return;
        }
        
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate
                .send("book.purchases", String.valueOf(event.getUserId()), event);
            future.thenAccept(result -> kafkaAvailable.set(true))
                  .exceptionally(ex -> {
                      handleKafkaError(ex, "book purchase");
                      return null;
                  });
        } catch (Exception e) {
            handleKafkaError(e, "book purchase");
        }
    }

    public void sendBookReviewEvent(BookReviewEvent event) {
        if (!kafkaAvailable.get()) {
            return;
        }
        
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate
                .send("book.reviews", String.valueOf(event.getBookId()), event);
            future.thenAccept(result -> kafkaAvailable.set(true))
                  .exceptionally(ex -> {
                      handleKafkaError(ex, "book review");
                      return null;
                  });
        } catch (Exception e) {
            handleKafkaError(e, "book review");
        }
    }

    public void sendBookRatingEvent(BookRatingEvent event) {
        if (!kafkaAvailable.get()) {
            return;
        }
        
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate
                .send("book.ratings", String.valueOf(event.getBookId()), event);
            future.thenAccept(result -> kafkaAvailable.set(true))
                  .exceptionally(ex -> {
                      handleKafkaError(ex, "book rating");
                      return null;
                  });
        } catch (Exception e) {
            handleKafkaError(e, "book rating");
        }
    }
    
    private void handleKafkaError(Throwable ex, String eventType) {
        // Логируем ошибки только периодически, чтобы не засорять логи
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastErrorLogTime > ERROR_LOG_INTERVAL_MS) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof TimeoutException) {
                log.warn("Kafka недоступен. Аналитика временно отключена. События {} будут игнорироваться.", eventType);
            } else if (cause instanceof InterruptException) {
                // Игнорируем прерывания
                return;
            } else {
                log.warn("Ошибка отправки события {} в Kafka: {}. Аналитика временно отключена.", 
                        eventType, cause.getMessage());
            }
            lastErrorLogTime = currentTime;
            kafkaAvailable.set(false);
        }
    }
}
