package com.alexbryksin.ordersmicroservice.bankAccount.events

data class EmailChangedEvent(val bankAccountId: String, val newEmail: String) {
    companion object {
        const val EMAIL_CHANGED_EVENT = "EMAIL_CHANGED_EVENT"
    }
}