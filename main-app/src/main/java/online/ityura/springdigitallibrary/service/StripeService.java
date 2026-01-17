package online.ityura.springdigitallibrary.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import online.ityura.springdigitallibrary.dto.event.BookPurchaseEvent;
import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.model.Purchase;
import online.ityura.springdigitallibrary.model.PurchaseStatus;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.BookRepository;
import online.ityura.springdigitallibrary.repository.PurchaseRepository;
import online.ityura.springdigitallibrary.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class StripeService {
    
    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PurchaseRepository purchaseRepository;
    
    @Autowired(required = false)
    private KafkaProducerService kafkaProducerService;
    
    @Value("${stripe.secret-key}")
    private String stripeSecretKey;
    
    @Value("${stripe.success-url}")
    private String successUrl;
    
    @Value("${stripe.cancel-url}")
    private String cancelUrl;
    
    @Value("${stripe.webhook-wait-timeout-ms:2000}")
    private long webhookWaitTimeoutMs;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }
    
    /**
     * Создает Stripe Checkout Session для оплаты книги
     */
    @Transactional
    public String createCheckoutSession(Long bookId, Long userId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Book not found with id: " + bookId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "User not found with id: " + userId));
        
        // Проверяем, не оплачена ли уже книга
        if (purchaseRepository.existsByUserIdAndBookIdAndStatus(userId, bookId, PurchaseStatus.COMPLETED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Book already purchased");
        }
        
        // Вычисляем финальную цену с учетом скидки
        BigDecimal finalPrice = calculateFinalPrice(book);
        
        // Если книга бесплатна, создаем покупку сразу
        if (finalPrice.compareTo(BigDecimal.ZERO) == 0) {
            // Используем pessimistic lock для предотвращения race condition
            Optional<Purchase> existingPurchase = purchaseRepository.findByUserIdAndBookIdWithLock(userId, bookId);
            if (existingPurchase.isPresent()) {
                Purchase purchase = existingPurchase.get();
                if (purchase.getStatus() == PurchaseStatus.COMPLETED) {
                    return null; // Уже есть завершенная покупка
                }
                // Обновляем существующую покупку
                purchase.setStatus(PurchaseStatus.COMPLETED);
                purchase.setAmountPaid(BigDecimal.ZERO);
                purchaseRepository.save(purchase);
                return null;
            }
            
            // Создаем новую запись о покупке
            // Обрабатываем возможное исключение дубликата на случай race condition
            try {
                Purchase purchase = Purchase.builder()
                        .user(user)
                        .book(book)
                        .stripePaymentIntentId("free_" + System.currentTimeMillis())
                        .amountPaid(BigDecimal.ZERO)
                        .status(PurchaseStatus.COMPLETED)
                        .build();
                purchaseRepository.save(purchase);
            } catch (DataIntegrityViolationException e) {
                // Если произошла ошибка дубликата, находим существующую покупку и обновляем её
                logger.warn("Duplicate free purchase detected for userId: {}, bookId: {}, attempting to update existing purchase", userId, bookId);
                Optional<Purchase> duplicatePurchase = purchaseRepository.findByUserIdAndBookId(userId, bookId);
                if (duplicatePurchase.isPresent()) {
                    Purchase purchase = duplicatePurchase.get();
                    if (purchase.getStatus() != PurchaseStatus.COMPLETED) {
                        purchase.setStatus(PurchaseStatus.COMPLETED);
                        purchase.setAmountPaid(BigDecimal.ZERO);
                        purchaseRepository.save(purchase);
                    }
                } else {
                    logger.error("DataIntegrityViolationException occurred but purchase not found for userId: {}, bookId: {}", userId, bookId);
                    throw new ResponseStatusException(HttpStatus.CONFLICT, 
                            "Purchase already exists but could not be retrieved");
                }
            }
            return null; // Возвращаем null для бесплатных книг
        }
        
        try {
            // Используем pessimistic lock для предотвращения race condition
            // Это гарантирует, что только один поток может обрабатывать создание покупки
            // для конкретной пары user_id + book_id одновременно
            Optional<Purchase> existingPurchase = purchaseRepository.findByUserIdAndBookIdWithLock(userId, bookId);
            
            // Создаем Checkout Session
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}&book_id=" + bookId)
                    .setCancelUrl(cancelUrl + "?book_id=" + bookId)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd")
                                                    .setUnitAmount(convertToCents(finalPrice))
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(book.getTitle())
                                                                    .setDescription(book.getDescription() != null && !book.getDescription().isEmpty() 
                                                                            ? book.getDescription() 
                                                                            : "Digital book")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("book_id", bookId.toString())
                    .putMetadata("user_id", userId.toString())
                    .build();
            
            Session session = Session.create(params);
            
            // Сохраняем session ID, так как он всегда доступен
            // В webhook мы будем искать покупку по session ID
            String sessionId = session.getId();
            
            // Перепроверяем существующую покупку после создания Session
            // (на случай, если другой поток создал покупку во время создания Session)
            if (!existingPurchase.isPresent()) {
                existingPurchase = purchaseRepository.findByUserIdAndBookId(userId, bookId);
            }
            
            // Если есть существующая покупка (PENDING или FAILED), обновляем её
            if (existingPurchase.isPresent()) {
                Purchase purchase = existingPurchase.get();
                purchase.setStripePaymentIntentId(sessionId);
                purchase.setAmountPaid(finalPrice);
                purchase.setStatus(PurchaseStatus.PENDING);
                purchaseRepository.save(purchase);
            } else {
                // Создаем новую запись о покупке со статусом PENDING
                // Обрабатываем возможное исключение дубликата на случай race condition
                try {
                    Purchase purchase = Purchase.builder()
                            .user(user)
                            .book(book)
                            .stripePaymentIntentId(sessionId) // Сохраняем session ID
                            .amountPaid(finalPrice)
                            .status(PurchaseStatus.PENDING)
                            .build();
                    purchaseRepository.save(purchase);
                } catch (DataIntegrityViolationException e) {
                    // Если произошла ошибка дубликата (race condition все же случилась),
                    // находим существующую покупку и обновляем её
                    logger.warn("Duplicate purchase detected for userId: {}, bookId: {}, attempting to update existing purchase", userId, bookId);
                    Optional<Purchase> duplicatePurchase = purchaseRepository.findByUserIdAndBookId(userId, bookId);
                    if (duplicatePurchase.isPresent()) {
                        Purchase purchase = duplicatePurchase.get();
                        purchase.setStripePaymentIntentId(sessionId);
                        purchase.setAmountPaid(finalPrice);
                        purchase.setStatus(PurchaseStatus.PENDING);
                        purchaseRepository.save(purchase);
                    } else {
                        // Если покупка не найдена, это странно, но пробрасываем исключение
                        logger.error("DataIntegrityViolationException occurred but purchase not found for userId: {}, bookId: {}", userId, bookId);
                        throw new ResponseStatusException(HttpStatus.CONFLICT, 
                                "Purchase already exists but could not be retrieved");
                    }
                }
            }
            
            return session.getUrl();
            
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to create checkout session: " + e.getMessage());
        }
    }
    
    /**
     * Обрабатывает успешную оплату через webhook
     * Принимает session ID или payment intent ID
     */
    @Transactional
    public void handlePaymentSuccess(String stripeId) {
        // Ищем покупку по session ID или payment intent ID
        Optional<Purchase> purchaseOpt = purchaseRepository.findByStripePaymentIntentId(stripeId);
        
        if (purchaseOpt.isEmpty()) {
            logger.error("Purchase not found for stripe ID: {}", stripeId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Purchase not found for stripe ID: " + stripeId);
        }
        
        Purchase purchase = purchaseOpt.get();
        logger.info("Updating purchase status to COMPLETED - userId: {}, bookId: {}, stripeId: {}", 
                purchase.getUser().getId(), purchase.getBook().getId(), stripeId);
        purchase.setStatus(PurchaseStatus.COMPLETED);
        purchaseRepository.save(purchase);
        purchaseRepository.flush(); // Принудительно сохраняем изменения в БД
        logger.info("Purchase status updated successfully");
        
        // Отправка события покупки книги
        try {
            Book book = purchase.getBook();
            User user = purchase.getUser();
            BigDecimal originalPrice = book.getPrice() != null ? book.getPrice() : BigDecimal.ZERO;
            BigDecimal discountPercent = book.getDiscountPercent() != null ? book.getDiscountPercent() : BigDecimal.ZERO;
            
            BookPurchaseEvent event = BookPurchaseEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("BOOK_PURCHASE")
                    .timestamp(LocalDateTime.now())
                    .bookId(book.getId())
                    .userId(user.getId())
                    .bookTitle(book.getTitle())
                    .amountPaid(purchase.getAmountPaid() != null ? purchase.getAmountPaid().doubleValue() : 0.0)
                    .originalPrice(originalPrice.doubleValue())
                    .discountPercent(discountPercent.doubleValue())
                    .stripeSessionId(stripeId)
                    .build();
            
            if (kafkaProducerService != null) {
                kafkaProducerService.sendBookPurchaseEvent(event);
                logger.debug("Sent book purchase event for bookId: {}, userId: {}", book.getId(), user.getId());
            }
        } catch (Exception e) {
            logger.error("Error sending book purchase event", e);
            // Не прерываем выполнение, если отправка события не удалась
        }
    }
    
    /**
     * Обрабатывает неудачную оплату через webhook
     */
    @Transactional
    public void handlePaymentFailure(String paymentIntentId) {
        Optional<Purchase> purchaseOpt = purchaseRepository.findByStripePaymentIntentId(paymentIntentId);
        
        if (purchaseOpt.isPresent()) {
            Purchase purchase = purchaseOpt.get();
            purchase.setStatus(PurchaseStatus.FAILED);
            purchaseRepository.save(purchase);
        }
    }
    
    /**
     * Вычисляет финальную цену с учетом скидки
     */
    private BigDecimal calculateFinalPrice(Book book) {
        BigDecimal price = book.getPrice();
        BigDecimal discountPercent = book.getDiscountPercent();
        
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            return price;
        }
        
        BigDecimal discountAmount = price.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        BigDecimal finalPrice = price.subtract(discountAmount);
        return finalPrice.max(BigDecimal.ZERO);
    }
    
    /**
     * Конвертирует BigDecimal в центы для Stripe (умножает на 100)
     */
    private Long convertToCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }
    
    /**
     * Проверяет, оплачена ли книга пользователем
     */
    public boolean isBookPurchased(Long userId, Long bookId) {
        return purchaseRepository.existsByUserIdAndBookIdAndStatus(userId, bookId, PurchaseStatus.COMPLETED);
    }
    
    /**
     * Проверяет статус платежа через Stripe API и обновляет статус покупки, если платеж успешен.
     * Используется как безопасный fallback, если webhook не пришел.
     * ВАЖНО: Этот метод проверяет статус через Stripe API, поэтому безопасен для вызова из клиента.
     * 
     * Сначала проверяет, не обработан ли уже платеж через webhook (статус COMPLETED в БД).
     * Если не обработан, ждет некоторое время (настраивается через stripe.webhook-wait-timeout-ms),
     * чтобы дать webhook время прийти, затем проверяет еще раз.
     * Только если webhook не обработал платеж, применяет fallback через Stripe API.
     * 
     * @param sessionId ID сессии Stripe Checkout
     * @return true если платеж успешен и статус обновлен, false если платеж еще не завершен
     * @throws ResponseStatusException если сессия не найдена или произошла ошибка
     */
    @Transactional
    public boolean verifyAndCompletePaymentIfNeeded(String sessionId) {
        // Сначала проверяем, не обработан ли уже платеж через webhook
        Optional<Purchase> purchaseOpt = purchaseRepository.findByStripePaymentIntentId(sessionId);
        if (purchaseOpt.isPresent()) {
            Purchase purchase = purchaseOpt.get();
            if (purchase.getStatus() == PurchaseStatus.COMPLETED) {
                logger.debug("Payment already processed via webhook for sessionId: {}", sessionId);
                return true; // Уже обработан через webhook
            }
        }
        
        // Если платеж еще не обработан, ждем немного, чтобы дать webhook время прийти
        // Webhook обычно приходит очень быстро (секунды), но может быть задержка
        long waitTimeoutMs = webhookWaitTimeoutMs;
        if (waitTimeoutMs > 0) {
            try {
                logger.debug("Waiting {} ms for webhook to arrive for sessionId: {}", waitTimeoutMs, sessionId);
                Thread.sleep(waitTimeoutMs);
                
                // Проверяем еще раз после ожидания
                purchaseOpt = purchaseRepository.findByStripePaymentIntentId(sessionId);
                if (purchaseOpt.isPresent()) {
                    Purchase purchase = purchaseOpt.get();
                    if (purchase.getStatus() == PurchaseStatus.COMPLETED) {
                        logger.info("Payment was processed via webhook during wait period for sessionId: {}", sessionId);
                        return true; // Webhook обработал платеж во время ожидания
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for webhook for sessionId: {}", sessionId);
            }
        }
        
        // Если webhook не обработал платеж, проверяем через Stripe API
        try {
            logger.info("Webhook didn't process payment, verifying via Stripe API for sessionId: {}", sessionId);
            // Получаем информацию о сессии из Stripe API
            Session session = Session.retrieve(sessionId);
            
            // Проверяем статус платежа
            if (!"complete".equals(session.getPaymentStatus()) && 
                !"paid".equals(session.getPaymentStatus())) {
                logger.debug("Payment not completed yet for sessionId: {}, status: {}", 
                        sessionId, session.getPaymentStatus());
                return false;
            }
            
            // Если платеж успешен, обновляем статус покупки
            logger.info("Payment verified via Stripe API for sessionId: {}, updating purchase status", sessionId);
            handlePaymentSuccess(sessionId);
            return true;
            
        } catch (StripeException e) {
            logger.error("Error verifying payment status via Stripe API for sessionId: {}", sessionId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to verify payment status: " + e.getMessage());
        }
    }
}

