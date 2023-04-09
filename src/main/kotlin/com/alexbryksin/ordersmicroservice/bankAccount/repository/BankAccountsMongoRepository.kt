package com.alexbryksin.ordersmicroservice.bankAccount.repository

import com.alexbryksin.ordersmicroservice.bankAccount.domain.BankAccountDocument
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository


@Repository
interface BankAccountsMongoRepository : ReactiveMongoRepository<BankAccountDocument, String>, BankAccountMongoCoroutineRepository {
}