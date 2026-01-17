package online.ityura.springdigitallibrary.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "books", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"title", "author_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "published_year")
    private Integer publishedYear;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "genre")
    private Genre genre;
    
    @Column(name = "deletion_locked", nullable = false)
    @Builder.Default
    private Boolean deletionLocked = false;
    
    @Column(name = "rating_avg", precision = 4, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal ratingAvg = BigDecimal.ZERO;
    
    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private Integer ratingCount = 0;
    
    @Column(name = "image_path")
    private String imagePath;
    
    @Column(name = "pdf_path")
    private String pdfPath;
    
    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;
    
    @Column(name = "discount_percent", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

