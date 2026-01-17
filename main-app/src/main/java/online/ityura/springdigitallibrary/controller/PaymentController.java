package online.ityura.springdigitallibrary.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import online.ityura.springdigitallibrary.dto.response.ErrorResponse;
import online.ityura.springdigitallibrary.repository.UserRepository;
import online.ityura.springdigitallibrary.service.StripeService;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@Tag(name = "Платежи", description = "API для оплаты книг через Stripe")
@RequiredArgsConstructor
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    private final StripeService stripeService;
    private final UserRepository userRepository;
    
    @Value("${stripe.webhook-secret}")
    private String webhookSecret;
    
    @Value("${app.frontend-url:}")
    private String frontendUrl;
    
    @Operation(
            summary = "Создать сессию оплаты для книги",
            description = "Создает Stripe Checkout Session для оплаты книги. Возвращает URL для перенаправления пользователя на страницу оплаты."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Сессия оплаты успешно создана",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\"checkoutUrl\":\"https://checkout.stripe.com/pay/cs_test_...\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Книга уже оплачена или книга бесплатна",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не авторизован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Книга не найдена",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/checkout/{bookId}")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @Parameter(description = "ID книги", example = "1", required = true)
            @PathVariable Long bookId,
            Authentication authentication) {
        
        Long userId = getCurrentUserId(authentication);
        String checkoutUrl = stripeService.createCheckoutSession(bookId, userId);
        
        Map<String, String> response = new HashMap<>();
        if (checkoutUrl == null) {
            // Бесплатная книга
            response.put("message", "Book is free, access granted");
            return ResponseEntity.ok(response);
        }
        
        response.put("checkoutUrl", checkoutUrl);
        return ResponseEntity.ok(response);
    }
    
    @Hidden // Скрыть от Swagger документации - этот endpoint вызывается Stripe, а не разработчиками
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader,
            HttpServletRequest request) {
        
        Event event;
        
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }
        
        // Обрабатываем событие
        logger.info("Received Stripe webhook event: {}", event.getType());
        
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            
            if (session != null) {
                // Используем session ID для поиска покупки, так как мы сохраняем session ID
                String sessionId = session.getId();
                logger.info("Processing checkout.session.completed for sessionId: {}", sessionId);
                try {
                    stripeService.handlePaymentSuccess(sessionId);
                    logger.info("Successfully processed payment for sessionId: {}", sessionId);
                } catch (Exception e) {
                    logger.error("Error processing payment for sessionId: {}", sessionId, e);
                    throw e;
                }
            } else {
                logger.warn("Session object is null in checkout.session.completed event");
            }
        } else if ("payment_intent.succeeded".equals(event.getType())) {
            // Также обрабатываем payment_intent.succeeded как резервный вариант
            com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) 
                    event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (paymentIntent != null) {
                String paymentIntentId = paymentIntent.getId();
                logger.info("Processing payment_intent.succeeded for paymentIntentId: {}", paymentIntentId);
                try {
                    stripeService.handlePaymentSuccess(paymentIntentId);
                    logger.info("Successfully processed payment for paymentIntentId: {}", paymentIntentId);
                } catch (Exception e) {
                    logger.error("Error processing payment for paymentIntentId: {}", paymentIntentId, e);
                    throw e;
                }
            } else {
                logger.warn("PaymentIntent object is null in payment_intent.succeeded event");
            }
        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) 
                    event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (paymentIntent != null) {
                logger.info("Processing payment_intent.payment_failed for paymentIntentId: {}", paymentIntent.getId());
                stripeService.handlePaymentFailure(paymentIntent.getId());
            }
        } else {
            logger.debug("Unhandled webhook event type: {}", event.getType());
        }
        
        return ResponseEntity.ok("Success");
    }
    
    @Operation(
            summary = "Страница успешной оплаты",
            description = "Публичная страница, на которую Stripe редиректит после успешной оплаты. " +
                    "Для браузерных запросов выполняет редирект на страницу книги, для API возвращает JSON."
    )
    @GetMapping("/success")
    public ResponseEntity<?> paymentSuccess(
            @RequestParam(value = "session_id", required = false) String sessionId,
            @RequestParam(value = "book_id", required = false) Long bookId,
            HttpServletRequest request) {
        
        // Безопасная проверка статуса платежа через Stripe API (fallback, если webhook не сработал)
        // ВАЖНО: Платеж считается успешным ТОЛЬКО после успешной проверки через Stripe API
        // Это безопасно, так как мы проверяем реальный статус платежа в Stripe
        // Без sessionId или при ошибке проверки платеж НЕ считается успешным
        boolean paymentVerified = false;
        if (sessionId != null) {
            try {
                logger.info("Verifying payment status via Stripe API for sessionId: {} (fallback if webhook didn't arrive)", sessionId);
                paymentVerified = stripeService.verifyAndCompletePaymentIfNeeded(sessionId);
                if (paymentVerified) {
                    logger.info("Payment verified and purchase status updated via success page for sessionId: {}", sessionId);
                } else {
                    logger.info("Payment not yet completed for sessionId: {}, waiting for webhook", sessionId);
                }
            } catch (Exception e) {
                logger.warn("Could not verify payment status via Stripe API for sessionId: {} - {}. Payment will remain pending until webhook arrives or manual verification succeeds.", sessionId, e.getMessage());
                // Платеж НЕ считается успешным при ошибке проверки - это безопасно
                // Пользователь увидит сообщение о том, что платеж обрабатывается
                paymentVerified = false;
            }
        } else {
            // Без sessionId невозможно проверить платеж - считаем неподтвержденным
            logger.warn("No sessionId provided in success callback. Cannot verify payment status.");
            paymentVerified = false;
        }
        
        // Проверяем, является ли запрос браузерным (по заголовку Accept)
        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        boolean isBrowserRequest = acceptHeader != null && 
                (acceptHeader.contains("text/html") || acceptHeader.contains("*/*"));
        
        // Если это браузерный запрос и есть book_id, делаем редирект на страницу книги
        // Редирект возможен только если указан frontend-url
        // Передаем статус платежа (success или pending) в зависимости от результата проверки
        if (isBrowserRequest && bookId != null && frontendUrl != null && !frontendUrl.isEmpty()) {
            String paymentStatus = paymentVerified ? "success" : "pending";
            String queryParams = sessionId != null 
                    ? "?session_id=" + sessionId + "&payment=" + paymentStatus
                    : "?payment=" + paymentStatus;
            String redirectUrl = frontendUrl + "/books/" + bookId + queryParams;
            
            logger.info("Redirecting to frontend with payment status {}: {}", paymentStatus, redirectUrl);
            
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, redirectUrl)
                    .build();
        }
        
        // Для API-запросов или если frontend-url не указан, возвращаем JSON
        Map<String, String> response = new HashMap<>();
        if (paymentVerified) {
            response.put("status", "success");
            response.put("message", "Payment completed successfully! You can now download the book.");
        } else {
            response.put("status", "pending");
            response.put("message", "Payment is being processed. Please wait for confirmation. You will be able to download the book once payment is confirmed.");
        }
        
        if (isBrowserRequest && (frontendUrl == null || frontendUrl.isEmpty())) {
            // Если это браузерный запрос, но frontend-url не настроен
            response.put("message", paymentVerified 
                    ? "Payment completed successfully! Please configure 'app.frontend-url' in application.properties to enable automatic redirect to the book page."
                    : "Payment is being processed. Please wait for confirmation.");
            response.put("redirectUrl", "/books/" + bookId + (sessionId != null ? "?session_id=" + sessionId + "&payment=" + (paymentVerified ? "success" : "pending") : "?payment=" + (paymentVerified ? "success" : "pending")));
        }
        if (sessionId != null) {
            response.put("session_id", sessionId);
        }
        if (bookId != null) {
            response.put("book_id", bookId.toString());
        }
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Страница отмены оплаты",
            description = "Публичная страница, на которую Stripe редиректит при отмене оплаты. " +
                    "Для браузерных запросов выполняет редирект на страницу книги, для API возвращает JSON."
    )
    @GetMapping("/cancel")
    public ResponseEntity<?> paymentCancel(
            @RequestParam(value = "book_id", required = false) Long bookId,
            HttpServletRequest request) {
        
        // Проверяем, является ли запрос браузерным (по заголовку Accept)
        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        boolean isBrowserRequest = acceptHeader != null && 
                (acceptHeader.contains("text/html") || acceptHeader.contains("*/*"));
        
        // Если это браузерный запрос и есть book_id, делаем редирект на страницу книги
        // Редирект возможен только если указан frontend-url
        if (isBrowserRequest && bookId != null && frontendUrl != null && !frontendUrl.isEmpty()) {
            String redirectUrl = frontendUrl + "/books/" + bookId + "?payment=cancelled";
            
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, redirectUrl)
                    .build();
        }
        
        // Для API-запросов возвращаем JSON
        Map<String, String> response = new HashMap<>();
        response.put("status", "cancelled");
        response.put("message", "Payment was cancelled. You can try again later.");
        if (bookId != null) {
            response.put("book_id", bookId.toString());
        }
        return ResponseEntity.ok(response);
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getId();
    }
}
