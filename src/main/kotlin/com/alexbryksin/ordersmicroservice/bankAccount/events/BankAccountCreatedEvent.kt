package com.alexbryksin.ordersmicroservice.bankAccount.events

import com.alexbryksin.ordersmicroservice.bankAccount.domain.BankAccount

data class BankAccountCreatedEvent(val bankAccount: BankAccount) {
    companion object {
        const val BANK_ACCOUNT_CREATED_EVENT = "BANK_ACCOUNT_CREATED_EVENT"
    }
}
