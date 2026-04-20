package online.ityura.springdigitallibrary.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.ityura.springdigitallibrary.dto.response.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Старый вариант с безопасным алгоритмом хеширования паролей BCrypt:
         return new BCryptPasswordEncoder();
        // Вариант БЕЗ хеширования, пароли хранятся и сравниваются в открытом виде (только для локальных тестов, не
        // использовать в продакшене):
//        return NoOpPasswordEncoder.getInstance();
        // Используем MD5 для хеширования паролей
//        return new Md5PasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * Same JSON shape as {@link online.ityura.springdigitallibrary.exception.GlobalExceptionHandler} (ErrorResponse).
     */
    private static void writeErrorResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus httpStatus,
            String errorCode,
            ObjectMapper objectMapper) throws IOException {
        ErrorResponse body = ErrorResponse.builder()
                .status(httpStatus.value())
                .error(errorCode)
                .message(httpStatus.getReasonPhrase())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        response.setStatus(httpStatus.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/health").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/v1/kuberinfo").permitAll()
                .requestMatchers("/api/v1/payment/webhook").permitAll()
                .requestMatchers("/api/v1/payment/success").permitAll()
                .requestMatchers("/api/v1/payment/cancel").permitAll()
                .requestMatchers(
                    "/swagger-ui/**", 
                    "/v3/api-docs/**", 
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/swagger-ui/index.html"
                ).permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // Публичные эндпоинты для книг (более специфичные правила идут первыми)
                .requestMatchers("/api/v1/books").permitAll()
                .requestMatchers("/api/v1/books/images/all").permitAll()
                // Эндпоинты изображений - публичные (паттерн для /api/v1/books/{id}/image)
                .requestMatchers("/api/v1/books/*/image").permitAll()
                // GET запросы к списку отзывов - публичные
                .requestMatchers(HttpMethod.GET, "/api/v1/books/*/reviews").permitAll()
                // Эндпоинты, требующие авторизацию (используем * вместо ** в середине)
                .requestMatchers("/api/v1/books/*/download").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/v1/books/*/ratings/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/v1/books/*/reviews/**").hasAnyRole("USER", "ADMIN")
                // Эндпоинты сообщений читателям - требуют авторизацию
                .requestMatchers("/api/v1/books/*/message/**").hasAnyRole("USER", "ADMIN")
                // Эндпоинты службы поддержки - требуют авторизацию
                .requestMatchers("/api/v1/support/**").hasAnyRole("USER", "ADMIN")
                // Эндпоинты платежей - требуют авторизацию (кроме webhook)
                .requestMatchers("/api/v1/payment/**").hasAnyRole("USER", "ADMIN")
                // Остальные эндпоинты книг (например, /api/v1/books/{id}) - публичные
                .requestMatchers("/api/v1/books/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) ->
                        writeErrorResponse(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", objectMapper))
                .accessDeniedHandler((HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) -> {
                    // Если пользователь не авторизован, возвращаем 401, иначе 403
                    if (SecurityContextHolder.getContext().getAuthentication() == null
                            || !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                            || "anonymousUser".equals(
                                    SecurityContextHolder.getContext().getAuthentication().getPrincipal())) {
                        writeErrorResponse(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", objectMapper);
                    } else {
                        writeErrorResponse(request, response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", objectMapper);
                    }
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        // LoggingContextFilter автоматически регистрируется через @Component
        // и выполняется после JwtAuthenticationFilter благодаря @Order(2)
        
        return http.build();
    }
}

