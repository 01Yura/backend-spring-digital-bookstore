package online.ityura.springdigitallibrary.repository;

import online.ityura.springdigitallibrary.model.EmailVerificationToken;
import online.ityura.springdigitallibrary.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    
    Optional<EmailVerificationToken> findByToken(String token);
    
    Optional<EmailVerificationToken> findByUser(User user);
    
    void deleteByUser(User user);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
