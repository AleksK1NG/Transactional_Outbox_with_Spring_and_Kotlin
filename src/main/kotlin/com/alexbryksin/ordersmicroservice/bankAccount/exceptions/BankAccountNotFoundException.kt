package com.alexbryksin.ordersmicroservice.bankAccount.exceptions

data class BankAccountNotFoundException(val id: String) : RuntimeException("bank account with id: $id not found")
