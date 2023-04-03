package com.alexbryksin.ordersmicroservice.bank_account.events

import java.math.BigDecimal

data class EmailChangedEvent(val bankAccountId: String, val newEmail: String) {
    companion object {
        const val EMAIL_CHANGED_EVENT = "EMAIL_CHANGED_EVENT"
    }
}