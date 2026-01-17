package online.ityura.springdigitallibrary.service;

import online.ityura.springdigitallibrary.dto.request.CreateBookRequest;
import online.ityura.springdigitallibrary.dto.request.PutBookRequest;
import online.ityura.springdigitallibrary.dto.request.UpdateBookRequest;
import online.ityura.springdigitallibrary.dto.response.AuthorResponse;
import online.ityura.springdigitallibrary.dto.response.BookResponse;
import online.ityura.springdigitallibrary.model.Author;
import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.repository.AuthorRepository;
import online.ityura.springdigitallibrary.repository.BookRepository;
import online.ityura.springdigitallibrary.repository.ReviewRepository;
import online.ityura.springdigitallibrary.service.BookImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminBookService {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private AuthorRepository authorRepository;
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private BookImageService bookImageService;
    
    @Autowired
    private BookFileService bookFileService;
    
    @Transactional
    public BookResponse createBook(CreateBookRequest request) {
        // Проверка уникальности (title, author)
        Author author = authorRepository.findByFullName(request.getAuthorName())
                .orElseGet(() -> {
                    Author newAuthor = Author.builder()
                            .fullName(request.getAuthorName())
                            .build();
                    return authorRepository.save(newAuthor);
                });
        
        if (bookRepository.existsByTitleAndAuthorId(request.getTitle(), author.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Book with this title and author already exists");
        }
        
        Book book = Book.builder()
                .title(request.getTitle())
                .author(author)
                .description(request.getDescription())
                .publishedYear(request.getPublishedYear())
                .genre(request.getGenre())
                .price(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO)
                .discountPercent(request.getDiscountPercent() != null ? request.getDiscountPercent() : BigDecimal.ZERO)
                .deletionLocked(false)
                .build();
        
        book = bookRepository.save(book);
        return mapToBookResponse(book);
    }
    
    @Transactional
    public List<BookResponse> createBooks(List<CreateBookRequest> requests) {
        List<BookResponse> responses = new ArrayList<>();
        
        for (CreateBookRequest request : requests) {
            // Проверка уникальности (title, author)
            Author author = authorRepository.findByFullName(request.getAuthorName())
                    .orElseGet(() -> {
                        Author newAuthor = Author.builder()
                                .fullName(request.getAuthorName())
                                .build();
                        return authorRepository.save(newAuthor);
                    });
            
            if (bookRepository.existsByTitleAndAuthorId(request.getTitle(), author.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                        "Book with title '" + request.getTitle() + "' and author '" + request.getAuthorName() + "' already exists");
            }
            
            Book book = Book.builder()
                    .title(request.getTitle())
                    .author(author)
                    .description(request.getDescription())
                    .publishedYear(request.getPublishedYear())
                    .genre(request.getGenre())
                    .price(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO)
                    .discountPercent(request.getDiscountPercent() != null ? request.getDiscountPercent() : BigDecimal.ZERO)
                    .deletionLocked(false)
                    .build();
            
            book = bookRepository.save(book);
            responses.add(mapToBookResponse(book));
        }
        
        return responses;
    }
    
    @Transactional
    public BookResponse updateBook(Long bookId, PutBookRequest request) {
        Book book = bookRepository.findByIdWithAuthor(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Book not found with id: " + bookId));
        
        // Проверка deletion_locked
        if (book.getDeletionLocked()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Cannot update book: modification is locked");
        }
        
        // Находим или создаем автора
        Author author = authorRepository.findByFullName(request.getAuthorName())
                .orElseGet(() -> {
                    Author newAuthor = Author.builder()
                            .fullName(request.getAuthorName())
                            .build();
                    return authorRepository.save(newAuthor);
                });
        
        // Проверяем уникальность title + author (если изменился title или author)
        boolean titleChanged = !request.getTitle().equals(book.getTitle());
        boolean authorChanged = !author.getId().equals(book.getAuthor().getId());
        
        if ((titleChanged || authorChanged) && 
            bookRepository.existsByTitleAndAuthorId(request.getTitle(), author.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Book with this title and author already exists");
        }
        
        // Заменяем все поля целиком (полная замена ресурса по REST стандартам)
        book.setTitle(request.getTitle());
        book.setAuthor(author);
        book.setDescription(request.getDescription());
        book.setPublishedYear(request.getPublishedYear());
        book.setGenre(request.getGenre());
        book.setPrice(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO);
        book.setDiscountPercent(request.getDiscountPercent() != null ? request.getDiscountPercent() : BigDecimal.ZERO);
        
        book = bookRepository.save(book);
        return mapToBookResponse(book);
    }
    
    @Transactional
    public BookResponse patchBook(Long bookId, UpdateBookRequest request, MultipartFile imageFile, MultipartFile pdfFile) {
        Book book = bookRepository.findByIdWithAuthor(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Book not found with id: " + bookId));
        
        // Проверка deletion_locked
        if (book.getDeletionLocked()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Cannot update book: modification is locked");
        }
        
        // Определяем автора для проверки уникальности
        Author author = book.getAuthor();
        if (request.getAuthorName() != null) {
            // Находим или создаем автора
            author = authorRepository.findByFullName(request.getAuthorName())
                    .orElseGet(() -> {
                        Author newAuthor = Author.builder()
                                .fullName(request.getAuthorName())
                                .build();
                        return authorRepository.save(newAuthor);
                    });
        }
        
        // Определяем title для проверки уникальности
        String titleToCheck = request.getTitle() != null ? request.getTitle() : book.getTitle();
        
        // Проверка уникальности если меняется title или author
        boolean titleChanged = request.getTitle() != null && !request.getTitle().equals(book.getTitle());
        boolean authorChanged = request.getAuthorName() != null && !author.getId().equals(book.getAuthor().getId());
        
        if ((titleChanged || authorChanged) && 
            bookRepository.existsByTitleAndAuthorId(titleToCheck, author.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Book with this title and author already exists");
        }
        
        // PATCH делает частичное обновление - обновляем только переданные поля
        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        
        if (request.getAuthorName() != null) {
            book.setAuthor(author);
        }
        
        if (request.getDescription() != null) {
            book.setDescription(request.getDescription());
        }
        if (request.getPublishedYear() != null) {
            book.setPublishedYear(request.getPublishedYear());
        }
        if (request.getGenre() != null) {
            book.setGenre(request.getGenre());
        }
        
        if (request.getPrice() != null) {
            book.setPrice(request.getPrice());
        }
        
        if (request.getDiscountPercent() != null) {
            book.setDiscountPercent(request.getDiscountPercent());
        }
        
        book = bookRepository.save(book);
        
        // Если передан файл изображения, обновляем изображение
        if (imageFile != null && !imageFile.isEmpty()) {
            bookImageService.uploadBookImage(bookId, imageFile);
        }
        
        // Если передан PDF файл, обновляем PDF
        if (pdfFile != null && !pdfFile.isEmpty()) {
            bookFileService.uploadBookPdf(bookId, pdfFile);
        }
        
        // Если были обновлены файлы, получаем обновленную книгу для возврата
        if ((imageFile != null && !imageFile.isEmpty()) || (pdfFile != null && !pdfFile.isEmpty())) {
            book = bookRepository.findByIdWithAuthor(bookId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Book not found with id: " + bookId));
        }
        
        return mapToBookResponse(book);
    }
    
    @Transactional
    public void deleteBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Book not found with id: " + bookId));
        
        // Проверка deletion_locked
        if (book.getDeletionLocked()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Cannot delete book: deletion is locked");
        }
        
        // Проверка наличия связанных сущностей (можно сделать каскадное удаление или запрет)
        // Для безопасности - запрещаем удаление если есть отзывы
        if (reviewRepository.countByBookId(bookId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Cannot delete book: it has reviews");
        }
        
        // Примечание: PDF файл хранится в файловой системе, 
        // его можно удалить физически, но для простоты оставляем как есть
        // (файл будет оставаться в файловой системе, но ссылка в БД удалится вместе с книгой)
        
        bookRepository.delete(book);
    }
    
    @Transactional
    public void deleteAuthorAndAllBooks(Long authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Author not found with id: " + authorId));
        
        // Находим все книги автора
        List<Book> books = bookRepository.findByAuthorId(authorId);
        
        // Для каждой книги проверяем ограничения и удаляем
        for (Book book : books) {
            Long bookId = book.getId();
            
            // Проверка deletion_locked
            if (book.getDeletionLocked()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                        "Cannot delete book with id " + bookId + ": deletion is locked");
            }
            
            // Проверка наличия связанных отзывов
            if (reviewRepository.countByBookId(bookId) > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                        "Cannot delete book with id " + bookId + ": it has reviews");
            }
            
            // Примечание: PDF файл хранится в файловой системе, 
            // его можно удалить физически, но для простоты оставляем как есть
            // (файл будет оставаться в файловой системе, но ссылка в БД удалится вместе с книгой)
            
            // Удаляем книгу через репозиторий (соблюдает все проверки)
            bookRepository.delete(book);
        }
        
        // Удаляем автора
        authorRepository.delete(author);
    }
    
    private BookResponse mapToBookResponse(Book book) {
        boolean hasFile = book.getPdfPath() != null && !book.getPdfPath().isEmpty();
        
        // Вычисляем финальную цену с учетом скидки
        BigDecimal finalPrice = calculateFinalPrice(book);
        
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(AuthorResponse.builder()
                        .id(book.getAuthor().getId())
                        .fullName(book.getAuthor().getFullName())
                        .build())
                .description(book.getDescription())
                .publishedYear(book.getPublishedYear())
                .genre(book.getGenre())
                .ratingAvg(book.getRatingAvg())
                .ratingCount(book.getRatingCount())
                .hasFile(hasFile)
                .imagePath(book.getImagePath())
                .price(book.getPrice())
                .discountPercent(book.getDiscountPercent())
                .finalPrice(finalPrice)
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }
    
    /**
     * Вычисляет финальную цену с учетом скидки
     */
    private BigDecimal calculateFinalPrice(Book book) {
        BigDecimal price = book.getPrice();
        BigDecimal discountPercent = book.getDiscountPercent();
        
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            return price;
        }
        
        BigDecimal discountAmount = price.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        BigDecimal finalPrice = price.subtract(discountAmount);
        return finalPrice.max(BigDecimal.ZERO);
    }
}

