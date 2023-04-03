package com.alexbryksin.ordersmicroservice.bank_account.events

import com.alexbryksin.ordersmicroservice.bank_account.domain.BankAccount

data class BankAccountCreatedEvent(val bankAccount: BankAccount) {
    companion object {
        const val BANK_ACCOUNT_CREATED_EVENT = "BANK_ACCOUNT_CREATED_EVENT"
    }
}
