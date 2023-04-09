package com.alexbryksin.ordersmicroservice.bankAccount.repository

import com.alexbryksin.ordersmicroservice.bankAccount.domain.BankAccountDocument
import org.springframework.stereotype.Repository


@Repository
interface BankAccountMongoCoroutineRepository {
    suspend fun findByID(id: String): BankAccountDocument
    suspend fun insert(bankAccountDocument: BankAccountDocument): BankAccountDocument
    suspend fun update(bankAccountDocument: BankAccountDocument): BankAccountDocument
    suspend fun deleteByID(id: String)
}