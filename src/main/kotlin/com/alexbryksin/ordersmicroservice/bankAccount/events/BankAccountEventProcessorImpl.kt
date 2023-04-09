package com.alexbryksin.ordersmicroservice.bankAccount.events

import com.alexbryksin.ordersmicroservice.bankAccount.domain.BankAccountDocument
import com.alexbryksin.ordersmicroservice.bankAccount.domain.of
import com.alexbryksin.ordersmicroservice.bankAccount.exceptions.InvalidVersionException
import com.alexbryksin.ordersmicroservice.bankAccount.repository.BankAccountsMongoRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class BankAccountEventProcessorImpl(private val bankAccountsMongoRepository: BankAccountsMongoRepository) : BankAccountEventProcessor {

    override suspend fun on(event: BankAccountCreatedEvent) {
        bankAccountsMongoRepository.insert(BankAccountDocument.of(event.bankAccount))
            .also { log.info("created bank account") }
    }

    override suspend fun on(event: BalanceDepositedEvent) {
        val bankAccount = bankAccountsMongoRepository.findByID(event.bankAccountId)
        if (bankAccount.version != event.version - 1) throw InvalidVersionException(bankAccount.id!!, event.version)
        bankAccount.depositBalance(event.amount)
        bankAccount.version = event.version
        bankAccountsMongoRepository.update(bankAccount)
    }

    override suspend fun on(event: BalanceWithdrawnEvent) {
        val bankAccount = bankAccountsMongoRepository.findByID(event.bankAccountId)
        if (bankAccount.version != event.version - 1) throw InvalidVersionException(bankAccount.id!!, event.version)
        bankAccount.withdrawBalance(event.amount)
        bankAccount.version = event.version
        bankAccountsMongoRepository.update(bankAccount)
    }

    override suspend fun on(event: EmailChangedEvent) {
        val bankAccount = bankAccountsMongoRepository.findByID(event.bankAccountId)
        if (bankAccount.version != event.version - 1) throw InvalidVersionException(bankAccount.id!!, event.version)
        bankAccount.email = event.newEmail
        bankAccount.version = event.version
        bankAccountsMongoRepository.update(bankAccount)
    }

    companion object {
        private val log = LoggerFactory.getLogger(BankAccountsMongoRepository::class.java)
    }
}