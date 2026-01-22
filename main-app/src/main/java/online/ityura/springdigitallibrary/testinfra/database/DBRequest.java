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
import lombok.Builder;
import lombok.Data;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DBRequest - Универсальный класс для выполнения запросов к базе данных
 * 
 * Этот класс предоставляет fluent API для построения и выполнения SQL запросов:
 * - Поддерживает различные типы запросов (SELECT, INSERT, UPDATE, DELETE)
 * - Автоматически маппит результаты в model объекты
 * - Управляет соединением с базой данных
 * - Обрабатывает параметры запросов через PreparedStatement
 * 
 * Использование:
 * <pre>
 * User user = DBRequest.builder()
 *     .requestType(DBRequest.RequestType.SELECT)
 *     .table("users")
 *     .where(Condition.equalTo("email", "user@example.com"))
 *     .extractAs(User.class);
 * </pre>
 * 
 * @author Generated
 * @version 1.0
 */
@Data
@Builder
public class DBRequest {
    
    /** Тип SQL запроса (SELECT, INSERT, UPDATE, DELETE) */
    private RequestType requestType;
    
    /** Имя таблицы для выполнения запроса */
    private String table;
    
    /** Список условий WHERE для запроса */
    private List<Condition> conditions;
    
    /** Класс для маппинга результата запроса */
    private Class<?> extractAsClass;

    /**
     * Создает новый экземпляр билдера для построения запроса
     * 
     * @return новый экземпляр DBRequestBuilder
     */
    public static DBRequestBuilder builder() {
        return new DBRequestBuilder();
    }

    /**
     * Выполняет запрос и извлекает результат в указанный класс
     * 
     * Этот метод является основным для выполнения запросов. Он:
     * - Устанавливает класс для маппинга результата
     * - Выполняет SQL запрос
     * - Маппит результат в объект указанного типа
     * 
     * @param <T> тип возвращаемого объекта
     * @param clazz класс для маппинга результата
     * @return объект типа T с данными из базы
     * @throws RuntimeException если произошла ошибка при выполнении запроса
     */
    public <T> T extractAs(Class<T> clazz) {
        this.extractAsClass = clazz;
        return executeQuery(clazz);
    }

    /**
     * Выполняет SQL запрос и возвращает результат
     * 
     * Этот приватный метод выполняет основную работу:
     * - Строит SQL запрос
     * - Устанавливает соединение с базой данных
     * - Устанавливает параметры для PreparedStatement
     * - Выполняет запрос
     * - Маппит результат в соответствующий model объект
     * 
     * @param <T> тип возвращаемого объекта
     * @param clazz класс для маппинга результата
     * @return объект типа T с данными из базы
     * @throws RuntimeException если произошла ошибка при выполнении запроса
     */
    private <T> T executeQuery(Class<T> clazz) {
        String sql = buildSQL();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // Устанавливаем параметры для условий WHERE
            if (conditions != null) {
                for (int i = 0; i < conditions.size(); i++) {
                    statement.setObject(i + 1, conditions.get(i).getValue());
                }
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                // Маппим результат в соответствующий model объект
                @SuppressWarnings("unchecked")
                T result = (T) (clazz == User.class ? mapToUser(resultSet) :
                        clazz == Author.class ? mapToAuthor(resultSet) :
                        clazz == Book.class ? mapToBook(resultSet) :
                        clazz == Review.class ? mapToReview(resultSet) :
                        clazz == Rating.class ? mapToRating(resultSet) :
                        clazz == Purchase.class ? mapToPurchase(resultSet) :
                        clazz == EmailVerificationToken.class ? mapToEmailVerificationToken(resultSet) :
                        clazz == PasswordResetToken.class ? mapToPasswordResetToken(resultSet) :
                        clazz == BookAnalytics.class ? mapToBookAnalytics(resultSet) :
                        clazz == SystemAnalytics.class ? mapToSystemAnalytics(resultSet) :
                        null);
                if (result == null) {
                    throw new UnsupportedOperationException("Mapping for " + clazz.getSimpleName() + " not implemented");
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
    }

    /**
     * Маппит ResultSet в объект User
     * 
     * Этот метод извлекает данные из ResultSet и создает объект User.
     * Ожидает, что ResultSet содержит следующие колонки:
     * - id (Long) - ID пользователя
     * - nickname (String) - никнейм пользователя
     * - email (String) - email пользователя
     * - password_hash (String) - хеш пароля
     * - role (String) - роль пользователя (USER, ADMIN)
     * - created_at (Timestamp) - дата создания
     * 
     * @param resultSet результат SQL запроса
     * @return объект User или null, если данных нет
     * @throws SQLException если произошла ошибка при чтении данных
     */
    private User mapToUser(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
            LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
            
            String roleStr = resultSet.getString("role");
            Role role = roleStr != null ? Role.valueOf(roleStr) : null;
            
            return User.builder()
                    .id(resultSet.getLong("id"))
                    .nickname(resultSet.getString("nickname"))
                    .email(resultSet.getString("email"))
                    .passwordHash(resultSet.getString("password_hash"))
                    .role(role)
                    .createdAt(createdAt)
                    .build();
        }
        return null;
    }

    /**
     * Маппит ResultSet в объект Author
     * 
     * Этот метод извлекает данные из ResultSet и создает объект Author.
     * Ожидает, что ResultSet содержит следующие колонки:
     * - id (Long) - ID автора
     * - full_name (String) - полное имя автора
     * - created_at (Timestamp) - дата создания
     * 
     * @param resultSet результат SQL запроса
     * @return объект Author или null, если данных нет
     * @throws SQLException если произошла ошибка при чтении данных
     */
    private Author mapToAuthor(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
            LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
            
            return Author.builder()
                    .id(resultSet.getLong("id"))
                    .fullName(resultSet.getString("full_name"))
                    .createdAt(createdAt)
                    .build();
        }
        return null;
    }

