package com.alexbryksin.ordersmicroservice.bankAccount.commands

import com.alexbryksin.ordersmicroservice.bankAccount.domain.BankAccount


interface BankAccountCommandService {
    suspend fun on(command: CreateBankAccountCommand): BankAccount
    suspend fun on(command: DepositBalanceCommand): BankAccount
    suspend fun on(command: WithdrawAmountCommand): BankAccount
    suspend fun on(command: ChangeEmailCommand): BankAccount
}