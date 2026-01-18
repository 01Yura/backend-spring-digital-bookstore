package online.ityura.springdigitallibrary.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import online.ityura.springdigitallibrary.repository.UserRepository;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Фильтр для добавления структурированных полей в логи через MDC.
 * Добавляет следующие поля:
 * - bookId: ID книги из URL
 * - userId: ID пользователя из SecurityContext
 * - requestId: Уникальный ID запроса для трейсинга
 * - requestPath: Путь HTTP запроса
 * - requestMethod: HTTP метод (GET, POST, etc.)
 * - ipAddress: IP адрес клиента (с учетом прокси)
 * - userAgent: Браузер/клиент пользователя
 * - responseTime: Время обработки запроса в миллисекундах
 * - statusCode: HTTP статус код ответа
 * 
 * Все поля автоматически попадут в JSON логи через LogstashEncoder.
 */
@Component
@Order(2) // Выполняется после JwtAuthenticationFilter (который имеет Order по умолчанию)
public class LoggingContextFilter extends OncePerRequestFilter {

    // Паттерн для извлечения bookId из URL
    // Поддерживает: /api/v1/books/123, /api/v1/books/123/download, /api/v1/books/123/reviews и т.д.
    private static final Pattern BOOK_ID_PATTERN = Pattern.compile("/api/v1/books/(\\d+)");
    
    @Autowired(required = false)
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        try {
            // Очищаем MDC перед обработкой запроса
            MDC.clear();
            
            // Генерируем уникальный ID запроса для трейсинга
            String requestId = UUID.randomUUID().toString();
            MDC.put("requestId", requestId);
            
            // Добавляем базовые поля
            MDC.put("requestPath", request.getRequestURI());
            MDC.put("requestMethod", request.getMethod());
            
            // IP адрес клиента
            String ipAddress = getClientIpAddress(request);
            if (ipAddress != null) {
                MDC.put("ipAddress", ipAddress);
            }
            
            // User-Agent (браузер/клиент)
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null && !userAgent.isEmpty()) {
                MDC.put("userAgent", userAgent);
            }
            
            // Извлекаем bookId из URL
            String bookId = extractBookId(request.getRequestURI());
            if (bookId != null) {
                MDC.put("bookId", bookId);
            }
            
            // Извлекаем userId из SecurityContext
            String userId = extractUserId();
            if (userId != null) {
                MDC.put("userId", userId);
            }
            
            // Продолжаем цепочку фильтров
            filterChain.doFilter(request, responseWrapper);
            
            // Вычисляем время обработки запроса
            long responseTime = System.currentTimeMillis() - startTime;
            MDC.put("responseTime", String.valueOf(responseTime));
            
            // HTTP статус код ответа
            int statusCode = responseWrapper.getStatus();
            MDC.put("statusCode", String.valueOf(statusCode));
            
            // Копируем ответ обратно в оригинальный response
            responseWrapper.copyBodyToResponse();
            
        } finally {
            // Очищаем MDC после обработки запроса
            MDC.clear();
        }
    }
    
    /**
     * Извлекает bookId из URL запроса
     * Поддерживает различные форматы:
     * - /api/v1/books/123
     * - /api/v1/books/123/download
     * - /api/v1/books/123/reviews
     */
    private String extractBookId(String requestUri) {
        // Пробуем извлечь из пути
        Matcher matcher = BOOK_ID_PATTERN.matcher(requestUri);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Извлекает userId из SecurityContext
     */
    private String extractUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() 
                    && authentication.getPrincipal() instanceof UserDetails userDetails) {
                String email = userDetails.getUsername();
                if (userRepository != null) {
                    return userRepository.findByEmail(email)
                            .map(user -> String.valueOf(user.getId()))
                            .orElse(null);
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки при извлечении userId
        }
        return null;
    }
    
    /**
     * Извлекает реальный IP адрес клиента с учетом прокси и load balancer
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Проверяем заголовки в порядке приоритета
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For может содержать несколько IP через запятую
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        // Если заголовки не помогли, используем REMOTE_ADDR
        return request.getRemoteAddr();
    }
}
