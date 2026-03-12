package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "document_master")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_type_id", nullable = false)
    private Long documentTypeId;

    @NotBlank(message = "Type name is required")
    @Size(max = 255)
    @Column(name = "type_name", nullable = false)
    private String typeName;
}
