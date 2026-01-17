package online.ityura.springdigitallibrary.repository;

import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.model.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByTitleAndAuthorId(String title, Long authorId);
    boolean existsByTitleAndAuthorId(String title, Long authorId);
    
    @Query("SELECT b FROM Book b JOIN FETCH b.author WHERE b.id = :id")
    Optional<Book> findByIdWithAuthor(@Param("id") Long id);
    
    @Query("SELECT b FROM Book b WHERE b.author.id = :authorId")
    List<Book> findByAuthorId(@Param("authorId") Long authorId);
    
    @Query("SELECT b FROM Book b WHERE b.imagePath IS NOT NULL AND b.imagePath != ''")
    List<Book> findAllWithImages();
    
    Page<Book> findByGenre(Genre genre, Pageable pageable);
}

