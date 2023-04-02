package com.alexbryksin.ordersmicroservice.bank_account.commands

import java.math.BigDecimal

data class DepositBalanceCommand(
    val id: String = "",
    val amount: BigDecimal
)
