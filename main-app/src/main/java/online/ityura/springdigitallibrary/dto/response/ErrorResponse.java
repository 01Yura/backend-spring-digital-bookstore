package online.ityura.springdigitallibrary.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.ityura.springdigitallibrary.dto.BaseDto;

import java.time.Instant;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Стандартизированный ответ об ошибке")
public class ErrorResponse extends BaseDto {
    
    @Schema(description = "HTTP статус код", example = "400")
    private Integer status;
    
    @Schema(description = "Код типа ошибки", example = "VALIDATION_ERROR")
    private String error;
    
    @Schema(description = "Сообщение об ошибке", example = "Validation failed")
    private String message;
    
    @Schema(description = "Ошибки валидации по полям (только для ошибок валидации)", example = "{\"title\":\"Title is required\",\"authorName\":\"Author name is required\"}")
    private Map<String, String> fieldErrors;
    
    @Schema(description = "Временная метка ошибки", example = "2025-12-17T13:20:00Z")
    private Instant timestamp;
    
    @Schema(description = "Путь запроса", example = "/api/v1/admin/books")
    private String path;
}












