package com.alexbryksin.ordersmicroservice.bankAccount.commands

data class ChangeEmailCommand(val id: String, val newEmail: String) {
}