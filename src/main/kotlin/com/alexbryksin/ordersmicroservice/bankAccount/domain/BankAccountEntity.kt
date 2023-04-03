package com.alexbryksin.ordersmicroservice.bankAccount.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*


@Table(schema = "microservices", name = "bank_accounts")
data class BankAccountEntity(
    @Id @Column("id") var id: UUID?,
    @Column("email") var email: String?,
    @Column("address") var address: String? = null,
    @Column("first_name") var firstName: String? = null,
    @Column("last_name") var lastName: String? = null,
    @Column("phone") var phone: String? = null,
    @Column("balance") var balance: BigDecimal = BigDecimal.ZERO,
    @Column("currency") var currency: Currency = Currency.USD,
    @Version @Column("version") var version: Int = 0,
    @CreatedDate @Column("created_at") var createdAt: LocalDateTime? = null,
    @LastModifiedDate @Column("updated_at") var updatedAt: LocalDateTime? = null
) {
    companion object

    fun toBankAccount(): BankAccount {
        val bankAccount = BankAccount(id.toString())
        bankAccount.email = email
        bankAccount.address = address
        bankAccount.firstName = firstName
        bankAccount.lastName = lastName
        bankAccount.phone = phone
        bankAccount.balance = balance
        bankAccount.currency = currency
        bankAccount.version = version
        bankAccount.createdAt = createdAt
        bankAccount.updatedAt = updatedAt
        return bankAccount
    }
}

fun BankAccountEntity.Companion.of(bankAccount: BankAccount): BankAccountEntity {

    return BankAccountEntity(
        id = bankAccount.getIdAsUUID(),
        email = bankAccount.email,
        address = bankAccount.address,
        firstName = bankAccount.firstName,
        lastName = bankAccount.lastName,
        phone = bankAccount.phone,
        balance = bankAccount.balance,
        currency = bankAccount.currency,
        version = bankAccount.version,
        createdAt = bankAccount.createdAt,
        updatedAt = bankAccount.updatedAt
    )
}