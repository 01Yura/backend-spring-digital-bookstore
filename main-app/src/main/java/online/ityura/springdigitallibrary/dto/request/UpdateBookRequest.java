package online.ityura.springdigitallibrary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.ityura.springdigitallibrary.dto.BaseDto;
import online.ityura.springdigitallibrary.model.Genre;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Запрос на обновление книги (все поля опциональны)")
public class UpdateBookRequest extends BaseDto {
    
    @Schema(description = "Название книги", example = "Updated Spring Boot Guide")
    private String title;
    
    @Schema(description = "Полное имя автора", example = "Jane Doe")
    private String authorName;
    
    @Schema(description = "Описание книги", example = "Updated description")
    private String description;
    
    @Schema(description = "Год публикации", example = "2024", minimum = "1000", maximum = "9999")
    @Min(value = 1000, message = "Published year must be at least 1000")
    @Max(value = 9999, message = "Published year must be at most 9999")
    private Integer publishedYear;
    
    @Schema(description = "Жанр книги", example = "FICTION")
    private Genre genre;
    
    @Schema(description = "Цена книги в USD", example = "9.99", minimum = "0")
    @Min(value = 0, message = "Price must be non-negative")
    private BigDecimal price;
    
    @Schema(description = "Процент скидки", example = "10.00", minimum = "0", maximum = "100")
    @Min(value = 0, message = "Discount percent must be non-negative")
    @Max(value = 100, message = "Discount percent must not exceed 100")
    private BigDecimal discountPercent;
}

