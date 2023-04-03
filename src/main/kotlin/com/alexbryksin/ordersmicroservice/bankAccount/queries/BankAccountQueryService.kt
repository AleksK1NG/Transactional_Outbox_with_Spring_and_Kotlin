package com.alexbryksin.ordersmicroservice.bankAccount.queries

import com.alexbryksin.ordersmicroservice.bankAccount.domain.BankAccount

interface BankAccountQueryService {
    suspend fun on(getBankAccountByIdQuery: GetBankAccountByIdQuery): BankAccount
}