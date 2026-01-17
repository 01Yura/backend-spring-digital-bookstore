package online.ityura.springdigitallibrary.service;

import online.ityura.springdigitallibrary.model.PasswordResetToken;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.PasswordResetTokenRepository;
import online.ityura.springdigitallibrary.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${app.password.reset.token-expiration-hours:1}")
    private int tokenExpirationHours;
    
    @Transactional
    public void generatePasswordResetToken(String email) {
        // Ищем пользователя по email
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        // Security: не раскрываем информацию о существовании email
        if (userOptional.isEmpty()) {
            logger.info("Password reset requested for non-existent email: {}", email);
            return; // Тихо выходим, не выбрасывая исключение
        }
        
        User user = userOptional.get();
        
        // Удаляем старые токены пользователя
        tokenRepository.deleteByUser(user);
        
        // Генерируем новый уникальный токен
        String token = UUID.randomUUID().toString();
        
        // Создаем новую запись токена
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(tokenExpirationHours))
                .used(false)
                .build();
        
        tokenRepository.save(resetToken);
        
        // Отправляем письмо с токеном
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), token);
            logger.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Ищем токен в БД
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, 
                        "Password reset token not found"
                ));
        
        // Проверяем, не использован ли токен
        if (resetToken.getUsed()) {
            throw new ResponseStatusException(
                    HttpStatus.GONE, 
                    "Password reset token has already been used"
            );
        }
        
        // Проверяем срок действия
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.GONE, 
                    "Password reset token has expired"
            );
        }
        
        // Находим пользователя
        User user = resetToken.getUser();
        
        // Валидация пароля выполняется на уровне DTO через аннотации
        
        // Хешируем новый пароль
        String hashedPassword = passwordEncoder.encode(newPassword);
        
        // Обновляем пароль пользователя
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);
        
        // Помечаем токен как использованный
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        
        logger.info("Password reset successfully for user: {}", user.getEmail());
    }
    
    /**
     * Проверяет валидность токена восстановления пароля без сброса пароля.
     * Используется для GET запроса при переходе по ссылке из письма.
     * 
     * @param token токен восстановления пароля
     * @throws ResponseStatusException если токен не найден, истек или уже использован
     */
    public void validateToken(String token) {
        // Ищем токен в БД
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, 
                        "Password reset token not found"
                ));
        
        // Проверяем, не использован ли токен
        if (resetToken.getUsed()) {
            throw new ResponseStatusException(
                    HttpStatus.GONE, 
                    "Password reset token has already been used"
            );
        }
        
        // Проверяем срок действия
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.GONE, 
                    "Password reset token has expired"
            );
        }
    }
}
