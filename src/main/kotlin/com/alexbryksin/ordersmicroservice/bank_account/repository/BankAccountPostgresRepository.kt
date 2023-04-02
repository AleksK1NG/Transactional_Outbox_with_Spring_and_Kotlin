package com.alexbryksin.ordersmicroservice.bank_account.repository

import org.springframework.stereotype.Repository
import java.math.BigDecimal


@Repository
interface BankAccountPostgresRepository {
    suspend fun updateBalance(id: String, amount: BigDecimal): Long
}