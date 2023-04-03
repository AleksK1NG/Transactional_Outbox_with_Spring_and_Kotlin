package com.alexbryksin.ordersmicroservice.bankAccount.queries

import com.alexbryksin.ordersmicroservice.bankAccount.domain.BankAccount
import com.alexbryksin.ordersmicroservice.bankAccount.repository.BankAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.data.relational.core.sql.LockMode
import org.springframework.data.relational.repository.Lock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*


@Service
class BankAccountQueryServiceImpl(private val bankAccountRepository: BankAccountRepository) : BankAccountQueryService {


    @Transactional
    @Lock(LockMode.PESSIMISTIC_WRITE)
    override suspend fun on(getBankAccountByIdQuery: GetBankAccountByIdQuery): BankAccount = withContext(Dispatchers.IO) {
        val foundAccount = bankAccountRepository.findById(UUID.fromString(getBankAccountByIdQuery.id))
            ?: throw RuntimeException("bank account not found fpr id: ${getBankAccountByIdQuery.id}")
        log.info("found account: $foundAccount")
        foundAccount.toBankAccount()
    }

    companion object {
        private val log = LoggerFactory.getLogger(BankAccountQueryServiceImpl::class.java)
    }
}