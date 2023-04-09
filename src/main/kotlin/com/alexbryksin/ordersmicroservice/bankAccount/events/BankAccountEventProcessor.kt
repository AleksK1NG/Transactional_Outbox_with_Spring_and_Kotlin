package com.alexbryksin.ordersmicroservice.bankAccount.events

interface BankAccountEventProcessor {
    suspend fun on(event: BankAccountCreatedEvent)
    suspend fun on(event: BalanceDepositedEvent)
    suspend fun on(event: BalanceWithdrawnEvent)
    suspend fun on(event: EmailChangedEvent)
}