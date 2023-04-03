package com.alexbryksin.ordersmicroservice.bankAccount.events

import java.math.BigDecimal

data class BalanceDepositedEvent(val bankAccountId: String, val amount: BigDecimal) {
    companion object {
        const val BALANCE_DEPOSITED_EVENT = "BALANCE_DEPOSITED_EVENT"
    }
}
