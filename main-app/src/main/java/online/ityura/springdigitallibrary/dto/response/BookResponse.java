package online.ityura.springdigitallibrary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.ityura.springdigitallibrary.dto.BaseDto;
import online.ityura.springdigitallibrary.model.Genre;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookResponse extends BaseDto {
    private Long id;
    private String title;
    private AuthorResponse author;
    private String description;
    private Integer publishedYear;
    private Genre genre;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Boolean hasFile;
    private String imagePath;
    private BigDecimal price;
    private BigDecimal discountPercent;
    private BigDecimal finalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReviewResponse> reviews;
}

