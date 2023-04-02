package com.alexbryksin.ordersmicroservice.bank_account.dto

import com.alexbryksin.ordersmicroservice.bank_account.domain.BankAccount
import com.alexbryksin.ordersmicroservice.bank_account.domain.Currency
import java.math.BigDecimal

data class BankAccountSuccessResponse(
    val id: String?,
    var email: String? = null,
    var address: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var phone: String? = null,
    var balance: BigDecimal = BigDecimal.ZERO,
    var currency: Currency = Currency.USD,
    var version: Int = 0,
    var createdAt: String? = null,
    var updatedAt: String? = null,
) {
    companion object
}

fun BankAccountSuccessResponse.Companion.of(bankAccount: BankAccount): BankAccountSuccessResponse {
    return BankAccountSuccessResponse(
        id = bankAccount.id,
        email = bankAccount.email,
        address = bankAccount.address,
        firstName = bankAccount.firstName,
        lastName = bankAccount.lastName,
        phone = bankAccount.phone,
        balance = bankAccount.balance,
        currency = bankAccount.currency,
        version = bankAccount.version,
        createdAt = bankAccount.createdAt.toString(),
        updatedAt = bankAccount.updatedAt.toString()
    )
}
