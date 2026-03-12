package com.trivine.llc.api.entity;

import com.trivine.llc.api.crypto.EncryptResult;
import com.trivine.llc.api.crypto.EncryptResultConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ein_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EinDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Relations ---
    @NotNull(message = "Company is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotNull(message = "Reason for applying is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reason_for_applying_id", nullable = false)
    private ReasonForApplying reasonForApplying;

    // --- EIN fields ---
    @NotBlank @Size(max = 255)
    @Column(name = "llc_name", nullable = false, length = 255)
    private String llcName;

    @NotNull @Min(1) @Max(9999)
    @Column(name = "number_of_members", nullable = false)
    private Integer numberOfMembers;

    @Size(max = 255)
    @Column(name = "trade_name", length = 255)
    private String tradeName;

    @NotBlank @Size(max = 255)
    @Column(name = "responsible_party_name", nullable = false, length = 255)
    private String responsiblePartyName;

    // 111-11-1111
    @Convert(converter = EncryptResultConverter.class)
    @Column(name="ssn_id_cipher_json", nullable=false)
    private EncryptResult ssnIdCipherJson;

    @Size(max = 255)
    @Column(name = "business_street_address", length = 255)
    private String businessStreetAddress;

    @Size(max = 100)
    @Column(name = "business_city", length = 100)
    private String businessCity;

    @Size(max = 100)
    @Column(name = "business_state", length = 100)
    private String businessState;

    @Size(max = 20)
    @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "Business ZIP must be 12345 or 12345-6789")
    @Column(name = "business_zip_code", length = 20)
    private String businessZipCode;

    @Size(max = 255)
    @Column(name = "mailing_street_address", length = 255)
    private String mailingStreetAddress;

    @Size(max = 100)
    @Column(name = "mailing_city", length = 100)
    private String mailingCity;

    @Size(max = 100)
    @Column(name = "mailing_state", length = 100)
    private String mailingState;

    @Size(max = 20)
    @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "Mailing ZIP must be 12345 or 12345-6789")
    @Column(name = "mailing_zip_code", length = 20)
    private String mailingZipCode;

    @Size(max = 20)
    @Column(name = "closing_month", length = 20)
    private String closingMonth;

    @Size(max = 255)
    @Column(name = "principal_activity", length = 255)
    private String principalActivity;

    @Column(name = "formation_date")
    private LocalDate formationDate;

    // Flags
    @NotNull @Column(name = "use_physical_address_for_mailing")
    private Boolean usePhysicalAddressForMailing;

    @NotNull @Column(name = "husband_wife_members")
    private Boolean husbandWifeMembers;

    @NotNull @Column(name = "sells_alcohol_tobacco_firearms")
    private Boolean sellsAlcoholTobaccoFirearms;

    @NotNull @Column(name = "file_annual_payroll_taxes")
    private Boolean fileAnnualPayrollTaxes;

    @NotNull @Column(name = "involves_gambling")
    private Boolean involvesGambling;

    @NotNull @Column(name = "owns_heavy_vehicle")
    private Boolean ownsHeavyVehicle;

    @NotNull @Column(name = "pays_federal_excise_taxes")
    private Boolean paysFederalExciseTaxes;

    @NotNull @Column(name = "hire_employee_in_12_months")
    private Boolean hireEmployeeIn12Months;

    @Column(name = "first_wage_date")
    private LocalDate firstWageDate;

    @Min(0) @Max(999999)
    @Column(name = "household_employees")
    private Integer householdEmployees;

    @Min(0) @Max(999999)
    @Column(name = "agricultural_employees")
    private Integer agriculturalEmployees;

    @Min(0) @Max(999999)
    @Column(name = "other_employees")
    private Integer otherEmployees;

    @Size(max = 100)
    @Column(name = "llc_type", length = 100)
    private String llcType;

    // 12-3456789
    @Size(max = 20)
    @Pattern(regexp = "^(|[0-9]{2}-[0-9]{7})$", message = "Previous Federal Tax ID (EIN) must be in the format ##-#######")
    @Column(name = "previous_federal_tax_id", length = 20)
    private String previousFederalTaxId;

    @Email @Size(max = 255)
    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "principal_sub_activity", length = 150)
    private String principalSubActivity;

    // (654) 646-5456
    @Size(max = 25)
    @Pattern(regexp = "^\\(\\d{3}\\) \\d{3}-\\d{4}$", message = "Phone must be in the format (###) ###-####")
    @Column(name = "phone_number", length = 25)
    private String phoneNumber;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private Instant createdOn;

    @UpdateTimestamp
    @Column(name = "updated_on", nullable = false)
    private Instant updatedOn;
}

