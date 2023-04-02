package com.alexbryksin.ordersmicroservice.bank_account.domain

import java.math.BigDecimal
import java.time.LocalDateTime

class BankAccount(val id: String) {
    var email: String? = null
    var address: String? = null
    var firstName: String? = null
    var lastName: String? = null
    var phone: String? = null
    var balance: BigDecimal = BigDecimal.ZERO
    var currency: Currency = Currency.UDS
    var version: Int = 0
    var createdAt: LocalDateTime? = null
    var updatedAt: LocalDateTime? = null

    fun depositBalance(amount: BigDecimal) {
        balance = balance.add(amount)
    }

    fun withdrawBalance(amount: BigDecimal) {
        balance = balance.add(amount)
    }

    fun getFullName() = "$firstName $lastName"
    override fun toString(): String {
        return "BankAccount(id='$id', email=$email, address=$address, firstName=$firstName, lastName=$lastName, phone=$phone, balance=$balance, currency=$currency, version=$version, createdAt=$createdAt, updatedAt=$updatedAt)"
    }
}