package online.ityura.springdigitallibrary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.ityura.springdigitallibrary.dto.BaseDto;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Ответ о состоянии приложения")
public class HealthResponse extends BaseDto {
    
    @Schema(description = "Статус работы приложения", example = "UP")
    private String status;
    
    @Schema(description = "Время работы приложения в формате HH:mm:ss", example = "02:15:30")
    private String uptime;
    
    @Schema(description = "Текущая метка времени в формате ISO-8601", example = "2025-12-13T04:14:37.122Z")
    private String timestamp;
}









