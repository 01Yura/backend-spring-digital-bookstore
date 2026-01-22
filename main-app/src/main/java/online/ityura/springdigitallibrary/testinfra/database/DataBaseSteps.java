package online.ityura.springdigitallibrary.testinfra.database;

import online.ityura.springdigitallibrary.model.Author;
import online.ityura.springdigitallibrary.model.Book;
import online.ityura.springdigitallibrary.model.BookAnalytics;
import online.ityura.springdigitallibrary.model.EmailVerificationToken;
import online.ityura.springdigitallibrary.model.PasswordResetToken;
import online.ityura.springdigitallibrary.model.Purchase;
import online.ityura.springdigitallibrary.model.PurchaseStatus;
import online.ityura.springdigitallibrary.model.Rating;
import online.ityura.springdigitallibrary.model.Review;
import online.ityura.springdigitallibrary.model.Role;
import online.ityura.springdigitallibrary.model.SystemAnalytics;
import online.ityura.springdigitallibrary.model.User;
import online.ityura.springdigitallibrary.testinfra.configs.Config;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * DataBaseSteps - Удобный класс для работы с базой данных в тестах
 * 
 * Предоставляет статические методы для выполнения типичных операций с БД:
 * - Получение данных (SELECT)
 * - Обновление данных (UPDATE)
 * - Проверка существования записей
 * 
 * Все методы логируют свои действия через StepLogger.
 * 
 * @author Generated
 * @version 1.0
 */
public class DataBaseSteps {

    // Чтобы не писать "магические строки" с именами таблиц
    public enum Table {
        USERS("users"),
        BOOKS("books"),
        AUTHORS("authors"),
        REVIEWS("reviews"),
        RATINGS("ratings"),
        PURCHASES("purchases"),
        EMAIL_VERIFICATION_TOKENS("email_verification_tokens"),
        PASSWORD_RESET_TOKENS("password_reset_tokens"),
        BOOK_ANALYTICS("book_analytics"),
        SYSTEM_ANALYTICS("system_analytics");

        private final String table;

        Table(String table) { 
            this.table = table; 
        }
        
        public String getTable() { 
            return table; 
        }
    }

    /* ======================= USERS ======================= */

