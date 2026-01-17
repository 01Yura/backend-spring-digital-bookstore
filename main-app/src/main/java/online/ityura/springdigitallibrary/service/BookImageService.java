package online.ityura.springdigitallibrary.service;

import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.time.Instant;

@Service
public class BookImageService {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Value("${app.images.storage-path}")
    private String storagePath;
    
    @Transactional
    public String uploadBookImage(Long bookId, MultipartFile file) {
        // Проверяем существование книги
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Book not found with id: " + bookId));
        
        // Проверка deletion_locked
        if (book.getDeletionLocked()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Cannot upload image: book modification is locked");
        }
        
        // Проверяем, что файл не пустой
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Image file is required");
        }
        
        try {
            // Создаем директорию если её нет
            Path storageDir = Paths.get(storagePath);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
            
            // Получаем расширение файла из оригинального имени
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String filename;
            Path filePath;
            
            // Проверяем, есть ли у книги уже изображение
            if (book.getImagePath() != null && !book.getImagePath().isEmpty()) {
                // Если есть старое изображение, переименовываем его с timestamp
                Path oldImagePath = Paths.get(book.getImagePath());
                if (Files.exists(oldImagePath)) {
                    // Получаем имя старого файла (без пути)
                    String oldFileName = oldImagePath.getFileName().toString();
                    
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
                    
                    // Если расширение не было в старом файле, используем расширение нового файла
                    if (oldExtension.isEmpty() && !extension.isEmpty()) {
                        oldExtension = extension;
                    }
                    
                    // Переименовываем старое изображение, добавляя timestamp
                    long timestamp = Instant.now().toEpochMilli();
                    String oldFileNameWithTimestamp = oldBaseName + "_" + timestamp + oldExtension;
                    Path oldImagePathWithTimestamp = storageDir.resolve(oldFileNameWithTimestamp);
                    
                    try {
                        Files.move(oldImagePath, oldImagePathWithTimestamp, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        // Логируем ошибку, но не прерываем выполнение
                        System.err.println("Failed to rename old image: " + e.getMessage());
                    }
                    
                    // Новое изображение сохраняем с именем старого
                    filename = oldFileName;
                    filePath = storageDir.resolve(filename);
                } else {
                    // Старое изображение не существует на диске, используем его имя для нового
                    String oldFileName = oldImagePath.getFileName().toString();
                    filename = oldFileName;
                    filePath = storageDir.resolve(filename);
                }
            } else {
                // Если у книги нет изображения, генерируем имя на основе названия книги
                String bookTitle = book.getTitle() != null ? book.getTitle().trim() : "";
                if (bookTitle.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Book title is empty, cannot generate image filename");
                }
                String sanitizedTitle = bookTitle.replaceAll("\\s+", "_");
                
                // Формируем имя файла: название_книги.расширение
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
            String imagePath = filePath.toString();
            
            // Обновляем путь к изображению в базе данных
            book.setImagePath(imagePath);
            bookRepository.save(book);
            
            return imagePath;
            
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to save image: " + e.getMessage());
        }
    }
    
    public Resource getBookImage(Long bookId) {
        // Проверяем существование книги
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Book not found with id: " + bookId));
        
        // Проверяем наличие изображения
        if (book.getImagePath() == null || book.getImagePath().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Image not found for book id: " + bookId);
        }
        
        try {
            Path imagePath = Paths.get(book.getImagePath());
            Resource resource = new UrlResource(imagePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Image file not found or not readable");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error reading image: " + e.getMessage());
        }
    }
    
    public Resource getAllBookImagesAsZip() {
        try {
            // Получаем все книги с изображениями
            List<Book> booksWithImages = bookRepository.findAllWithImages();
            
            if (booksWithImages.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "No books with images found");
            }
            
            // Создаем временный ZIP файл
            Path zipPath = Files.createTempFile("book-images-", ".zip");
            
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                for (Book book : booksWithImages) {
                    if (book.getImagePath() != null && !book.getImagePath().isEmpty()) {
                        try {
                            Path imagePath = Paths.get(book.getImagePath());
                            if (Files.exists(imagePath) && Files.isReadable(imagePath)) {
                                // Получаем имя файла из пути
                                String fileName = imagePath.getFileName().toString();
                                // Используем ID книги и название для уникальности имени в ZIP
                                String zipEntryName = book.getId() + "_" + fileName;
                                
                                ZipEntry zipEntry = new ZipEntry(zipEntryName);
                                zos.putNextEntry(zipEntry);
                                
                                // Копируем содержимое файла в ZIP
                                try (InputStream is = Files.newInputStream(imagePath)) {
                                    byte[] buffer = new byte[8192];
                                    int length;
                                    while ((length = is.read(buffer)) > 0) {
                                        zos.write(buffer, 0, length);
                                    }
                                }
                                
                                zos.closeEntry();
                            }
                        } catch (IOException e) {
                            // Пропускаем файлы, которые не удалось прочитать
                            System.err.println("Failed to add image for book " + book.getId() + ": " + e.getMessage());
                        }
                    }
                }
            }
            
            // Читаем содержимое ZIP файла в байтовый массив
            byte[] zipBytes = Files.readAllBytes(zipPath);
            
            // Удаляем временный файл
            Files.deleteIfExists(zipPath);
            
            // Создаем Resource из байтового массива
            return new ByteArrayResource(zipBytes);
            
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error creating ZIP archive: " + e.getMessage());
        }
    }
}
