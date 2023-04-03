package com.alexbryksin.ordersmicroservice.bankAccount.repository

import org.springframework.stereotype.Repository
import java.math.BigDecimal


@Repository
interface BankAccountPostgresRepository {
    suspend fun updateBalance(id: String, amount: BigDecimal): Long
}