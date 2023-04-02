package com.alexbryksin.ordersmicroservice.bank_account.commands

import com.alexbryksin.ordersmicroservice.bank_account.domain.Currency
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


