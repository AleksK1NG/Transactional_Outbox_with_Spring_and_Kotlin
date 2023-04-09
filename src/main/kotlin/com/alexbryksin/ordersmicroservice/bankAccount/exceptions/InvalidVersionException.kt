package com.alexbryksin.ordersmicroservice.bankAccount.exceptions

data class InvalidVersionException(val id: String, val version: Int) : RuntimeException("invalid version: $version for account with id: $id")
