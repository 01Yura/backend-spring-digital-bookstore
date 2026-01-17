package online.ityura.springdigitallibrary.service;

import online.ityura.springdigitallibrary.model.EmailVerificationToken;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.repository.EmailVerificationTokenRepository;
import online.ityura.springdigitallibrary.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EmailVerificationService {
    
    @Autowired
    private EmailVerificationTokenRepository tokenRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Value("${app.email.verification.token-expiration-hours:24}")
    private int tokenExpirationHours;
    
    @Transactional
    public String generateVerificationToken(User user) {
        // Удаляем старые токены пользователя
        tokenRepository.deleteByUser(user);
        
        // Генерируем новый уникальный токен
        String token = UUID.randomUUID().toString();
        
        // Создаем новую запись токена
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(tokenExpirationHours))
                .used(false)
                .build();
        
        tokenRepository.save(verificationToken);
        
        return token;
    }
    
    @Transactional
    public void verifyEmail(String token) {
        // Ищем токен в БД
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, 
                        "Verification token not found"
                ));
        
        // Проверяем, не был ли токен уже использован
        if (verificationToken.getUsed()) {
            throw new ResponseStatusException(
                    HttpStatus.GONE, 
                    "Verification token has already been used"
            );
        }
        
        // Проверяем, не истек ли токен
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.GONE, 
                    "Verification token has expired"
            );
        }
        
        // Находим пользователя
        User user = verificationToken.getUser();
        
        // Устанавливаем isVerified = true
        user.setIsVerified(true);
        userRepository.save(user);
        
        // Помечаем токен как использованный
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);
    }
    
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, 
                        "User not found"
                ));
        
        if (user.getIsVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, 
                    "Email is already verified"
            );
        }
        
        String token = generateVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), token);
    }
}
