package online.ityura.springdigitallibrary.repository;

import online.ityura.springdigitallibrary.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookIdOrderByCreatedAtDesc(Long bookId);
    Page<Review> findByBookIdOrderByCreatedAtDesc(Long bookId, Pageable pageable);
    Optional<Review> findByBookIdAndUserId(Long bookId, Long userId);
    boolean existsByBookIdAndUserId(Long bookId, Long userId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.book.id = :bookId")
    long countByBookId(@Param("bookId") Long bookId);
    
    @Query("SELECT r FROM Review r JOIN FETCH r.user JOIN FETCH r.book WHERE r.book.id = :bookId ORDER BY r.createdAt DESC")
    List<Review> findByBookIdWithUserOrderByCreatedAtDesc(@Param("bookId") Long bookId);
    
    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.book.id IN :bookIds ORDER BY r.book.id, r.createdAt DESC")
    List<Review> findByBookIdIn(@Param("bookIds") List<Long> bookIds);
    
    @EntityGraph(attributePaths = {"user", "book"})
    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

