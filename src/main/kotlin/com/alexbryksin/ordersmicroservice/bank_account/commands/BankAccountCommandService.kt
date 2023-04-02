package com.alexbryksin.ordersmicroservice.bank_account.commands

import com.alexbryksin.ordersmicroservice.bank_account.domain.BankAccount


interface BankAccountCommandService {
    suspend fun on(command: CreateBankAccountCommand): BankAccount
    suspend fun on(command: DepositBalanceCommand): Long
    suspend fun on(command: WithdrawAmountCommand): BankAccount
    suspend fun on(command: ChangeEmailCommand): BankAccount
}