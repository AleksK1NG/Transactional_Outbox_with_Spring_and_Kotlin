package com.alexbryksin.ordersmicroservice.bank_account.repository

import com.alexbryksin.ordersmicroservice.bank_account.domain.BankAccountEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.relational.core.sql.LockMode
import org.springframework.data.relational.repository.Lock
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.*

@Repository
interface BankAccountRepository : CoroutineCrudRepository<BankAccountEntity, UUID>, BankAccountPostgresRepository {

    @Lock(LockMode.PESSIMISTIC_WRITE)
    @Modifying
    @Query("UPDATE microservices.bank_accounts ba SET balance = :balance WHERE id = :id RETURNING *")
    suspend fun updateBalanceById(@Param("balance") balance: BigDecimal, @Param("id") id: UUID): Long
}