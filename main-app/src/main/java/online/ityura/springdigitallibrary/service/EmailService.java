package online.ityura.springdigitallibrary.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${app.email.verification.base-url}")
    private String baseUrl;
    
    @Value("${spring.mail.properties.mail.smtp.from:noreply@localhost}")
    private String fromEmail;
    
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Подтвердите ваш email");
            
            String verificationUrl = baseUrl + "/verify-email?token=" + verificationToken;
            
            String htmlContent = buildVerificationEmailHtml(toEmail, verificationUrl);
            String textContent = buildVerificationEmailText(verificationUrl);
            
            helper.setText(textContent, htmlContent);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
    
    private String buildVerificationEmailHtml(String email, String verificationUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".button { display: inline-block; padding: 12px 24px; background-color: #007bff; color: #ffffff; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
                ".button:hover { background-color: #0056b3; }" +
                ".footer { margin-top: 30px; font-size: 12px; color: #666; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<h2>Подтвердите ваш email</h2>" +
                "<p>Здравствуйте!</p>" +
                "<p>Спасибо за регистрацию в нашей системе. Для завершения регистрации, пожалуйста, подтвердите ваш email адрес, перейдя по ссылке ниже:</p>" +
                "<p><a href='" + verificationUrl + "' class='button'>Подтвердить email</a></p>" +
                "<p>Или скопируйте и вставьте следующую ссылку в ваш браузер:</p>" +
                "<p style='word-break: break-all; color: #007bff;'>" + verificationUrl + "</p>" +
                "<p><strong>Важно:</strong> Ссылка действительна в течение 24 часов.</p>" +
                "<p>Если вы не регистрировались в нашей системе, пожалуйста, проигнорируйте это письмо.</p>" +
                "<div class='footer'>" +
                "<p>С уважением,<br>Команда Spring Digital Bookstore</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    
    private String buildVerificationEmailText(String verificationUrl) {
        return "Здравствуйте!\n\n" +
                "Спасибо за регистрацию в нашей системе. Для завершения регистрации, пожалуйста, подтвердите ваш email адрес, перейдя по следующей ссылке:\n\n" +
                verificationUrl + "\n\n" +
                "Важно: Ссылка действительна в течение 24 часов.\n\n" +
                "Если вы не регистрировались в нашей системе, пожалуйста, проигнорируйте это письмо.\n\n" +
                "С уважением,\n" +
                "Команда Spring Digital Bookstore";
    }
    
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Восстановление пароля");
            
            String resetUrl = baseUrl + "/reset-password?token=" + resetToken;
            
            String htmlContent = buildPasswordResetEmailHtml(toEmail, resetUrl);
            String textContent = buildPasswordResetEmailText(resetUrl);
            
            helper.setText(textContent, htmlContent);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    private String buildPasswordResetEmailHtml(String email, String resetUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".button { display: inline-block; padding: 12px 24px; background-color: #007bff; color: #ffffff; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
                ".button:hover { background-color: #0056b3; }" +
                ".footer { margin-top: 30px; font-size: 12px; color: #666; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<h2>Восстановление пароля</h2>" +
                "<p>Здравствуйте!</p>" +
                "<p>Мы получили запрос на восстановление пароля для вашего аккаунта. Если это были вы, пожалуйста, перейдите по ссылке ниже для сброса пароля:</p>" +
                "<p><a href='" + resetUrl + "' class='button'>Сбросить пароль</a></p>" +
                "<p>Или скопируйте и вставьте следующую ссылку в ваш браузер:</p>" +
                "<p style='word-break: break-all; color: #007bff;'>" + resetUrl + "</p>" +
                "<p><strong>Важно:</strong> Ссылка действительна в течение 1 часа.</p>" +
                "<p>Если вы не запрашивали восстановление пароля, пожалуйста, проигнорируйте это письмо. Ваш пароль останется без изменений.</p>" +
                "<p>После сброса пароля вы сможете войти в систему с новым паролем.</p>" +
                "<div class='footer'>" +
                "<p>С уважением,<br>Команда Spring Digital Bookstore</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    
    private String buildPasswordResetEmailText(String resetUrl) {
        return "Здравствуйте!\n\n" +
                "Мы получили запрос на восстановление пароля для вашего аккаунта. Если это были вы, пожалуйста, перейдите по следующей ссылке для сброса пароля:\n\n" +
                resetUrl + "\n\n" +
                "Важно: Ссылка действительна в течение 1 часа.\n\n" +
                "Если вы не запрашивали восстановление пароля, пожалуйста, проигнорируйте это письмо. Ваш пароль останется без изменений.\n\n" +
                "После сброса пароля вы сможете войти в систему с новым паролем.\n\n" +
                "С уважением,\n" +
                "Команда Spring Digital Bookstore";
    }
}
