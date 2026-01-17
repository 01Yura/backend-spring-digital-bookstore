package online.ityura.springdigitallibrary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.ityura.springdigitallibrary.dto.BaseDto;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Запрос на обновление рейтинга")
public class UpdateRatingRequest extends BaseDto {
    
    @Schema(description = "Новое значение рейтинга (от 1 до 10)", example = "9", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "10")
    @NotNull(message = "Rating value is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 10, message = "Rating must be at most 10")
    private Short value;
}

