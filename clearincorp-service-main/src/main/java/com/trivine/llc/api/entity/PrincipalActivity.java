// PrincipalActivity.java
package com.trivine.llc.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;

@Entity
@Table(name = "principal_activity",
        uniqueConstraints = @UniqueConstraint(name = "uq_principal_activity_value", columnNames = "value"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PrincipalActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150, nullable = false)
    private String value;

    // Stores JSON array (e.g., ["A","B","C"])
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sub_activities", columnDefinition = "json", nullable = false)
    private List<String> subActivities;
}


