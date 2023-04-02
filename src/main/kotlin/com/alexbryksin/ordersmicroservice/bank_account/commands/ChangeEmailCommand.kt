package com.alexbryksin.ordersmicroservice.bank_account.commands

data class ChangeEmailCommand(val id: String, val newEmail: String) {
}