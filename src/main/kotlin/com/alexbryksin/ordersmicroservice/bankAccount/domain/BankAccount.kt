package com.alexbryksin.ordersmicroservice.bankAccount.domain

import com.alexbryksin.ordersmicroservice.bankAccount.commands.CreateBankAccountCommand
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class BankAccount(var id: String? = null) {
    var email: String? = null
    var address: String? = null
    var firstName: String? = null
    var lastName: String? = null
    var phone: String? = null
    var balance: BigDecimal = BigDecimal.ZERO
    var currency: Currency = Currency.USD
    var version: Int = 0
    var createdAt: LocalDateTime? = null
    var updatedAt: LocalDateTime? = null

    fun depositBalance(amount: BigDecimal): BankAccount {
        balance = balance.add(amount)
        return this
    }

    fun withdrawBalance(amount: BigDecimal): BankAccount {
        balance = balance.subtract(amount)
        return this
    }

    fun getFullName() = "$firstName $lastName"
    override fun toString(): String {
        return "BankAccount(id='$id', email=$email, address=$address, firstName=$firstName, lastName=$lastName, phone=$phone, balance=$balance, currency=$currency, version=$version, createdAt=$createdAt, updatedAt=$updatedAt)"
    }

    fun getIdAsUUID(): UUID? {
        if (id == null) return id
        return UUID.fromString(id)
    }

    companion object
}


fun BankAccount.Companion.of(createBankAccountCommand: CreateBankAccountCommand): BankAccount {
    return BankAccount().apply {
        email = createBankAccountCommand.email
        address = createBankAccountCommand.address
        firstName = createBankAccountCommand.firstName
        lastName = createBankAccountCommand.lastName
        phone = createBankAccountCommand.phone
        balance = createBankAccountCommand.balance
        currency = createBankAccountCommand.currency
//        version = 1
    }
}