    /**
     * Получает пользователя из базы данных по email
     * @param email email пользователя
     * @return объект User или null, если не найден
     */
    public static User getUserByEmail(String email) {
        return StepLogger.log(
            "Get user from database by email: " + email,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.USERS.getTable())
                    .where(Condition.equalTo("email", email))
                    .extractAs(User.class)
        );
    }

    /**
     * Получает пользователя из базы данных по nickname
     * @param nickname nickname пользователя
     * @return объект User или null, если не найден
     */
    public static User getUserByNickname(String nickname) {
        return StepLogger.log(
            "Get user from database by nickname: " + nickname,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.USERS.getTable())
                    .where(Condition.equalTo("nickname", nickname))
                    .extractAs(User.class)
        );
    }

    /**
     * Получает пользователя из базы данных по ID
     * @param id ID пользователя
     * @return объект User или null, если не найден
     */
    public static User getUserById(Long id) {
        return StepLogger.log(
            "Get user from database by ID: " + id,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.USERS.getTable())
                    .where(Condition.equalTo("id", id))
                    .extractAs(User.class)
        );
    }

    /**
     * Получает пользователя из базы данных по роли
     * @param role роль пользователя
     * @return объект User или null, если не найден
     */
    public static User getUserByRole(String role) {
        return StepLogger.log(
            "Get user from database by role: " + role,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.USERS.getTable())
                    .where(Condition.equalTo("role", role))
                    .extractAs(User.class)
        );
    }

    /**
     * Получает всех пользователей из базы данных
     * @return список всех пользователей
     */
    public static List<User> getAllUsers() {
        return StepLogger.log(
            "Get all users from database",
            () -> {
                String sql = "SELECT * FROM " + Table.USERS.getTable();
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    
                    List<User> users = new ArrayList<>();
                    while (resultSet.next()) {
                        Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                        LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                        
                        String roleStr = resultSet.getString("role");
                        Role role = roleStr != null ? Role.valueOf(roleStr) : null;
                        
                        users.add(User.builder()
                                .id(resultSet.getLong("id"))
                                .nickname(resultSet.getString("nickname"))
                                .email(resultSet.getString("email"))
                                .passwordHash(resultSet.getString("password_hash"))
                                .role(role)
                                .createdAt(createdAt)
                                .build());
                    }
                    return users;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get all users", e);
                }
            }
        );
    }

    /* ===================== BOOKS ====================== */

    /**
     * Получает книгу из базы данных по ID
     * @param id ID книги
     * @return объект Book или null, если не найдена
     */
    public static Book getBookById(Long id) {
        return StepLogger.log(
            "Get book from database by ID: " + id,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.BOOKS.getTable())
                    .where(Condition.equalTo("id", id))
                    .extractAs(Book.class)
        );
    }

    /**
     * Получает книгу из базы данных по названию
     * @param title название книги
     * @return объект Book или null, если не найдена
     */
    public static Book getBookByTitle(String title) {
        return StepLogger.log(
            "Get book from database by title: " + title,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.BOOKS.getTable())
                    .where(Condition.equalTo("title", title))
                    .extractAs(Book.class)
        );
    }

    /**
     * Получает книгу из базы данных по ID автора
     * @param authorId ID автора
     * @return объект Book или null, если не найдена
     */
    public static Book getBookByAuthorId(Long authorId) {
        return StepLogger.log(
            "Get book from database by author ID: " + authorId,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.BOOKS.getTable())
                    .where(Condition.equalTo("author_id", authorId))
                    .extractAs(Book.class)
        );
    }

    /**
     * Получает все книги из базы данных
     * @return список всех книг
     */
    public static List<Book> getAllBooks() {
        return StepLogger.log(
            "Get all books from database",
            () -> {
                String sql = "SELECT * FROM " + Table.BOOKS.getTable();
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    
                    List<Book> books = new ArrayList<>();
                    while (resultSet.next()) {
                        Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                        LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                        
                        Timestamp updatedAtTimestamp = resultSet.getTimestamp("updated_at");
                        LocalDateTime updatedAt = updatedAtTimestamp != null ? updatedAtTimestamp.toLocalDateTime() : null;
                        
                        String genreStr = resultSet.getString("genre");
                        online.ityura.springdigitallibrary.model.Genre genre = null;
                        if (genreStr != null) {
                            try {
                                genre = online.ityura.springdigitallibrary.model.Genre.valueOf(genreStr);
                            } catch (IllegalArgumentException e) {
                                // Если значение не найдено в enum, оставляем null
                            }
                        }
                        
                        Long authorId = resultSet.getLong("author_id");
                        Author author = null;
                        if (!resultSet.wasNull() && authorId != null) {
                            author = Author.builder().id(authorId).build();
                        }
                        
                        books.add(Book.builder()
                                .id(resultSet.getLong("id"))
                                .title(resultSet.getString("title"))
                                .author(author)
                                .description(resultSet.getString("description"))
                                .publishedYear(resultSet.getObject("published_year", Integer.class))
                                .genre(genre)
                                .deletionLocked(resultSet.getBoolean("deletion_locked"))
                                .ratingAvg(resultSet.getBigDecimal("rating_avg"))
                                .ratingCount(resultSet.getInt("rating_count"))
                                .imagePath(resultSet.getString("image_path"))
                                .pdfPath(resultSet.getString("pdf_path"))
                                .createdAt(createdAt)
                                .updatedAt(updatedAt)
                                .build());
                    }
                    return books;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get all books", e);
                }
            }
        );
    }

    /**
     * Получает все книги конкретного автора
     * @param authorId ID автора
     * @return список книг автора
     */
    public static List<Book> getBooksByAuthorId(Long authorId) {
        return StepLogger.log(
            "Get all books for author ID: " + authorId,
            () -> {
                String sql = "SELECT * FROM " + Table.BOOKS.getTable() + " WHERE author_id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, authorId);
                    List<Book> books = new ArrayList<>();
                    
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                            LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                            
                            Timestamp updatedAtTimestamp = resultSet.getTimestamp("updated_at");
                            LocalDateTime updatedAt = updatedAtTimestamp != null ? updatedAtTimestamp.toLocalDateTime() : null;
                            
                            String genreStr = resultSet.getString("genre");
                            online.ityura.springdigitallibrary.model.Genre genre = null;
                            if (genreStr != null) {
                                try {
                                    genre = online.ityura.springdigitallibrary.model.Genre.valueOf(genreStr);
                                } catch (IllegalArgumentException e) {
                                    // Если значение не найдено в enum, оставляем null
                                }
                            }
                            
                            Long authorIdFromDb = resultSet.getLong("author_id");
                            Author author = null;
                            if (!resultSet.wasNull() && authorIdFromDb != null) {
                                author = Author.builder().id(authorIdFromDb).build();
                            }
                            
                            books.add(Book.builder()
                                    .id(resultSet.getLong("id"))
                                    .title(resultSet.getString("title"))
                                    .author(author)
                                    .description(resultSet.getString("description"))
                                    .publishedYear(resultSet.getObject("published_year", Integer.class))
                                    .genre(genre)
                                    .deletionLocked(resultSet.getBoolean("deletion_locked"))
                                    .ratingAvg(resultSet.getBigDecimal("rating_avg"))
                                    .ratingCount(resultSet.getInt("rating_count"))
                                    .imagePath(resultSet.getString("image_path"))
                                    .pdfPath(resultSet.getString("pdf_path"))
                                    .createdAt(createdAt)
                                    .updatedAt(updatedAt)
                                    .build());
                        }
                    }
                    return books;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get books for author ID: " + authorId, e);
                }
            }
        );
    }

    /* ===================== AUTHORS ====================== */

    /**
     * Получает автора из базы данных по ID
     * @param id ID автора
     * @return объект Author или null, если не найден
     */
    public static Author getAuthorById(Long id) {
        return StepLogger.log(
            "Get author from database by ID: " + id,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.AUTHORS.getTable())
                    .where(Condition.equalTo("id", id))
                    .extractAs(Author.class)
        );
    }

    /**
     * Получает автора из базы данных по полному имени
     * @param fullName полное имя автора
     * @return объект Author или null, если не найден
     */
    public static Author getAuthorByFullName(String fullName) {
        return StepLogger.log(
            "Get author from database by full name: " + fullName,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.AUTHORS.getTable())
                    .where(Condition.equalTo("full_name", fullName))
                    .extractAs(Author.class)
        );
    }

    /**
     * Получает всех авторов из базы данных
     * @return список всех авторов
     */
    public static List<Author> getAllAuthors() {
        return StepLogger.log(
            "Get all authors from database",
            () -> {
                String sql = "SELECT * FROM " + Table.AUTHORS.getTable();
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    
                    List<Author> authors = new ArrayList<>();
                    while (resultSet.next()) {
                        Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                        LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                        
                        authors.add(Author.builder()
                                .id(resultSet.getLong("id"))
                                .fullName(resultSet.getString("full_name"))
                                .createdAt(createdAt)
                                .build());
                    }
                    return authors;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get all authors", e);
                }
            }
        );
    }

    /* =================== REVIEWS =================== */

    /**
     * Получает отзыв из базы данных по ID
     * @param id ID отзыва
     * @return объект Review или null, если не найден
     */
    public static Review getReviewById(Long id) {
        return StepLogger.log(
            "Get review from database by ID: " + id,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.REVIEWS.getTable())
                    .where(Condition.equalTo("id", id))
                    .extractAs(Review.class)
        );
    }

    /**
     * Получает все отзывы из базы данных
     * @return список всех отзывов
     */
    public static List<Review> getAllReviews() {
        return StepLogger.log(
            "Get all reviews from database",
            () -> {
                String sql = "SELECT * FROM " + Table.REVIEWS.getTable();
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    
                    List<Review> reviews = new ArrayList<>();
                    while (resultSet.next()) {
                        Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                        LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                        
                        Timestamp updatedAtTimestamp = resultSet.getTimestamp("updated_at");
                        LocalDateTime updatedAt = updatedAtTimestamp != null ? updatedAtTimestamp.toLocalDateTime() : null;
                        
                        Long bookId = resultSet.getLong("book_id");
                        Book book = null;
                        if (!resultSet.wasNull() && bookId != null) {
                            book = Book.builder().id(bookId).build();
                        }
                        
                        Long userId = resultSet.getLong("user_id");
                        User user = null;
                        if (!resultSet.wasNull() && userId != null) {
                            user = User.builder().id(userId).build();
                        }
                        
                        reviews.add(Review.builder()
                                .id(resultSet.getLong("id"))
                                .book(book)
                                .user(user)
                                .text(resultSet.getString("text"))
                                .createdAt(createdAt)
                                .updatedAt(updatedAt)
                                .build());
                    }
                    return reviews;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get all reviews", e);
                }
            }
        );
    }

    /**
     * Получает отзывы по ID книги
     * @param bookId ID книги
     * @return список отзывов книги
     */
    public static List<Review> getReviewsByBookId(Long bookId) {
        return StepLogger.log(
            "Get reviews for book ID: " + bookId,
            () -> {
                String sql = "SELECT * FROM " + Table.REVIEWS.getTable() + " WHERE book_id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, bookId);
                    List<Review> reviews = new ArrayList<>();
                    
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                            LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                            
                            Timestamp updatedAtTimestamp = resultSet.getTimestamp("updated_at");
                            LocalDateTime updatedAt = updatedAtTimestamp != null ? updatedAtTimestamp.toLocalDateTime() : null;
                            
                            Long bookIdFromDb = resultSet.getLong("book_id");
                            Book book = null;
                            if (!resultSet.wasNull() && bookIdFromDb != null) {
                                book = Book.builder().id(bookIdFromDb).build();
                            }
                            
                            Long userId = resultSet.getLong("user_id");
                            User user = null;
                            if (!resultSet.wasNull() && userId != null) {
                                user = User.builder().id(userId).build();
                            }
                            
                            reviews.add(Review.builder()
                                    .id(resultSet.getLong("id"))
                                    .book(book)
                                    .user(user)
                                    .text(resultSet.getString("text"))
                                    .createdAt(createdAt)
                                    .updatedAt(updatedAt)
                                    .build());
                        }
                    }
                    return reviews;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get reviews for book ID: " + bookId, e);
                }
            }
        );
    }

    /**
     * Получает отзывы по ID пользователя
     * @param userId ID пользователя
     * @return список отзывов пользователя
     */
    public static List<Review> getReviewsByUserId(Long userId) {
        return StepLogger.log(
            "Get reviews for user ID: " + userId,
            () -> {
                String sql = "SELECT * FROM " + Table.REVIEWS.getTable() + " WHERE user_id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, userId);
                    List<Review> reviews = new ArrayList<>();
                    
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                            LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                            
                            Timestamp updatedAtTimestamp = resultSet.getTimestamp("updated_at");
                            LocalDateTime updatedAt = updatedAtTimestamp != null ? updatedAtTimestamp.toLocalDateTime() : null;
                            
                            Long bookId = resultSet.getLong("book_id");
                            Book book = null;
                            if (!resultSet.wasNull() && bookId != null) {
                                book = Book.builder().id(bookId).build();
                            }
                            
                            Long userIdFromDb = resultSet.getLong("user_id");
                            User user = null;
                            if (!resultSet.wasNull() && userIdFromDb != null) {
                                user = User.builder().id(userIdFromDb).build();
                            }
                            
                            reviews.add(Review.builder()
                                    .id(resultSet.getLong("id"))
                                    .book(book)
                                    .user(user)
                                    .text(resultSet.getString("text"))
                                    .createdAt(createdAt)
                                    .updatedAt(updatedAt)
                                    .build());
                        }
                    }
                    return reviews;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get reviews for user ID: " + userId, e);
                }
            }
        );
    }

    /* =================== RATINGS =================== */

    /**
     * Получает оценку из базы данных по ID
     * @param id ID оценки
     * @return объект Rating или null, если не найден
     */
    public static Rating getRatingById(Long id) {
        return StepLogger.log(
            "Get rating from database by ID: " + id,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.RATINGS.getTable())
                    .where(Condition.equalTo("id", id))
                    .extractAs(Rating.class)
        );
    }

    /**
     * Получает все оценки из базы данных
     * @return список всех оценок
     */
    public static List<Rating> getAllRatings() {
        return StepLogger.log(
            "Get all ratings from database",
            () -> {
                String sql = "SELECT * FROM " + Table.RATINGS.getTable();
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    
                    List<Rating> ratings = new ArrayList<>();
                    while (resultSet.next()) {
                        Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                        LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                        
                        Timestamp updatedAtTimestamp = resultSet.getTimestamp("updated_at");
                        LocalDateTime updatedAt = updatedAtTimestamp != null ? updatedAtTimestamp.toLocalDateTime() : null;
                        
                        Long bookId = resultSet.getLong("book_id");
                        Book book = null;
                        if (!resultSet.wasNull() && bookId != null) {
                            book = Book.builder().id(bookId).build();
                        }
                        
                        Long userId = resultSet.getLong("user_id");
                        User user = null;
                        if (!resultSet.wasNull() && userId != null) {
                            user = User.builder().id(userId).build();
                        }
                        
                        ratings.add(Rating.builder()
                                .id(resultSet.getLong("id"))
                                .book(book)
                                .user(user)
                                .value(resultSet.getShort("value"))
                                .createdAt(createdAt)
                                .updatedAt(updatedAt)
                                .build());
                    }
                    return ratings;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get all ratings", e);
                }
            }
        );
    }

    /**
     * Получает оценки по ID книги
     * @param bookId ID книги
     * @return список оценок книги
     */
    public static List<Rating> getRatingsByBookId(Long bookId) {
        return StepLogger.log(
            "Get ratings for book ID: " + bookId,
            () -> {
                String sql = "SELECT * FROM " + Table.RATINGS.getTable() + " WHERE book_id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, bookId);
                    List<Rating> ratings = new ArrayList<>();
                    
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                            LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                            
                            Timestamp updatedAtTimestamp = resultSet.getTimestamp("updated_at");
                            LocalDateTime updatedAt = updatedAtTimestamp != null ? updatedAtTimestamp.toLocalDateTime() : null;
                            
                            Long bookIdFromDb = resultSet.getLong("book_id");
                            Book book = null;
                            if (!resultSet.wasNull() && bookIdFromDb != null) {
                                book = Book.builder().id(bookIdFromDb).build();
                            }
                            
                            Long userId = resultSet.getLong("user_id");
                            User user = null;
                            if (!resultSet.wasNull() && userId != null) {
                                user = User.builder().id(userId).build();
                            }
                            
                            ratings.add(Rating.builder()
                                    .id(resultSet.getLong("id"))
                                    .book(book)
                                    .user(user)
                                    .value(resultSet.getShort("value"))
                                    .createdAt(createdAt)
                                    .updatedAt(updatedAt)
                                    .build());
                        }
                    }
                    return ratings;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get ratings for book ID: " + bookId, e);
                }
            }
        );
    }

    /**
     * Получает оценки по ID пользователя
     * @param userId ID пользователя
     * @return список оценок пользователя
     */
    public static List<Rating> getRatingsByUserId(Long userId) {
        return StepLogger.log(
            "Get ratings for user ID: " + userId,
            () -> {
                String sql = "SELECT * FROM " + Table.RATINGS.getTable() + " WHERE user_id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, userId);
                    List<Rating> ratings = new ArrayList<>();
                    
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                            LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                            
                            Timestamp updatedAtTimestamp = resultSet.getTimestamp("updated_at");
                            LocalDateTime updatedAt = updatedAtTimestamp != null ? updatedAtTimestamp.toLocalDateTime() : null;
                            
                            Long bookId = resultSet.getLong("book_id");
                            Book book = null;
                            if (!resultSet.wasNull() && bookId != null) {
                                book = Book.builder().id(bookId).build();
                            }
                            
                            Long userIdFromDb = resultSet.getLong("user_id");
                            User user = null;
                            if (!resultSet.wasNull() && userIdFromDb != null) {
                                user = User.builder().id(userIdFromDb).build();
                            }
                            
                            ratings.add(Rating.builder()
                                    .id(resultSet.getLong("id"))
                                    .book(book)
                                    .user(user)
                                    .value(resultSet.getShort("value"))
                                    .createdAt(createdAt)
                                    .updatedAt(updatedAt)
                                    .build());
                        }
                    }
                    return ratings;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get ratings for user ID: " + userId, e);
                }
            }
        );
    }

    /* =================== UPDATE METHODS =================== */

    /**
     * Обновляет nickname пользователя в базе данных
     * @param userId ID пользователя
     * @param newNickname новый nickname
     * @return количество обновлённых строк
     */
    public static int updateUserNickname(Long userId, String newNickname) {
        return StepLogger.log(
            "Update user nickname in database for user ID: " + userId + " to: " + newNickname,
            () -> {
                String sql = "UPDATE " + Table.USERS.getTable() + " SET nickname = ? WHERE id = ?";

                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {

                    statement.setString(1, newNickname);
                    statement.setLong(2, userId);
                    return statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to update nickname for userId=" + userId, e);
                }
            }
        );
    }

    /**
     * Обновляет email пользователя в базе данных
     * @param userId ID пользователя
     * @param newEmail новый email
     * @return количество обновлённых строк
     */
    public static int updateUserEmail(Long userId, String newEmail) {
        return StepLogger.log(
            "Update user email in database for user ID: " + userId + " to: " + newEmail,
            () -> {
                String sql = "UPDATE " + Table.USERS.getTable() + " SET email = ? WHERE id = ?";

                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {

                    statement.setString(1, newEmail);
                    statement.setLong(2, userId);
                    return statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to update email for userId=" + userId, e);
                }
            }
        );
    }

    /**
     * Обновляет роль пользователя в базе данных
     * @param userId ID пользователя
     * @param newRole новая роль
     * @return количество обновлённых строк
     */
    public static int updateUserRole(Long userId, String newRole) {
        return StepLogger.log(
            "Update user role in database for user ID: " + userId + " to: " + newRole,
            () -> {
                String sql = "UPDATE " + Table.USERS.getTable() + " SET role = ? WHERE id = ?";

                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {

                    statement.setString(1, newRole);
                    statement.setLong(2, userId);
                    return statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to update role for userId=" + userId, e);
                }
            }
        );
    }

    /**
     * Устанавливает статус верификации email пользователя в базе данных
     * @param userId ID пользователя
     * @return количество обновлённых строк
     */
    public static int setUserVerified(Long userId) {
        return StepLogger.log(
            "Set user as verified in database for user ID: " + userId,
            () -> {
                String sql = "UPDATE " + Table.USERS.getTable() + " SET is_verified = true WHERE id = ?";

                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {

                    statement.setLong(1, userId);
                    return statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to set user as verified for userId=" + userId, e);
                }
            }
        );
    }

    /**
     * Обновляет средний рейтинг книги в базе данных
     * @param bookId ID книги
     * @param newRatingAvg новый средний рейтинг
     * @return количество обновлённых строк
     */
    public static int updateBookRatingAvg(Long bookId, BigDecimal newRatingAvg) {
        return StepLogger.log(
            "Update book rating average in database for book ID: " + bookId + " to: " + newRatingAvg,
            () -> {
                String sql = "UPDATE " + Table.BOOKS.getTable() + " SET rating_avg = ? WHERE id = ?";

                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {

                    statement.setBigDecimal(1, newRatingAvg);
                    statement.setLong(2, bookId);
                    return statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to update rating_avg for bookId=" + bookId, e);
                }
            }
        );
    }

    /**
     * Обновляет количество оценок книги в базе данных
     * @param bookId ID книги
     * @param newRatingCount новое количество оценок
     * @return количество обновлённых строк
     */
    public static int updateBookRatingCount(Long bookId, Integer newRatingCount) {
        return StepLogger.log(
            "Update book rating count in database for book ID: " + bookId + " to: " + newRatingCount,
            () -> {
                String sql = "UPDATE " + Table.BOOKS.getTable() + " SET rating_count = ? WHERE id = ?";

                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {

                    statement.setInt(1, newRatingCount);
                    statement.setLong(2, bookId);
                    return statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to update rating_count for bookId=" + bookId, e);
                }
            }
        );
    }

    /**
     * Обновляет текст отзыва в базе данных
     * @param reviewId ID отзыва
     * @param newText новый текст отзыва
     * @return количество обновлённых строк
     */
    public static int updateReviewText(Long reviewId, String newText) {
        return StepLogger.log(
            "Update review text in database for review ID: " + reviewId + " to: " + newText,
            () -> {
                String sql = "UPDATE " + Table.REVIEWS.getTable() + " SET text = ? WHERE id = ?";

                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {

                    statement.setString(1, newText);
                    statement.setLong(2, reviewId);
                    return statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to update text for reviewId=" + reviewId, e);
                }
            }
        );
    }

    /**
     * Обновляет значение оценки в базе данных
     * @param ratingId ID оценки
     * @param newValue новое значение оценки
     * @return количество обновлённых строк
     */
    public static int updateRatingValue(Long ratingId, Short newValue) {
        return StepLogger.log(
            "Update rating value in database for rating ID: " + ratingId + " to: " + newValue,
            () -> {
                String sql = "UPDATE " + Table.RATINGS.getTable() + " SET value = ? WHERE id = ?";

                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {

                    statement.setShort(1, newValue);
                    statement.setLong(2, ratingId);
                    return statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to update value for ratingId=" + ratingId, e);
                }
            }
        );
    }

    /**
     * Обновляет статус покупки в базе данных
     * @param purchaseId ID покупки
     * @param newStatus новый статус покупки
     * @return количество обновлённых строк
     */
    public static int updatePurchaseStatus(Long purchaseId, PurchaseStatus newStatus) {
        return StepLogger.log(
            "Update purchase status in database for purchase ID: " + purchaseId + " to: " + newStatus,
            () -> {
                String sql = "UPDATE " + Table.PURCHASES.getTable() + " SET status = ? WHERE id = ?";

                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {

                    statement.setString(1, newStatus != null ? newStatus.name() : null);
                    statement.setLong(2, purchaseId);
                    return statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to update status for purchaseId=" + purchaseId, e);
                }
            }
        );
    }

    /**
     * Устанавливает токен верификации email как использованный в базе данных
     * @param tokenId ID токена
     * @return количество обновлённых строк
     */
    public static int setEmailVerificationTokenAsUsed(Long tokenId) {
        return StepLogger.log(
            "Set email verification token as used in database for token ID: " + tokenId,
            () -> {
                String sql = "UPDATE " + Table.EMAIL_VERIFICATION_TOKENS.getTable() + " SET used = true WHERE id = ?";

                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {

                    statement.setLong(1, tokenId);
                    return statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to set email verification token as used for tokenId=" + tokenId, e);
                }
            }
        );
    }

    /**
     * Устанавливает токен сброса пароля как использованный в базе данных
     * @param tokenId ID токена
     * @return количество обновлённых строк
     */
    public static int setPasswordResetTokenAsUsed(Long tokenId) {
        return StepLogger.log(
            "Set password reset token as used in database for token ID: " + tokenId,
            () -> {
                String sql = "UPDATE " + Table.PASSWORD_RESET_TOKENS.getTable() + " SET used = true WHERE id = ?";

                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {

                    statement.setLong(1, tokenId);
                    return statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to set password reset token as used for tokenId=" + tokenId, e);
                }
            }
        );
    }

    /* =================== PURCHASES =================== */

    /**
     * Получает покупку из базы данных по ID
     * @param id ID покупки
     * @return объект Purchase или null, если не найдена
     */
    public static Purchase getPurchaseById(Long id) {
        return StepLogger.log(
            "Get purchase from database by ID: " + id,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.PURCHASES.getTable())
                    .where(Condition.equalTo("id", id))
                    .extractAs(Purchase.class)
        );
    }

    /**
     * Получает все покупки из базы данных
     * @return список всех покупок
     */
    public static List<Purchase> getAllPurchases() {
        return StepLogger.log(
            "Get all purchases from database",
            () -> {
                String sql = "SELECT * FROM " + Table.PURCHASES.getTable();
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    
                    List<Purchase> purchases = new ArrayList<>();
                    while (resultSet.next()) {
                        Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                        LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                        
                        Timestamp updatedAtTimestamp = resultSet.getTimestamp("updated_at");
                        LocalDateTime updatedAt = updatedAtTimestamp != null ? updatedAtTimestamp.toLocalDateTime() : null;
                        
                        Long bookId = resultSet.getLong("book_id");
                        Book book = null;
                        if (!resultSet.wasNull() && bookId != null) {
                            book = Book.builder().id(bookId).build();
                        }
                        
                        Long userId = resultSet.getLong("user_id");
                        User user = null;
                        if (!resultSet.wasNull() && userId != null) {
                            user = User.builder().id(userId).build();
                        }
                        
                        String statusStr = resultSet.getString("status");
                        PurchaseStatus status = null;
                        if (statusStr != null) {
                            try {
                                status = PurchaseStatus.valueOf(statusStr);
                            } catch (IllegalArgumentException e) {
                                // Если значение не найдено в enum, оставляем null
                            }
                        }
                        
                        purchases.add(Purchase.builder()
                                .id(resultSet.getLong("id"))
                                .user(user)
                                .book(book)
                                .stripePaymentIntentId(resultSet.getString("stripe_payment_intent_id"))
                                .amountPaid(resultSet.getBigDecimal("amount_paid"))
                                .status(status)
                                .createdAt(createdAt)
                                .updatedAt(updatedAt)
                                .build());
                    }
                    return purchases;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get all purchases", e);
                }
            }
        );
    }

    /**
     * Получает покупки по ID пользователя
     * @param userId ID пользователя
     * @return список покупок пользователя
     */
    public static List<Purchase> getPurchasesByUserId(Long userId) {
        return StepLogger.log(
            "Get purchases for user ID: " + userId,
            () -> {
                String sql = "SELECT * FROM " + Table.PURCHASES.getTable() + " WHERE user_id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, userId);
                    List<Purchase> purchases = new ArrayList<>();
                    
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                            LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                            
                            Timestamp updatedAtTimestamp = resultSet.getTimestamp("updated_at");
                            LocalDateTime updatedAt = updatedAtTimestamp != null ? updatedAtTimestamp.toLocalDateTime() : null;
                            
                            Long bookId = resultSet.getLong("book_id");
                            Book book = null;
                            if (!resultSet.wasNull() && bookId != null) {
                                book = Book.builder().id(bookId).build();
                            }
                            
                            Long userIdFromDb = resultSet.getLong("user_id");
                            User user = null;
                            if (!resultSet.wasNull() && userIdFromDb != null) {
                                user = User.builder().id(userIdFromDb).build();
                            }
                            
                            String statusStr = resultSet.getString("status");
                            PurchaseStatus status = null;
                            if (statusStr != null) {
                                try {
                                    status = PurchaseStatus.valueOf(statusStr);
                                } catch (IllegalArgumentException e) {
                                    // Если значение не найдено в enum, оставляем null
                                }
                            }
                            
                            purchases.add(Purchase.builder()
                                    .id(resultSet.getLong("id"))
                                    .user(user)
                                    .book(book)
                                    .stripePaymentIntentId(resultSet.getString("stripe_payment_intent_id"))
                                    .amountPaid(resultSet.getBigDecimal("amount_paid"))
                                    .status(status)
                                    .createdAt(createdAt)
                                    .updatedAt(updatedAt)
                                    .build());
                        }
                    }
                    return purchases;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get purchases for user ID: " + userId, e);
                }
            }
        );
    }

    /**
     * Получает покупки по ID книги
     * @param bookId ID книги
     * @return список покупок книги
     */
    public static List<Purchase> getPurchasesByBookId(Long bookId) {
        return StepLogger.log(
            "Get purchases for book ID: " + bookId,
            () -> {
                String sql = "SELECT * FROM " + Table.PURCHASES.getTable() + " WHERE book_id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, bookId);
                    List<Purchase> purchases = new ArrayList<>();
                    
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                            LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                            
                            Timestamp updatedAtTimestamp = resultSet.getTimestamp("updated_at");
                            LocalDateTime updatedAt = updatedAtTimestamp != null ? updatedAtTimestamp.toLocalDateTime() : null;
                            
                            Long bookIdFromDb = resultSet.getLong("book_id");
                            Book book = null;
                            if (!resultSet.wasNull() && bookIdFromDb != null) {
                                book = Book.builder().id(bookIdFromDb).build();
                            }
                            
                            Long userId = resultSet.getLong("user_id");
                            User user = null;
                            if (!resultSet.wasNull() && userId != null) {
                                user = User.builder().id(userId).build();
                            }
                            
                            String statusStr = resultSet.getString("status");
                            PurchaseStatus status = null;
                            if (statusStr != null) {
                                try {
                                    status = PurchaseStatus.valueOf(statusStr);
                                } catch (IllegalArgumentException e) {
                                    // Если значение не найдено в enum, оставляем null
                                }
                            }
                            
                            purchases.add(Purchase.builder()
                                    .id(resultSet.getLong("id"))
                                    .user(user)
                                    .book(book)
                                    .stripePaymentIntentId(resultSet.getString("stripe_payment_intent_id"))
                                    .amountPaid(resultSet.getBigDecimal("amount_paid"))
                                    .status(status)
                                    .createdAt(createdAt)
                                    .updatedAt(updatedAt)
                                    .build());
                        }
                    }
                    return purchases;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get purchases for book ID: " + bookId, e);
                }
            }
        );
    }

    /* =================== EMAIL VERIFICATION TOKENS =================== */

    /**
     * Получает токен верификации email из базы данных по токену
     * @param token токен верификации
     * @return объект EmailVerificationToken или null, если не найден
     */
    public static EmailVerificationToken getEmailVerificationTokenByToken(String token) {
        return StepLogger.log(
            "Get email verification token from database by token: " + token,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.EMAIL_VERIFICATION_TOKENS.getTable())
                    .where(Condition.equalTo("token", token))
                    .extractAs(EmailVerificationToken.class)
        );
    }

    /**
     * Получает токен верификации email из базы данных по ID
     * @param id ID токена
     * @return объект EmailVerificationToken или null, если не найден
     */
    public static EmailVerificationToken getEmailVerificationTokenById(Long id) {
        return StepLogger.log(
            "Get email verification token from database by ID: " + id,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.EMAIL_VERIFICATION_TOKENS.getTable())
                    .where(Condition.equalTo("id", id))
                    .extractAs(EmailVerificationToken.class)
        );
    }

    /**
     * Получает все токены верификации email из базы данных
     * @return список всех токенов
     */
    public static List<EmailVerificationToken> getAllEmailVerificationTokens() {
        return StepLogger.log(
            "Get all email verification tokens from database",
            () -> {
                String sql = "SELECT * FROM " + Table.EMAIL_VERIFICATION_TOKENS.getTable();
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    
                    List<EmailVerificationToken> tokens = new ArrayList<>();
                    while (resultSet.next()) {
                        Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                        LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                        
                        Timestamp expiresAtTimestamp = resultSet.getTimestamp("expires_at");
                        LocalDateTime expiresAt = expiresAtTimestamp != null ? expiresAtTimestamp.toLocalDateTime() : null;
                        
                        Long userId = resultSet.getLong("user_id");
                        User user = null;
                        if (!resultSet.wasNull() && userId != null) {
                            user = User.builder().id(userId).build();
                        }
                        
                        tokens.add(EmailVerificationToken.builder()
                                .id(resultSet.getLong("id"))
                                .token(resultSet.getString("token"))
                                .user(user)
                                .expiresAt(expiresAt)
                                .createdAt(createdAt)
                                .used(resultSet.getBoolean("used"))
                                .build());
                    }
                    return tokens;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get all email verification tokens", e);
                }
            }
        );
    }

    /**
     * Получает токены верификации email по ID пользователя
     * @param userId ID пользователя
     * @return список токенов пользователя
     */
    public static List<EmailVerificationToken> getEmailVerificationTokensByUserId(Long userId) {
        return StepLogger.log(
            "Get email verification tokens for user ID: " + userId,
            () -> {
                String sql = "SELECT * FROM " + Table.EMAIL_VERIFICATION_TOKENS.getTable() + " WHERE user_id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, userId);
                    List<EmailVerificationToken> tokens = new ArrayList<>();
                    
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                            LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                            
                            Timestamp expiresAtTimestamp = resultSet.getTimestamp("expires_at");
                            LocalDateTime expiresAt = expiresAtTimestamp != null ? expiresAtTimestamp.toLocalDateTime() : null;
                            
                            Long userIdFromDb = resultSet.getLong("user_id");
                            User user = null;
                            if (!resultSet.wasNull() && userIdFromDb != null) {
                                user = User.builder().id(userIdFromDb).build();
                            }
                            
                            tokens.add(EmailVerificationToken.builder()
                                    .id(resultSet.getLong("id"))
                                    .token(resultSet.getString("token"))
                                    .user(user)
                                    .expiresAt(expiresAt)
                                    .createdAt(createdAt)
                                    .used(resultSet.getBoolean("used"))
                                    .build());
                        }
                    }
                    return tokens;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get email verification tokens for user ID: " + userId, e);
                }
            }
        );
    }

    /* =================== PASSWORD RESET TOKENS =================== */

    /**
     * Получает токен сброса пароля из базы данных по токену
     * @param token токен сброса пароля
     * @return объект PasswordResetToken или null, если не найден
     */
    public static PasswordResetToken getPasswordResetTokenByToken(String token) {
        return StepLogger.log(
            "Get password reset token from database by token: " + token,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.PASSWORD_RESET_TOKENS.getTable())
                    .where(Condition.equalTo("token", token))
                    .extractAs(PasswordResetToken.class)
        );
    }

    /**
     * Получает токен сброса пароля из базы данных по ID
     * @param id ID токена
     * @return объект PasswordResetToken или null, если не найден
     */
    public static PasswordResetToken getPasswordResetTokenById(Long id) {
        return StepLogger.log(
            "Get password reset token from database by ID: " + id,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.PASSWORD_RESET_TOKENS.getTable())
                    .where(Condition.equalTo("id", id))
                    .extractAs(PasswordResetToken.class)
        );
    }

    /**
     * Получает все токены сброса пароля из базы данных
     * @return список всех токенов
     */
    public static List<PasswordResetToken> getAllPasswordResetTokens() {
        return StepLogger.log(
            "Get all password reset tokens from database",
            () -> {
                String sql = "SELECT * FROM " + Table.PASSWORD_RESET_TOKENS.getTable();
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    
                    List<PasswordResetToken> tokens = new ArrayList<>();
                    while (resultSet.next()) {
                        Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                        LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                        
                        Timestamp expiresAtTimestamp = resultSet.getTimestamp("expires_at");
                        LocalDateTime expiresAt = expiresAtTimestamp != null ? expiresAtTimestamp.toLocalDateTime() : null;
                        
                        Long userId = resultSet.getLong("user_id");
                        User user = null;
                        if (!resultSet.wasNull() && userId != null) {
                            user = User.builder().id(userId).build();
                        }
                        
                        tokens.add(PasswordResetToken.builder()
                                .id(resultSet.getLong("id"))
                                .token(resultSet.getString("token"))
                                .user(user)
                                .expiresAt(expiresAt)
                                .createdAt(createdAt)
                                .used(resultSet.getBoolean("used"))
                                .build());
                    }
                    return tokens;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get all password reset tokens", e);
                }
            }
        );
    }

    /**
     * Получает токены сброса пароля по ID пользователя
     * @param userId ID пользователя
     * @return список токенов пользователя
     */
    public static List<PasswordResetToken> getPasswordResetTokensByUserId(Long userId) {
        return StepLogger.log(
            "Get password reset tokens for user ID: " + userId,
            () -> {
                String sql = "SELECT * FROM " + Table.PASSWORD_RESET_TOKENS.getTable() + " WHERE user_id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, userId);
                    List<PasswordResetToken> tokens = new ArrayList<>();
                    
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                            LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                            
                            Timestamp expiresAtTimestamp = resultSet.getTimestamp("expires_at");
                            LocalDateTime expiresAt = expiresAtTimestamp != null ? expiresAtTimestamp.toLocalDateTime() : null;
                            
                            Long userIdFromDb = resultSet.getLong("user_id");
                            User user = null;
                            if (!resultSet.wasNull() && userIdFromDb != null) {
                                user = User.builder().id(userIdFromDb).build();
                            }
                            
                            tokens.add(PasswordResetToken.builder()
                                    .id(resultSet.getLong("id"))
                                    .token(resultSet.getString("token"))
                                    .user(user)
                                    .expiresAt(expiresAt)
                                    .createdAt(createdAt)
                                    .used(resultSet.getBoolean("used"))
                                    .build());
                        }
                    }
                    return tokens;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get password reset tokens for user ID: " + userId, e);
                }
            }
        );
    }

    /* =================== BOOK ANALYTICS =================== */

    /**
     * Получает аналитику книги из базы данных по ID
     * @param id ID аналитики
     * @return объект BookAnalytics или null, если не найдена
     */
    public static BookAnalytics getBookAnalyticsById(Long id) {
        return StepLogger.log(
            "Get book analytics from database by ID: " + id,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.BOOK_ANALYTICS.getTable())
                    .where(Condition.equalTo("id", id))
                    .extractAs(BookAnalytics.class)
        );
    }

    /**
     * Получает аналитику книги из базы данных по ID книги
     * @param bookId ID книги
     * @return объект BookAnalytics или null, если не найдена
     */
    public static BookAnalytics getBookAnalyticsByBookId(Long bookId) {
        return StepLogger.log(
            "Get book analytics from database by book ID: " + bookId,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.BOOK_ANALYTICS.getTable())
                    .where(Condition.equalTo("book_id", bookId))
                    .extractAs(BookAnalytics.class)
        );
    }

    /**
     * Получает всю аналитику книг из базы данных
     * @return список всей аналитики книг
     */
    public static List<BookAnalytics> getAllBookAnalytics() {
        return StepLogger.log(
            "Get all book analytics from database",
            () -> {
                String sql = "SELECT * FROM " + Table.BOOK_ANALYTICS.getTable();
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    
                    List<BookAnalytics> analytics = new ArrayList<>();
                    while (resultSet.next()) {
                        Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                        LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                        
                        Timestamp aggregatedAtTimestamp = resultSet.getTimestamp("aggregated_at");
                        LocalDateTime aggregatedAt = aggregatedAtTimestamp != null ? aggregatedAtTimestamp.toLocalDateTime() : null;
                        
                        analytics.add(BookAnalytics.builder()
                                .id(resultSet.getLong("id"))
                                .bookId(resultSet.getLong("book_id"))
                                .bookTitle(resultSet.getString("book_title"))
                                .bookGenre(resultSet.getString("book_genre"))
                                .viewCount(resultSet.getObject("view_count", Long.class))
                                .downloadCount(resultSet.getObject("download_count", Long.class))
                                .purchaseCount(resultSet.getObject("purchase_count", Long.class))
                                .reviewCount(resultSet.getObject("review_count", Long.class))
                                .ratingCount(resultSet.getObject("rating_count", Long.class))
                                .averageRating(resultSet.getBigDecimal("average_rating"))
                                .totalRevenue(resultSet.getBigDecimal("total_revenue"))
                                .uniqueViewers(resultSet.getObject("unique_viewers", Integer.class))
                                .uniqueDownloaders(resultSet.getObject("unique_downloaders", Integer.class))
                                .uniquePurchasers(resultSet.getObject("unique_purchasers", Integer.class))
                                .aggregatedAt(aggregatedAt)
                                .createdAt(createdAt)
                                .build());
                    }
                    return analytics;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get all book analytics", e);
                }
            }
        );
    }

    /* =================== SYSTEM ANALYTICS =================== */

    /**
     * Получает системную аналитику из базы данных по ID
     * @param id ID аналитики
     * @return объект SystemAnalytics или null, если не найдена
     */
    public static SystemAnalytics getSystemAnalyticsById(Long id) {
        return StepLogger.log(
            "Get system analytics from database by ID: " + id,
            () -> DBRequest.builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table(Table.SYSTEM_ANALYTICS.getTable())
                    .where(Condition.equalTo("id", id))
                    .extractAs(SystemAnalytics.class)
        );
    }

    /**
     * Получает всю системную аналитику из базы данных
     * @return список всей системной аналитики
     */
    public static List<SystemAnalytics> getAllSystemAnalytics() {
        return StepLogger.log(
            "Get all system analytics from database",
            () -> {
                String sql = "SELECT * FROM " + Table.SYSTEM_ANALYTICS.getTable() + " ORDER BY aggregated_at DESC";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    
                    List<SystemAnalytics> analytics = new ArrayList<>();
                    while (resultSet.next()) {
                        Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                        LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                        
                        Timestamp aggregatedAtTimestamp = resultSet.getTimestamp("aggregated_at");
                        LocalDateTime aggregatedAt = aggregatedAtTimestamp != null ? aggregatedAtTimestamp.toLocalDateTime() : null;
                        
                        analytics.add(SystemAnalytics.builder()
                                .id(resultSet.getLong("id"))
                                .totalBooks(resultSet.getObject("total_books", Integer.class))
                                .totalUsers(resultSet.getObject("total_users", Integer.class))
                                .totalViews(resultSet.getObject("total_views", Long.class))
                                .totalDownloads(resultSet.getObject("total_downloads", Long.class))
                                .totalPurchases(resultSet.getObject("total_purchases", Long.class))
                                .totalRevenue(resultSet.getBigDecimal("total_revenue"))
                                .totalReviews(resultSet.getObject("total_reviews", Long.class))
                                .totalRatings(resultSet.getObject("total_ratings", Long.class))
                                .averageRating(resultSet.getBigDecimal("average_rating"))
                                .averageReviewLength(resultSet.getBigDecimal("average_review_length"))
                                .mostPopularBookId(resultSet.getObject("most_popular_book_id", Long.class))
                                .mostPopularBookTitle(resultSet.getString("most_popular_book_title"))
                                .topGenre(resultSet.getString("top_genre"))
                                .topGenreBookCount(resultSet.getObject("top_genre_book_count", Integer.class))
                                .topGenreTotalViews(resultSet.getObject("top_genre_total_views", Long.class))
                                .aggregatedAt(aggregatedAt)
                                .createdAt(createdAt)
                                .build());
                    }
                    return analytics;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get all system analytics", e);
                }
            }
        );
    }

    /**
     * Получает последнюю системную аналитику из базы данных
     * @return объект SystemAnalytics или null, если не найдена
     */
    public static SystemAnalytics getLatestSystemAnalytics() {
        return StepLogger.log(
            "Get latest system analytics from database",
            () -> {
                String sql = "SELECT * FROM " + Table.SYSTEM_ANALYTICS.getTable() + " ORDER BY aggregated_at DESC LIMIT 1";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    
                    if (resultSet.next()) {
                        Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                        LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
                        
                        Timestamp aggregatedAtTimestamp = resultSet.getTimestamp("aggregated_at");
                        LocalDateTime aggregatedAt = aggregatedAtTimestamp != null ? aggregatedAtTimestamp.toLocalDateTime() : null;
                        
                        return SystemAnalytics.builder()
                                .id(resultSet.getLong("id"))
                                .totalBooks(resultSet.getObject("total_books", Integer.class))
                                .totalUsers(resultSet.getObject("total_users", Integer.class))
                                .totalViews(resultSet.getObject("total_views", Long.class))
                                .totalDownloads(resultSet.getObject("total_downloads", Long.class))
                                .totalPurchases(resultSet.getObject("total_purchases", Long.class))
                                .totalRevenue(resultSet.getBigDecimal("total_revenue"))
                                .totalReviews(resultSet.getObject("total_reviews", Long.class))
                                .totalRatings(resultSet.getObject("total_ratings", Long.class))
                                .averageRating(resultSet.getBigDecimal("average_rating"))
                                .averageReviewLength(resultSet.getBigDecimal("average_review_length"))
                                .mostPopularBookId(resultSet.getObject("most_popular_book_id", Long.class))
                                .mostPopularBookTitle(resultSet.getString("most_popular_book_title"))
                                .topGenre(resultSet.getString("top_genre"))
                                .topGenreBookCount(resultSet.getObject("top_genre_book_count", Integer.class))
                                .topGenreTotalViews(resultSet.getObject("top_genre_total_views", Long.class))
                                .aggregatedAt(aggregatedAt)
                                .createdAt(createdAt)
                                .build();
                    }
                    return null;
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to get latest system analytics", e);
                }
            }
        );
    }

    /* =================== UTILITY METHODS =================== */

    /**
     * Проверяет существование пользователя в базе данных
     * @param email email пользователя
     * @return true если пользователь существует, false в противном случае
     */
    public static boolean userExists(String email) {
        return StepLogger.log(
            "Check if user exists: " + email,
            () -> {
                String sql = "SELECT COUNT(*) FROM " + Table.USERS.getTable() + " WHERE email = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setString(1, email);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getInt(1) > 0;
                        }
                        return false;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to check if user exists: " + email, e);
                }
            }
        );
    }

    /**
     * Проверяет существование пользователя по nickname в базе данных
     * @param nickname nickname пользователя
     * @return true если пользователь существует, false в противном случае
     */
    public static boolean userExistsByNickname(String nickname) {
        return StepLogger.log(
            "Check if user exists by nickname: " + nickname,
            () -> {
                String sql = "SELECT COUNT(*) FROM " + Table.USERS.getTable() + " WHERE nickname = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setString(1, nickname);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getInt(1) > 0;
                        }
                        return false;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to check if user exists by nickname: " + nickname, e);
                }
            }
        );
    }

    /**
     * Проверяет существование книги в базе данных
     * @param bookId ID книги
     * @return true если книга существует, false в противном случае
     */
    public static boolean bookExists(Long bookId) {
        return StepLogger.log(
            "Check if book exists: " + bookId,
            () -> {
                String sql = "SELECT COUNT(*) FROM " + Table.BOOKS.getTable() + " WHERE id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, bookId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getInt(1) > 0;
                        }
                        return false;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to check if book exists: " + bookId, e);
                }
            }
        );
    }

    /**
     * Проверяет существование автора в базе данных
     * @param authorId ID автора
     * @return true если автор существует, false в противном случае
     */
    public static boolean authorExists(Long authorId) {
        return StepLogger.log(
            "Check if author exists: " + authorId,
            () -> {
                String sql = "SELECT COUNT(*) FROM " + Table.AUTHORS.getTable() + " WHERE id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, authorId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getInt(1) > 0;
                        }
                        return false;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to check if author exists: " + authorId, e);
                }
            }
        );
    }

    /**
     * Проверяет существование автора по полному имени в базе данных
     * @param fullName полное имя автора
     * @return true если автор существует, false в противном случае
     */
    public static boolean authorExistsByFullName(String fullName) {
        return StepLogger.log(
            "Check if author exists by full name: " + fullName,
            () -> {
                String sql = "SELECT COUNT(*) FROM " + Table.AUTHORS.getTable() + " WHERE full_name = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setString(1, fullName);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getInt(1) > 0;
                        }
                        return false;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to check if author exists by full name: " + fullName, e);
                }
            }
        );
    }

    /**
     * Проверяет существование отзыва в базе данных
     * @param reviewId ID отзыва
     * @return true если отзыв существует, false в противном случае
     */
    public static boolean reviewExists(Long reviewId) {
        return StepLogger.log(
            "Check if review exists: " + reviewId,
            () -> {
                String sql = "SELECT COUNT(*) FROM " + Table.REVIEWS.getTable() + " WHERE id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, reviewId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getInt(1) > 0;
                        }
                        return false;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to check if review exists: " + reviewId, e);
                }
            }
        );
    }

    /**
     * Проверяет существование оценки в базе данных
     * @param ratingId ID оценки
     * @return true если оценка существует, false в противном случае
     */
    public static boolean ratingExists(Long ratingId) {
        return StepLogger.log(
            "Check if rating exists: " + ratingId,
            () -> {
                String sql = "SELECT COUNT(*) FROM " + Table.RATINGS.getTable() + " WHERE id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, ratingId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getInt(1) > 0;
                        }
                        return false;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to check if rating exists: " + ratingId, e);
                }
            }
        );
    }

    /**
     * Проверяет существование покупки в базе данных
     * @param purchaseId ID покупки
     * @return true если покупка существует, false в противном случае
     */
    public static boolean purchaseExists(Long purchaseId) {
        return StepLogger.log(
            "Check if purchase exists: " + purchaseId,
            () -> {
                String sql = "SELECT COUNT(*) FROM " + Table.PURCHASES.getTable() + " WHERE id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, purchaseId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getInt(1) > 0;
                        }
                        return false;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to check if purchase exists: " + purchaseId, e);
                }
            }
        );
    }

    /**
     * Проверяет существование токена верификации email в базе данных
     * @param token токен верификации
     * @return true если токен существует, false в противном случае
     */
    public static boolean emailVerificationTokenExists(String token) {
        return StepLogger.log(
            "Check if email verification token exists: " + token,
            () -> {
                String sql = "SELECT COUNT(*) FROM " + Table.EMAIL_VERIFICATION_TOKENS.getTable() + " WHERE token = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setString(1, token);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getInt(1) > 0;
                        }
                        return false;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to check if email verification token exists: " + token, e);
                }
            }
        );
    }

    /**
     * Проверяет существование токена сброса пароля в базе данных
     * @param token токен сброса пароля
     * @return true если токен существует, false в противном случае
     */
    public static boolean passwordResetTokenExists(String token) {
        return StepLogger.log(
            "Check if password reset token exists: " + token,
            () -> {
                String sql = "SELECT COUNT(*) FROM " + Table.PASSWORD_RESET_TOKENS.getTable() + " WHERE token = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setString(1, token);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getInt(1) > 0;
                        }
                        return false;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to check if password reset token exists: " + token, e);
                }
            }
        );
    }

    /**
     * Проверяет существование аналитики книги в базе данных
     * @param bookAnalyticsId ID аналитики
     * @return true если аналитика существует, false в противном случае
     */
    public static boolean bookAnalyticsExists(Long bookAnalyticsId) {
        return StepLogger.log(
            "Check if book analytics exists: " + bookAnalyticsId,
            () -> {
                String sql = "SELECT COUNT(*) FROM " + Table.BOOK_ANALYTICS.getTable() + " WHERE id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, bookAnalyticsId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getInt(1) > 0;
                        }
                        return false;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to check if book analytics exists: " + bookAnalyticsId, e);
                }
            }
        );
    }

    /**
     * Проверяет существование системной аналитики в базе данных
     * @param systemAnalyticsId ID аналитики
     * @return true если аналитика существует, false в противном случае
     */
    public static boolean systemAnalyticsExists(Long systemAnalyticsId) {
        return StepLogger.log(
            "Check if system analytics exists: " + systemAnalyticsId,
            () -> {
                String sql = "SELECT COUNT(*) FROM " + Table.SYSTEM_ANALYTICS.getTable() + " WHERE id = ?";
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    
                    statement.setLong(1, systemAnalyticsId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getInt(1) > 0;
                        }
                        return false;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to check if system analytics exists: " + systemAnalyticsId, e);
                }
            }
        );
    }

    /**
     * Получает соединение с базой данных
     * Использует класс Config для чтения настроек подключения из application.properties
     * 
     * @return Connection объект
     * @throws SQLException если не удается подключиться
     */
    private static Connection getConnection() throws SQLException {
        String url = Config.getApplicationProperty("spring.datasource.url");
        String username = Config.getApplicationProperty("spring.datasource.username");
        String password = Config.getApplicationProperty("spring.datasource.password");
        
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Простой класс для логирования шагов выполнения
     */
    private static class StepLogger {
        /**
         * Логирует сообщение и выполняет действие
         * @param message сообщение для логирования
         * @param action действие для выполнения
         * @param <T> тип возвращаемого значения
         * @return результат выполнения действия
         */
        static <T> T log(String message, Supplier<T> action) {
            System.out.println("[DB Step] " + message);
            try {
                T result = action.get();
                System.out.println("[DB Step] ✓ Completed: " + message);
                return result;
            } catch (Exception e) {
                System.out.println("[DB Step] ✗ Failed: " + message + " - " + e.getMessage());
                throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
            }
        }
    }
}

