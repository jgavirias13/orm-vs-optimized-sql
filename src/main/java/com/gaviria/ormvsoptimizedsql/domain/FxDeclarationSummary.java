package com.gaviria.ormvsoptimizedsql.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fx_declaration_summary")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class FxDeclarationSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "fx_declaration_id", nullable = false, unique = true)
    private FxDeclaration fxDeclaration;

    @Column(name = "total_local", precision = 18, scale = 2)
    private BigDecimal totalLocal;

    @Column(name = "total_usd", precision = 18, scale = 2)
    private BigDecimal totalUsd;

    @Column(name = "items_json", columnDefinition = "jsonb")
    private String itemsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // getters/setters
}