package com.alexbryksin.ordersmicroservice.bank_account.commands

import com.alexbryksin.ordersmicroservice.bank_account.domain.BankAccount
import com.alexbryksin.ordersmicroservice.bank_account.domain.BankAccountEntity
import com.alexbryksin.ordersmicroservice.bank_account.domain.OutboxEvent
import com.alexbryksin.ordersmicroservice.bank_account.domain.of
import com.alexbryksin.ordersmicroservice.bank_account.events.BalanceDepositedEvent
import com.alexbryksin.ordersmicroservice.bank_account.events.BalanceWithdrawnEvent
import com.alexbryksin.ordersmicroservice.bank_account.events.BankAccountCreatedEvent
import com.alexbryksin.ordersmicroservice.bank_account.events.EmailChangedEvent
import com.alexbryksin.ordersmicroservice.bank_account.exceptions.BankAccountNotFoundException
import com.alexbryksin.ordersmicroservice.bank_account.repository.BankAccountRepository
import com.alexbryksin.ordersmicroservice.bank_account.repository.OutboxRepository
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*


@Service
class BankAccountCommandServiceImpl(
    private val bankAccountRepository: BankAccountRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
) : BankAccountCommandService {


    @Transactional(timeout = 3)
    override suspend fun on(command: CreateBankAccountCommand): BankAccount = withContext(Dispatchers.IO) {
        val bankAccount = BankAccount.of(command)
        val savedBankAccount = bankAccountRepository.save(BankAccountEntity.of(bankAccount))
        log.info("saved bank account: $savedBankAccount")

        val bankAccountCreatedEvent = BankAccountCreatedEvent(savedBankAccount.toBankAccount())
        val eventData = objectMapper.writeValueAsBytes(bankAccountCreatedEvent)
        val outboxEvent = OutboxEvent(
            null,
            savedBankAccount.id,
            BankAccountCreatedEvent.BANK_ACCOUNT_CREATED_EVENT,
            eventData,
            savedBankAccount.version.toLong(),
            LocalDateTime.now()
        )
        val savedEvent = outboxRepository.save(outboxEvent)
        log.info("saved outbox event: $savedEvent")

        log.info("publishing outbox event: $savedEvent")
        savedBankAccount.toBankAccount()
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 3)
    override suspend fun on(command: DepositBalanceCommand): Long = withContext(Dispatchers.IO) {
        val bankAccountEntity = bankAccountRepository.findById(UUID.fromString(command.id))
            ?: throw BankAccountNotFoundException(command.id)
        val bankAccount = bankAccountEntity.toBankAccount().depositBalance(command.amount)
        val savedBankAccount = bankAccountRepository.save(BankAccountEntity.of(bankAccount))
        log.info("saved bank account: $savedBankAccount")

        val balanceDepositedEvent = BalanceDepositedEvent(savedBankAccount.id.toString(), command.amount)
        val eventData = objectMapper.writeValueAsBytes(balanceDepositedEvent)
        val outboxEvent =
            OutboxEvent(null, savedBankAccount.id, BalanceDepositedEvent.BALANCE_DEPOSITED_EVENT, eventData, savedBankAccount.version.toLong(), LocalDateTime.now())
        val savedEvent = outboxRepository.save(outboxEvent)
        log.info("saved outbox event: $savedEvent")

        log.info("publishing outbox event: $savedEvent")
        1
    }

//    override suspend fun on(command: DepositBalanceCommand): Long = withContext(Dispatchers.IO) {
//        bankAccountRepository.updateBalance(command.id, command.amount)
//    }

    @Transactional(timeout = 3)
    override suspend fun on(command: WithdrawAmountCommand): BankAccount = withContext(Dispatchers.IO) {
        val bankAccountEntity =
            bankAccountRepository.findById(UUID.fromString(command.id)) ?: throw BankAccountNotFoundException(command.id)
        val bankAccount = bankAccountEntity.toBankAccount().withdrawBalance(command.amount)
        val savedBankAccount = bankAccountRepository.save(BankAccountEntity.of(bankAccount))
        log.info("saved bank account: $savedBankAccount")

        val balanceWithdrawnEvent = BalanceWithdrawnEvent(savedBankAccount.id.toString(), command.amount)
        val eventData = objectMapper.writeValueAsBytes(balanceWithdrawnEvent)
        val outboxEvent =
            OutboxEvent(null, savedBankAccount.id, BalanceWithdrawnEvent.BALANCE_WITHDRAWN_EVENT, eventData, savedBankAccount.version.toLong(), LocalDateTime.now())
        val savedEvent = outboxRepository.save(outboxEvent)
        log.info("saved outbox event: $savedEvent")

        log.info("publishing outbox event: $savedEvent")
        savedBankAccount.toBankAccount()
    }

    @Transactional(timeout = 3)
    override suspend fun on(command: ChangeEmailCommand): BankAccount = withContext(Dispatchers.IO) {
        val bankAccountEntity = bankAccountRepository.findById(UUID.fromString(command.id))
            ?: throw BankAccountNotFoundException(command.id)
        bankAccountEntity.email = command.newEmail
        val savedBankAccount = bankAccountRepository.save(bankAccountEntity)

        val emailChangedEvent = EmailChangedEvent(savedBankAccount.id.toString(), command.newEmail)
        val eventData = objectMapper.writeValueAsBytes(emailChangedEvent)
        val outboxEvent =
            OutboxEvent(null, savedBankAccount.id, EmailChangedEvent.EMAIL_CHANGED_EVENT, eventData, savedBankAccount.version.toLong(), LocalDateTime.now())
        val savedEvent = outboxRepository.save(outboxEvent)
        log.info("saved outbox event: $savedEvent")

        log.info("publishing outbox event: $savedEvent")
        savedBankAccount.toBankAccount()
    }

    companion object {
        private val log = LoggerFactory.getLogger(BankAccountCommandServiceImpl::class.java)
    }
}