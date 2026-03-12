package com.trivine.llc.api.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

//@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "company_id")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id",nullable = false)
    private Long companyId;

    @Column(name = "company_name")
    @Size(max = 255)
    private String companyName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "suffix_id")
    private SuffixMaster suffixMaster;

    @Column(name = "company_effective_date")
    private LocalDate companyEffectiveDate;

    @Column(name = "state")
    @Size(max = 255)
    private String state;

    @Column(name = "use_address")
    private Boolean useAddress= false;

    @Column(name = "company_desc")
    @Size(max = 255)
    private String companyDesc;

    @Column(name = "street_address_1")
    @Size(max = 255)
    private String streetAddress1;

    @Column(name = "street_address_2")
    @Size(max = 255)
    private String streetAddress2;

    @Column(name = "city")
    @Size(max = 255)
    private String city;

    @Column(name = "zip_code")
    @Size(max = 255)
    private String zipCode;

    @Column(name = "country")
    @Size(max = 255)
    private String country;

    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Column(name = "company_email1")
    @Size(max = 255)
    private String companyEmail1;

    @Email(regexp = ".+[@].+[\\.].+", message = "Email should be valid")
    @Column(name = "company_email2")
    @Size(max = 255)
    private String companyEmail2;

    @Column(name = "reg_form1")
    private Boolean regForm1= false;
        @Column(name = "reg_form2")
        private Boolean regForm2 = false;
    @Column(name = "reg_form3")
    private Boolean regForm3= false;
    @Column(name = "reg_form4")
    private Boolean regForm4= false;

    @Column(name = "management_style")
    private String managementStyle;

    @Pattern(
            regexp = "^\\(\\d{3}\\) \\d{3}-\\d{4}$",
            message = "Phone number must be in the format (123) 456-7890"
    )
    @Size(max = 255)
    @Column(name = "company_phone1")
    private String companyPhone1;

    @Pattern(
            regexp = "^\\(\\d{3}\\) \\d{3}-\\d{4}$",
            message = "Phone number must be in the format (123) 456-7890"
    )
    @Size(max = 255)
    @Column(name = "company_phone2")
    private String companyPhone2;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "login_user_id")
    private LoginUser loginUser;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "last_updated", insertable = false)
    private LocalDateTime lastUpdated;

    @Column(name = "is_active")
    private Boolean isActive=true;


    @Column(name="expedited_service_chosen")
    private Boolean IsExpeditedServiceSelected= false;

    @Column(name = "trade_name", nullable = false)
    private String tradeName;

    @Column(name = "principal_activity", nullable = false)
    private String principalActivity;

    // @Column(name = "is_new_flag", nullable = false)
    // private boolean isNewFlag;



    @Builder.Default
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<ManagerMember> managers = new HashSet<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompanyServices> companyServices;

    @Builder.Default
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Member> members = new HashSet<>();

    @EqualsAndHashCode.Exclude
    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private RegisteredAgent registeredAgent;

    @EqualsAndHashCode.Exclude
    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CompanyPrincipal companyPrincipal;

}
