package online.ityura.springdigitallibrary.repository;

import online.ityura.springdigitallibrary.model.PasswordResetToken;
import online.ityura.springdigitallibrary.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    Optional<PasswordResetToken> findByUser(User user);
    
    void deleteByUser(User user);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
