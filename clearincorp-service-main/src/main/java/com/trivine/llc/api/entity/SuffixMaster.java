package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "suffix_master")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuffixMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suffix_id", nullable = false)
    private Long suffixId;

    @Size(max = 255)
    @Column(name = "suffix")
    private String suffix;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Builder.Default
    @Column(name = "is_active")
    private boolean active = true;

    @OneToMany(mappedBy = "suffixMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Company> companies;
}
