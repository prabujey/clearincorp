package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDocuments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id", nullable = false)
    private DocumentMaster documentMaster;

    @Column(name = "is_viewed", nullable = false)
    private boolean isViewed;

    @Column(name = "is_downloaded", nullable = false)
    private boolean isDownloaded;

    @Column(name = "uploaded_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime uploadedAt;
}
