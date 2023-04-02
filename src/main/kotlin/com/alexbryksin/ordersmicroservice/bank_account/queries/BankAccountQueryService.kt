package com.alexbryksin.ordersmicroservice.bank_account.queries

import com.alexbryksin.ordersmicroservice.bank_account.domain.BankAccount

interface BankAccountQueryService {
    suspend fun on(getBankAccountByIdQuery: GetBankAccountByIdQuery): BankAccount
}