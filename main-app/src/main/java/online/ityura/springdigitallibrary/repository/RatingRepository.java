package online.ityura.springdigitallibrary.repository;

import online.ityura.springdigitallibrary.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByBookIdAndUserId(Long bookId, Long userId);
    boolean existsByBookIdAndUserId(Long bookId, Long userId);
    
    @Query("SELECT AVG(r.value) FROM Rating r WHERE r.book.id = :bookId")
    Double calculateAverageRating(@Param("bookId") Long bookId);
    
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.book.id = :bookId")
    long countByBookId(@Param("bookId") Long bookId);
}

