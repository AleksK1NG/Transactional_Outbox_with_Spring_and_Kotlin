package com.alexbryksin.ordersmicroservice.bank_account.exceptions

data class BankAccountNotFoundException(val id: String) : RuntimeException("bank account with id: $id not found")
