package online.ityura.springdigitallibrary.repository;

import jakarta.persistence.LockModeType;
import online.ityura.springdigitallibrary.model.Purchase;
import online.ityura.springdigitallibrary.model.PurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    Optional<Purchase> findByStripePaymentIntentId(String stripePaymentIntentId);
    
    @Query("SELECT COUNT(p) > 0 FROM Purchase p WHERE p.user.id = :userId AND p.book.id = :bookId AND p.status = :status")
    boolean existsByUserIdAndBookIdAndStatus(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("status") PurchaseStatus status);
    
    @Query("SELECT p FROM Purchase p WHERE p.user.id = :userId AND p.book.id = :bookId")
    Optional<Purchase> findByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Purchase p WHERE p.user.id = :userId AND p.book.id = :bookId")
    Optional<Purchase> findByUserIdAndBookIdWithLock(@Param("userId") Long userId, @Param("bookId") Long bookId);
    
    @Query("SELECT COUNT(p) > 0 FROM Purchase p WHERE p.user.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);
}
