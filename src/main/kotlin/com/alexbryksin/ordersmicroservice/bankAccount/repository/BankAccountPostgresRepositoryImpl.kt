package com.alexbryksin.ordersmicroservice.bankAccount.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.core.FluentR2dbcOperations
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.math.BigDecimal
import java.util.*


@Repository
class BankAccountPostgresRepositoryImpl(
    private val r2dbcOps: FluentR2dbcOperations,
    private val dbClient: DatabaseClient,
    private val tx: ReactiveTransactionManager,
    private val txOp: TransactionalOperator
) : BankAccountPostgresRepository {

    override suspend fun updateBalance(id: String, amount: BigDecimal): Long = withContext(Dispatchers.IO) {
        txOp.executeAndAwait {
            val result =
                dbClient.sql("UPDATE microservices.bank_accounts SET balance = (balance + :amount), version = version + 1 WHERE id = :id AND version = version")
                    .bind("amount", amount)
                    .bind("id", UUID.fromString(id))
                    .fetch()
                    .rowsUpdated()
                    .awaitSingle()

            log.info("result: $result")
            result
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(BankAccountPostgresRepositoryImpl::class.java)
    }
}