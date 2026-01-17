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
public class ReviewResponse extends BaseDto {
    private Long id;
    private Long bookId;
    private UserInfoResponse user;
    private String text;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

