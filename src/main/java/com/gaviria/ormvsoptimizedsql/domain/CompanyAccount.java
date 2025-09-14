package com.gaviria.ormvsoptimizedsql.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(
        name = "company_account",
        uniqueConstraints = @UniqueConstraint(name = "uq_company_account", columnNames = {"company_id", "account_number"})
)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class CompanyAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "account_number", nullable = false, length = 40)
    private String accountNumber;

    private String iban;
    private String bankName;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(length = 2)
    private String country;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "companyAccount")
    private List<Movement> movements;

    @OneToMany(mappedBy = "companyAccount")
    private List<FxDeclaration> fxDeclarations;

    // getters/setters
}