    /**
     * Маппит ResultSet в объект Book
     * 
     * Этот метод извлекает данные из ResultSet и создает объект Book.
     * Ожидает, что ResultSet содержит следующие колонки:
     * - id (Long) - ID книги
     * - title (String) - название книги
     * - author_id (Long) - ID автора (маппится как Long, не как объект Author)
     * - description (String) - описание книги
     * - published_year (Integer) - год публикации
     * - genre (String) - жанр книги
     * - deletion_locked (Boolean) - флаг блокировки удаления
     * - rating_avg (BigDecimal) - средний рейтинг
     * - rating_count (Integer) - количество оценок
     * - image_path (String) - путь к изображению
     * - pdf_path (String) - путь к PDF файлу
     * - created_at (Timestamp) - дата создания
     * - updated_at (Timestamp) - дата обновления
     * 
     * @param resultSet результат SQL запроса
     * @return объект Book или null, если данных нет
     * @throws SQLException если произошла ошибка при чтении данных
     */
    private Book mapToBook(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
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
                // Создаем минимальный объект Author только с ID
                author = Author.builder().id(authorId).build();
            }
            
            return Book.builder()
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
                    .build();
        }
        return null;
    }

    /**
     * Маппит ResultSet в объект Review
     * 
     * Этот метод извлекает данные из ResultSet и создает объект Review.
     * Ожидает, что ResultSet содержит следующие колонки:
     * - id (Long) - ID отзыва
     * - book_id (Long) - ID книги
     * - user_id (Long) - ID пользователя
     * - text (String) - текст отзыва
     * - created_at (Timestamp) - дата создания
     * - updated_at (Timestamp) - дата обновления
     * 
     * @param resultSet результат SQL запроса
     * @return объект Review или null, если данных нет
     * @throws SQLException если произошла ошибка при чтении данных
     */
    private Review mapToReview(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
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
            
            return Review.builder()
                    .id(resultSet.getLong("id"))
                    .book(book)
                    .user(user)
                    .text(resultSet.getString("text"))
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();
        }
        return null;
    }

    /**
     * Маппит ResultSet в объект Rating
     * 
     * Этот метод извлекает данные из ResultSet и создает объект Rating.
     * Ожидает, что ResultSet содержит следующие колонки:
     * - id (Long) - ID оценки
     * - book_id (Long) - ID книги
     * - user_id (Long) - ID пользователя
     * - value (Short) - значение оценки
     * - created_at (Timestamp) - дата создания
     * - updated_at (Timestamp) - дата обновления
     * 
     * @param resultSet результат SQL запроса
     * @return объект Rating или null, если данных нет
     * @throws SQLException если произошла ошибка при чтении данных
     */
    private Rating mapToRating(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
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
            
            return Rating.builder()
                    .id(resultSet.getLong("id"))
                    .book(book)
                    .user(user)
                    .value(resultSet.getShort("value"))
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();
        }
        return null;
    }

    /**
     * Маппит ResultSet в объект Purchase
     * 
     * Этот метод извлекает данные из ResultSet и создает объект Purchase.
     * Ожидает, что ResultSet содержит следующие колонки:
     * - id (Long) - ID покупки
     * - user_id (Long) - ID пользователя
     * - book_id (Long) - ID книги
     * - stripe_payment_intent_id (String) - ID платежного намерения Stripe
     * - amount_paid (BigDecimal) - сумма оплаты
     * - status (String) - статус покупки (PENDING, COMPLETED, FAILED, REFUNDED)
     * - created_at (Timestamp) - дата создания
     * - updated_at (Timestamp) - дата обновления
     * 
     * @param resultSet результат SQL запроса
     * @return объект Purchase или null, если данных нет
     * @throws SQLException если произошла ошибка при чтении данных
     */
    private Purchase mapToPurchase(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
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
            
            return Purchase.builder()
                    .id(resultSet.getLong("id"))
                    .user(user)
                    .book(book)
                    .stripePaymentIntentId(resultSet.getString("stripe_payment_intent_id"))
                    .amountPaid(resultSet.getBigDecimal("amount_paid"))
                    .status(status)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();
        }
        return null;
    }

    /**
     * Маппит ResultSet в объект EmailVerificationToken
     * 
     * Этот метод извлекает данные из ResultSet и создает объект EmailVerificationToken.
     * Ожидает, что ResultSet содержит следующие колонки:
     * - id (Long) - ID токена
     * - token (String) - токен верификации
     * - user_id (Long) - ID пользователя
     * - expires_at (Timestamp) - дата истечения
     * - created_at (Timestamp) - дата создания
     * - used (Boolean) - флаг использования токена
     * 
     * @param resultSet результат SQL запроса
     * @return объект EmailVerificationToken или null, если данных нет
     * @throws SQLException если произошла ошибка при чтении данных
     */
    private EmailVerificationToken mapToEmailVerificationToken(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
            LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
            
            Timestamp expiresAtTimestamp = resultSet.getTimestamp("expires_at");
            LocalDateTime expiresAt = expiresAtTimestamp != null ? expiresAtTimestamp.toLocalDateTime() : null;
            
            Long userId = resultSet.getLong("user_id");
            User user = null;
            if (!resultSet.wasNull() && userId != null) {
                user = User.builder().id(userId).build();
            }
            
            return EmailVerificationToken.builder()
                    .id(resultSet.getLong("id"))
                    .token(resultSet.getString("token"))
                    .user(user)
                    .expiresAt(expiresAt)
                    .createdAt(createdAt)
                    .used(resultSet.getBoolean("used"))
                    .build();
        }
        return null;
    }

    /**
     * Маппит ResultSet в объект PasswordResetToken
     * 
     * Этот метод извлекает данные из ResultSet и создает объект PasswordResetToken.
     * Ожидает, что ResultSet содержит следующие колонки:
     * - id (Long) - ID токена
     * - token (String) - токен сброса пароля
     * - user_id (Long) - ID пользователя
     * - expires_at (Timestamp) - дата истечения
     * - created_at (Timestamp) - дата создания
     * - used (Boolean) - флаг использования токена
     * 
     * @param resultSet результат SQL запроса
     * @return объект PasswordResetToken или null, если данных нет
     * @throws SQLException если произошла ошибка при чтении данных
     */
    private PasswordResetToken mapToPasswordResetToken(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
            LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
            
            Timestamp expiresAtTimestamp = resultSet.getTimestamp("expires_at");
            LocalDateTime expiresAt = expiresAtTimestamp != null ? expiresAtTimestamp.toLocalDateTime() : null;
            
            Long userId = resultSet.getLong("user_id");
            User user = null;
            if (!resultSet.wasNull() && userId != null) {
                user = User.builder().id(userId).build();
            }
            
            return PasswordResetToken.builder()
                    .id(resultSet.getLong("id"))
                    .token(resultSet.getString("token"))
                    .user(user)
                    .expiresAt(expiresAt)
                    .createdAt(createdAt)
                    .used(resultSet.getBoolean("used"))
                    .build();
        }
        return null;
    }

    /**
     * Маппит ResultSet в объект BookAnalytics
     * 
     * Этот метод извлекает данные из ResultSet и создает объект BookAnalytics.
     * Ожидает, что ResultSet содержит следующие колонки:
     * - id (Long) - ID аналитики
     * - book_id (Long) - ID книги
     * - book_title (String) - название книги
     * - book_genre (String) - жанр книги
     * - view_count (Long) - количество просмотров
     * - download_count (Long) - количество скачиваний
     * - purchase_count (Long) - количество покупок
     * - review_count (Long) - количество отзывов
     * - rating_count (Long) - количество оценок
     * - average_rating (BigDecimal) - средний рейтинг
     * - total_revenue (BigDecimal) - общая выручка
     * - unique_viewers (Integer) - уникальные просмотры
     * - unique_downloaders (Integer) - уникальные скачивания
     * - unique_purchasers (Integer) - уникальные покупки
     * - aggregated_at (Timestamp) - дата агрегации
     * - created_at (Timestamp) - дата создания
     * 
     * @param resultSet результат SQL запроса
     * @return объект BookAnalytics или null, если данных нет
     * @throws SQLException если произошла ошибка при чтении данных
     */
    private BookAnalytics mapToBookAnalytics(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
            LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
            
            Timestamp aggregatedAtTimestamp = resultSet.getTimestamp("aggregated_at");
            LocalDateTime aggregatedAt = aggregatedAtTimestamp != null ? aggregatedAtTimestamp.toLocalDateTime() : null;
            
            return BookAnalytics.builder()
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
                    .build();
        }
        return null;
    }

    /**
     * Маппит ResultSet в объект SystemAnalytics
     * 
     * Этот метод извлекает данные из ResultSet и создает объект SystemAnalytics.
     * Ожидает, что ResultSet содержит следующие колонки:
     * - id (Long) - ID аналитики
     * - total_books (Integer) - общее количество книг
     * - total_users (Integer) - общее количество пользователей
     * - total_views (Long) - общее количество просмотров
     * - total_downloads (Long) - общее количество скачиваний
     * - total_purchases (Long) - общее количество покупок
     * - total_revenue (BigDecimal) - общая выручка
     * - total_reviews (Long) - общее количество отзывов
     * - total_ratings (Long) - общее количество оценок
     * - average_rating (BigDecimal) - средний рейтинг
     * - average_review_length (BigDecimal) - средняя длина отзыва
     * - most_popular_book_id (Long) - ID самой популярной книги
     * - most_popular_book_title (String) - название самой популярной книги
     * - top_genre (String) - топ жанр
     * - top_genre_book_count (Integer) - количество книг в топ жанре
     * - top_genre_total_views (Long) - общее количество просмотров топ жанра
     * - aggregated_at (Timestamp) - дата агрегации
     * - created_at (Timestamp) - дата создания
     * 
     * @param resultSet результат SQL запроса
     * @return объект SystemAnalytics или null, если данных нет
     * @throws SQLException если произошла ошибка при чтении данных
     */
    private SystemAnalytics mapToSystemAnalytics(ResultSet resultSet) throws SQLException {
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
    }

    /**
     * Строит SQL запрос на основе параметров запроса
     * 
     * Этот метод создает SQL запрос в зависимости от типа запроса:
     * - SELECT: создает SELECT запрос с условиями WHERE
     * - INSERT, UPDATE, DELETE: пока не реализованы
     * 
     * Для SELECT запросов:
     * - Добавляет "SELECT * FROM table_name"
     * - Добавляет условия WHERE, если они есть
     * - Объединяет несколько условий через AND
     * 
     * @return готовый SQL запрос
     * @throws UnsupportedOperationException если тип запроса не поддерживается
     */
    private String buildSQL() {
        StringBuilder sql = new StringBuilder();

        switch (requestType) {
            case SELECT:
                sql.append("SELECT * FROM ").append(table);
                if (conditions != null && !conditions.isEmpty()) {
                    sql.append(" WHERE ");
                    for (int i = 0; i < conditions.size(); i++) {
                        if (i > 0) sql.append(" AND ");
                        sql.append(conditions.get(i).getColumn()).append(" ").append(conditions.get(i).getOperator()).append(" ?");
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Request type " + requestType + " not implemented");
        }

        return sql.toString();
    }

    /**
     * Создает соединение с базой данных
     * 
     * Этот метод использует настройки из application.properties для подключения к БД.
     * Использует класс Config для чтения свойств с учетом приоритетов:
     * 1. Системные свойства (System.getProperty)
     * 2. Переменные окружения (System.getenv)
     * 3. application.properties файл (с обработкой синтаксиса ${VAR:default})
     * 
     * Используемые свойства:
     * - spring.datasource.url - URL базы данных
     * - spring.datasource.username - имя пользователя
     * - spring.datasource.password - пароль
     * 
     * @return Connection объект для работы с базой данных
     * @throws SQLException если не удается подключиться к базе данных
     */
    private Connection getConnection() throws SQLException {
        // Явно загружаем драйвер PostgreSQL для надежности
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found. Make sure postgresql dependency is in classpath.", e);
        }
        
        String url = Config.getApplicationProperty("spring.datasource.url");
        String username = Config.getApplicationProperty("spring.datasource.username");
        String password = Config.getApplicationProperty("spring.datasource.password");
        
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Перечисление типов SQL запросов
     * 
     * Поддерживаемые типы:
     * - SELECT - выборка данных
     * - INSERT - вставка данных (пока не реализовано)
     * - UPDATE - обновление данных (пока не реализовано)
     * - DELETE - удаление данных (пока не реализовано)
     */
    public enum RequestType {
        /** Выборка данных из таблицы */
        SELECT, 
        /** Вставка данных в таблицу (пока не реализовано) */
        INSERT, 
        /** Обновление данных в таблице (пока не реализовано) */
        UPDATE, 
        /** Удаление данных из таблицы (пока не реализовано) */
        DELETE
    }

    /**
     * Внутренний класс-билдер для построения DBRequest
     * 
     * Этот класс реализует паттерн Builder для удобного создания запросов.
     * Позволяет цепочкой вызовов настроить все параметры запроса.
     * 
     * Пример использования:
     * <pre>
     * User user = DBRequest.builder()
     *     .requestType(DBRequest.RequestType.SELECT)
     *     .table("users")
     *     .where(Condition.equalTo("email", "user@example.com"))
     *     .extractAs(User.class);
     * </pre>
     */
    public static class DBRequestBuilder {
        /** Тип SQL запроса */
        private RequestType requestType;
        
        /** Имя таблицы для запроса */
        private String table;
        
        /** Список условий WHERE */
        private List<Condition> conditions = new ArrayList<>();
        
        /** Класс для маппинга результата */
        private Class<?> extractAsClass;

        /**
         * Устанавливает тип SQL запроса
         * 
         * @param requestType тип запроса (SELECT, INSERT, UPDATE, DELETE)
         * @return this для поддержки цепочки вызовов
         */
        public DBRequestBuilder requestType(RequestType requestType) {
            this.requestType = requestType;
            return this;
        }

        /**
         * Добавляет условие WHERE к запросу
         * 
         * Можно вызывать несколько раз для добавления нескольких условий.
         * Условия объединяются через AND.
         * 
         * @param condition условие для WHERE
         * @return this для поддержки цепочки вызовов
         */
        public DBRequestBuilder where(Condition condition) {
            this.conditions.add(condition);
            return this;
        }

        /**
         * Устанавливает имя таблицы для запроса
         * 
         * @param table имя таблицы
         * @return this для поддержки цепочки вызовов
         */
        public DBRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

        /**
         * Выполняет запрос и извлекает результат в указанный класс
         * 
         * Этот метод завершает построение запроса и выполняет его.
         * Создает новый экземпляр DBRequest с текущими параметрами и выполняет запрос.
         * 
         * @param <T> тип возвращаемого объекта
         * @param clazz класс для маппинга результата
         * @return объект типа T с данными из базы
         * @throws RuntimeException если произошла ошибка при выполнении запроса
         */
        public <T> T extractAs(Class<T> clazz) {
            this.extractAsClass = clazz;
            DBRequest request = DBRequest.builder()
                    .requestType(requestType)
                    .table(table)
                    .conditions(conditions)
                    .extractAsClass(extractAsClass)
                    .build();
            return request.extractAs(clazz);
        }
    }
}

