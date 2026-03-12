package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "token")
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class EmailToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Size(max = 255)
    @Column(name = "email")
    private String email;

    @Size(min = 6, max = 6, message = "Token must be exactly 6 characters")
    @Column(name = "token_value", length = 6, nullable = false)
    private String tokenValue;

    @Column(name = "created_on", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdOn;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;
}
