package online.ityura.springdigitallibrary.service;

import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

@Service
public class BookFileService {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private StripeService stripeService;
    
    @Value("${app.pdf.storage-path}")
    private String storagePath;
    
    public Resource downloadBookFile(Long bookId, Long userId) {
        // Получаем книгу из базы данных
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Book not found with id: " + bookId));
        
        // Проверяем, есть ли PDF файл у книги
        if (book.getPdfPath() == null || book.getPdfPath().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "PDF file not found for book id: " + bookId);
        }
        
        // Проверяем, нужно ли платить за книгу
        BigDecimal finalPrice = calculateFinalPrice(book);
        if (finalPrice.compareTo(BigDecimal.ZERO) > 0) {
            // Книга платная, проверяем оплату
            if (!stripeService.isBookPurchased(userId, bookId)) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, 
                        "Book must be purchased before download. Please complete payment first.");
            }
        }
        
        try {
            Path filePath = Paths.get(book.getPdfPath());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "PDF file not found or not readable at path: " + book.getPdfPath());
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error reading PDF file: " + e.getMessage());
        }
    }
    
    public String getOriginalFilename(Long bookId) {
        // Получаем книгу из базы данных
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Book not found with id: " + bookId));
        
        // Проверяем, есть ли PDF файл у книги
        if (book.getPdfPath() == null || book.getPdfPath().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "PDF file not found for book id: " + bookId);
        }
        
        // Извлекаем имя файла из пути
        Path filePath = Paths.get(book.getPdfPath());
        String filename = filePath.getFileName().toString();
        
        // Если имя файла не найдено, формируем на основе названия книги
        if (filename == null || filename.isEmpty()) {
            filename = book.getTitle()
                    .replaceAll("\\s+", "_")
                    .replaceAll("[<>:\"|?*]", "") + ".pdf";
        }
        
        return filename;
    }
    
    @Transactional
    public String uploadBookPdf(Long bookId, MultipartFile file) {
        // Проверяем существование книги
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Book not found with id: " + bookId));
        
        // Проверка deletion_locked
        if (book.getDeletionLocked()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Cannot upload PDF: book modification is locked");
        }
        
        // Проверяем, что файл не пустой
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "PDF file is required");
        }
        
        // Проверяем, что файл является PDF
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "File must be a PDF (application/pdf)");
        }
        
        try {
            // Создаем директорию если её нет
            Path storageDir = Paths.get(storagePath);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
            
            // Получаем расширение файла из оригинального имени
            String originalFilename = file.getOriginalFilename();
            String extension = ".pdf";
            if (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf")) {
                extension = ".pdf";
            }
            
            String filename;
            Path filePath;
            
            // Проверяем, есть ли у книги уже PDF файл
            if (book.getPdfPath() != null && !book.getPdfPath().isEmpty()) {
                // Если есть старый PDF, переименовываем его с timestamp
                Path oldPdfPath = Paths.get(book.getPdfPath());
                if (Files.exists(oldPdfPath)) {
                    // Получаем имя старого файла (без пути)
                    String oldFileName = oldPdfPath.getFileName().toString();
                    
                    // Извлекаем базовое имя и расширение из старого файла
                    String oldBaseName;
                    String oldExtension = "";
                    if (oldFileName.contains(".")) {
                        int lastDotIndex = oldFileName.lastIndexOf(".");
                        oldBaseName = oldFileName.substring(0, lastDotIndex);
                        oldExtension = oldFileName.substring(lastDotIndex);
                    } else {
                        oldBaseName = oldFileName;
                    }
                    
                    // Если расширение не было в старом файле, используем .pdf
                    if (oldExtension.isEmpty()) {
                        oldExtension = ".pdf";
                    }
                    
                    // Переименовываем старый PDF, добавляя timestamp
                    long timestamp = Instant.now().toEpochMilli();
                    String oldFileNameWithTimestamp = oldBaseName + "_" + timestamp + oldExtension;
                    Path oldPdfPathWithTimestamp = storageDir.resolve(oldFileNameWithTimestamp);
                    
                    try {
                        Files.move(oldPdfPath, oldPdfPathWithTimestamp, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        // Логируем ошибку, но не прерываем выполнение
                        System.err.println("Failed to rename old PDF: " + e.getMessage());
                    }
                    
                    // Новый PDF сохраняем с именем старого
                    filename = oldFileName;
                    filePath = storageDir.resolve(filename);
                } else {
                    // Старый PDF не существует на диске, используем его имя для нового
                    String oldFileName = oldPdfPath.getFileName().toString();
                    filename = oldFileName;
                    filePath = storageDir.resolve(filename);
                }
            } else {
                // Если у книги нет PDF, генерируем имя на основе названия книги
                String bookTitle = book.getTitle() != null ? book.getTitle().trim() : "";
                if (bookTitle.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Book title is empty, cannot generate PDF filename");
                }
                String sanitizedTitle = bookTitle.replaceAll("\\s+", "_")
                        .replaceAll("[<>:\"|?*]", "");
                
                // Формируем имя файла: название_книги.pdf
                filename = sanitizedTitle + extension;
                filePath = storageDir.resolve(filename);
                
                // Если файл с таким именем уже существует, добавляем UUID
                if (Files.exists(filePath)) {
                    String baseName = sanitizedTitle;
                    filename = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
                    filePath = storageDir.resolve(filename);
                }
            }
            
            // Сохраняем новый файл
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Формируем полный путь для сохранения в БД
            String pdfPath = filePath.toString();
            
            // Обновляем путь к PDF в базе данных
            book.setPdfPath(pdfPath);
            bookRepository.save(book);
            
            return pdfPath;
            
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to save PDF: " + e.getMessage());
        }
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
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        
        BigDecimal finalPrice = price.subtract(discountAmount);
        return finalPrice.max(BigDecimal.ZERO);
    }
}

