package com.gaviria.ormvsoptimizedsql.domain

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

@Entity
@Table(name = "movement")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
class Movement {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private val id: Long? = null

   @ManyToOne
   @JoinColumn(name = "company_account_id", nullable = false)
   private val companyAccount: CompanyAccount? = null

   @ManyToOne
   @JoinColumn(name = "counterparty_account_id", nullable = false)
   private val counterpartyAccount: CounterpartyAccount? = null

   @ManyToOne
   @JoinColumn(name = "currency_code", referencedColumnName = "code", nullable = false)
   private val currency: Currency? = null

   @Column(nullable = false, precision = 18, scale = 2)
   private var amount: BigDecimal? = null

   @Column(name = "booked_at", nullable = false)
   private var bookedAt: LocalDateTime? = null

   private val description: String? = null

   @Column(name = "created_at", nullable = false)
   private var createdAt: Instant? = Instant.now()

   @ManyToMany
   @JoinTable(
      name = "movement_tags",
      joinColumns = [JoinColumn(name = "movement_id")],
      inverseJoinColumns = [JoinColumn(name = "tag_id")]
   )
   private val tags: MutableSet<MovementTag?>? = null // getters/setters
}