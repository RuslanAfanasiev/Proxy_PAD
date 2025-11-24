package org.example.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@MappedSuperclass
@Data
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime lastChangedAt;

    @PrePersist
    @PreUpdate
    protected void updateTimestamp() {
        lastChangedAt = LocalDateTime.now();
    }
}
