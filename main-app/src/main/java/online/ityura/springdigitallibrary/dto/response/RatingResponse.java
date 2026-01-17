package online.ityura.springdigitallibrary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.ityura.springdigitallibrary.dto.BaseDto;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingResponse extends BaseDto {
    private Long id;
    private Long bookId;
    private Long userId;
    private Short value;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

