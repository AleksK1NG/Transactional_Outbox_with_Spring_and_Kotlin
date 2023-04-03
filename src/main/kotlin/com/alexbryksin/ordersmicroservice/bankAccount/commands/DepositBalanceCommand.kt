package com.alexbryksin.ordersmicroservice.bankAccount.commands

import java.math.BigDecimal

data class DepositBalanceCommand(
    val id: String = "",
    val amount: BigDecimal
)
