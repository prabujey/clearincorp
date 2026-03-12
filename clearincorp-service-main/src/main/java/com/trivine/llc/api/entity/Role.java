package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "role")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id", nullable = false)
        private Long id;

        @Column(name = "name", nullable = false)
        @Size(max = 255)
        private String name;

        @Builder.Default
        @Column(name = "is_deleted", nullable = false)
        private Boolean isDeleted = false;

        @Column(name = "add_by")
        @Size(max = 255)
        private String addBy;

        @Column(name = "add_date", nullable = false, updatable = false)
        private LocalDateTime addDate;

        @Column(name = "last_mod_by")
        @Size(max = 255)
        private String lastModBy;

        @Column(name = "last_mod_date")
        private LocalDateTime lastModDate;

        @PrePersist
        protected void onCreate() {
                this.addDate = LocalDateTime.now();
                this.lastModDate = LocalDateTime.now();
                this.isDeleted = false;
        }

        @PreUpdate
        protected void onUpdate() {
                this.lastModDate = LocalDateTime.now();
        }
}
