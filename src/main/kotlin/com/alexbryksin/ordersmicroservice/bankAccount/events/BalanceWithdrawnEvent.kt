package com.alexbryksin.ordersmicroservice.bankAccount.events

import java.math.BigDecimal

data class BalanceWithdrawnEvent(val bankAccountId: String, val amount: BigDecimal) {
    companion object {
        const val BALANCE_WITHDRAWN_EVENT = "BALANCE_WITHDRAWN_EVENT"
    }
}
