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
@Schema(description = "Ответ на запрос в службу поддержки")
public class SupportResponse extends BaseDto {
    
    @Schema(description = "Сообщение об успешной отправке", example = "Сообщение успешно отправлено в службу поддержки")
    private String message;
    
    @Schema(description = "Тип отправленного контента", example = "text")
    private String contentType;
}
