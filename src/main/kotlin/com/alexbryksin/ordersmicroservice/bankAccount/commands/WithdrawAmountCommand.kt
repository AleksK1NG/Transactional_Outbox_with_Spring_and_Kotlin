package com.alexbryksin.ordersmicroservice.bankAccount.commands

import java.math.BigDecimal

data class WithdrawAmountCommand(
    val id: String = "",
    val amount: BigDecimal
)
