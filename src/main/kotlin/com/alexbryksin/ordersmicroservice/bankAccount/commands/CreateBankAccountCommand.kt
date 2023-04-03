package com.alexbryksin.ordersmicroservice.bankAccount.commands

import com.alexbryksin.ordersmicroservice.bankAccount.domain.Currency
import java.math.BigDecimal

data class CreateBankAccountCommand(
    var email: String,
    val address: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val balance: BigDecimal = BigDecimal.ZERO,
    val currency: Currency = Currency.USD
) {
    companion object
}


