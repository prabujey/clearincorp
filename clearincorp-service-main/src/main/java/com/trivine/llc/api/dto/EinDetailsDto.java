package com.trivine.llc.api.dto;

import com.trivine.llc.api.crypto.EncryptResult;
import lombok.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Data
public class EinDetailsDto {
    private Long id;

    @NotNull
    private Long companyId;

    @NotNull
    private Long reasonForApplyingId;

    @NotBlank @Size(max = 150)  private String llcName;
    @NotNull @Min(1) @Max(100) private Integer numberOfMembers;
    @Size(max = 150)            private String tradeName;
    @NotBlank @Size(max = 60)  private String responsiblePartyName;
    @NotBlank @Pattern(regexp = "^[0-9]{3}-[0-9]{2}-[0-9]{4}$") private String ssnId="***-**-****";
    private EncryptResult ssnIdCipherJson;

    @Size(max = 255) private String businessStreetAddress;
    @Size(max = 100) private String businessCity;
    @Size(max = 100) private String businessState;
    @Size(max = 20)  @Pattern(regexp = "^\\d{5}(-\\d{4})?$") private String businessZipCode;

    @Size(max = 255) private String mailingStreetAddress;
    @Size(max = 100) private String mailingCity;
    @Size(max = 100) private String mailingState;
    @Size(max = 20)  @Pattern(regexp = "^\\d{5}(-\\d{4})?$") private String mailingZipCode;

    @Size(max = 20)  private String closingMonth;
    @Size(max = 255) private String principalActivity;
    @Size(max = 255) private String principalSubActivity;
    private LocalDate formationDate;

    @NotNull private Boolean usePhysicalAddressForMailing;
    @NotNull private Boolean husbandWifeMembers;
    @NotNull private Boolean sellsAlcoholTobaccoFirearms;
    @NotNull private Boolean fileAnnualPayrollTaxes;
    @NotNull private Boolean involvesGambling;
    @NotNull private Boolean ownsHeavyVehicle;
    @NotNull private Boolean paysFederalExciseTaxes;
    @NotNull private Boolean hireEmployeeIn12Months;

    private LocalDate firstWageDate;

    @Min(0) @Max(999999) private Integer householdEmployees;
    @Min(0) @Max(999999) private Integer agriculturalEmployees;
    @Min(0) @Max(999999) private Integer otherEmployees;

    @Size(max = 100) private String llcType;

    @Pattern(regexp = "^(|[0-9]{2}-[0-9]{7})$")
    private String previousFederalTaxId;

    @Email @Size(max = 255) private String email;
    @Pattern(regexp = "^\\(\\d{3}\\) \\d{3}-\\d{4}$") private String phoneNumber;
}


