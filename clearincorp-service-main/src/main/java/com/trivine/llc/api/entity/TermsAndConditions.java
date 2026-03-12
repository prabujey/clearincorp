package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "terms_and_conditions_master")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TermsAndConditions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_type", nullable = false, unique = true)
    private String formType;

    @Lob
    @Column(nullable = false)
    private String content;

}

