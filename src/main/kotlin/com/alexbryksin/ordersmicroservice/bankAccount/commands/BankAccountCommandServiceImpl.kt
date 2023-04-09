package com.alexbryksin.ordersmicroservice.bankAccount.commands

import com.alexbryksin.ordersmicroservice.bankAccount.domain.BankAccount
import com.alexbryksin.ordersmicroservice.bankAccount.domain.BankAccountEntity
import com.alexbryksin.ordersmicroservice.bankAccount.domain.OutboxEvent
import com.alexbryksin.ordersmicroservice.bankAccount.domain.of
import com.alexbryksin.ordersmicroservice.bankAccount.events.BalanceDepositedEvent
import com.alexbryksin.ordersmicroservice.bankAccount.events.BalanceWithdrawnEvent
import com.alexbryksin.ordersmicroservice.bankAccount.events.BankAccountCreatedEvent
import com.alexbryksin.ordersmicroservice.bankAccount.events.EmailChangedEvent
import com.alexbryksin.ordersmicroservice.bankAccount.exceptions.BankAccountNotFoundException
import com.alexbryksin.ordersmicroservice.bankAccount.exceptions.UnknownEventTypeException
import com.alexbryksin.ordersmicroservice.bankAccount.repository.BankAccountRepository
import com.alexbryksin.ordersmicroservice.bankAccount.repository.OutboxRepository
import com.alexbryksin.ordersmicroservice.configuration.KafkaTopicsConfiguration
import com.alexbryksin.ordersmicroservice.eventPublisher.EventsPublisher
import com.alexbryksin.ordersmicroservice.utils.serializer.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.time.LocalDateTime
import java.util.*


@Service
class BankAccountCommandServiceImpl(
    private val bankAccountRepository: BankAccountRepository,
    private val outboxRepository: OutboxRepository,
    private val txOp: TransactionalOperator,
    private val eventsPublisher: EventsPublisher,
    private val kafkaTopicsConfiguration: KafkaTopicsConfiguration,
    private val serializer: Serializer
) : BankAccountCommandService {

    override suspend fun on(command: CreateBankAccountCommand): BankAccount = withContext(Dispatchers.IO) {
        val resultPair = txOp.executeAndAwait {
            val bankAccount = BankAccount.of(command)

            val savedBankAccount = bankAccountRepository.save(BankAccountEntity.of(bankAccount))
            bankAccount.id = savedBankAccount.id.toString()
            log.info("saved bank account: $savedBankAccount")

            val bankAccountCreatedEvent = BankAccountCreatedEvent(savedBankAccount.toBankAccount())
            val outboxEvent = createNewOutboxEvent(
                savedBankAccount.id,
                savedBankAccount.version.toLong(),
                bankAccountCreatedEvent,
                BankAccountCreatedEvent.BANK_ACCOUNT_CREATED_EVENT,
            )
            val savedEvent = outboxRepository.save(outboxEvent)
            log.info("saved outbox event: $savedEvent")
            Pair(savedBankAccount, savedEvent)
        }

        publish(resultPair.second)
        resultPair.first.toBankAccount()
    }

    override suspend fun on(command: DepositBalanceCommand): BankAccount = withContext(Dispatchers.IO) {
        val resultPair = txOp.executeAndAwait {
            val bankAccountEntity = bankAccountRepository.findById(UUID.fromString(command.id))
                ?: throw BankAccountNotFoundException(command.id)

            val bankAccount = bankAccountEntity.toBankAccount().depositBalance(command.amount)
            val savedBankAccount = bankAccountRepository.save(BankAccountEntity.of(bankAccount))
            log.info("saved bank account: $savedBankAccount")

            val balanceDepositedEvent = BalanceDepositedEvent(savedBankAccount.id.toString(), command.amount)
            val outboxEvent = createNewOutboxEvent(
                savedBankAccount.id,
                savedBankAccount.version.toLong(),
                balanceDepositedEvent,
                BalanceDepositedEvent.BALANCE_DEPOSITED_EVENT,
            )
            val savedEvent = outboxRepository.save(outboxEvent)
            log.info("saved outbox event: $savedEvent")
            Pair(savedBankAccount, savedEvent)
        }

        publish(resultPair.second)
        resultPair.first.toBankAccount()
    }

    override suspend fun on(command: WithdrawAmountCommand): BankAccount = withContext(Dispatchers.IO) {
        val resultPair = txOp.executeAndAwait {
            val bankAccountEntity =
                bankAccountRepository.findById(UUID.fromString(command.id)) ?: throw BankAccountNotFoundException(command.id)
            val bankAccount = bankAccountEntity.toBankAccount().withdrawBalance(command.amount)
            val savedBankAccount = bankAccountRepository.save(BankAccountEntity.of(bankAccount))
            log.info("saved bank account: $savedBankAccount")

            val balanceWithdrawnEvent = BalanceWithdrawnEvent(savedBankAccount.id.toString(), command.amount)
            val outboxEvent = createNewOutboxEvent(
                savedBankAccount.id,
                savedBankAccount.version.toLong(),
                balanceWithdrawnEvent,
                BalanceWithdrawnEvent.BALANCE_WITHDRAWN_EVENT,
            )
            val savedEvent = outboxRepository.save(outboxEvent)
            log.info("saved outbox event: $savedEvent")
            Pair(savedBankAccount, savedEvent)
        }

        publish(resultPair.second)
        resultPair.first.toBankAccount()
    }

    @Transactional(timeout = 3)
    override suspend fun on(command: ChangeEmailCommand): BankAccount = withContext(Dispatchers.IO) {
        val resultPair = txOp.executeAndAwait {
            val bankAccountEntity = bankAccountRepository.findById(UUID.fromString(command.id))
                ?: throw BankAccountNotFoundException(command.id)
            bankAccountEntity.email = command.newEmail
            val savedBankAccount = bankAccountRepository.save(bankAccountEntity)

            val emailChangedEvent = EmailChangedEvent(savedBankAccount.id.toString(), command.newEmail)
            val outboxEvent = createNewOutboxEvent(
                savedBankAccount.id,
                savedBankAccount.version.toLong(),
                emailChangedEvent,
                EmailChangedEvent.EMAIL_CHANGED_EVENT,
            )
            val savedEvent = outboxRepository.save(outboxEvent)
            log.info("saved outbox event: $savedEvent")

            log.info("publishing outbox event: $savedEvent")
            Pair(savedBankAccount, savedEvent)
        }

        publish(resultPair.second)
        resultPair.first.toBankAccount()
    }


    override suspend fun deleteOutboxRecordsWithLock() =
        outboxRepository.deleteOutboxRecordsWithLock { eventsPublisher.publish(getTopicName(it.eventType), it) }


    private suspend fun publish(event: OutboxEvent) = withContext(Dispatchers.IO) {
        try {
            log.info("publishing event: $event")
            outboxRepository.deleteOutboxRecordByID(event.eventId!!) {
                eventsPublisher.publish(
                    getTopicName(event.eventType),
                    event.aggregateId.toString(),
                    event
                )
            }
            log.info("event published and deleted: $event")
        } catch (e: Exception) {
            log.error("exception while publishing outbox event: ${e.localizedMessage}")
        }
    }

    private fun getTopicName(eventType: String?) = when (eventType) {
        BankAccountCreatedEvent.BANK_ACCOUNT_CREATED_EVENT -> kafkaTopicsConfiguration.bankAccountCreated?.name
        BalanceDepositedEvent.BALANCE_DEPOSITED_EVENT -> kafkaTopicsConfiguration.balanceDeposited?.name
        BalanceWithdrawnEvent.BALANCE_WITHDRAWN_EVENT -> kafkaTopicsConfiguration.balanceWithdrawn?.name
        EmailChangedEvent.EMAIL_CHANGED_EVENT -> kafkaTopicsConfiguration.emailChanged?.name
        else -> throw UnknownEventTypeException(eventType)
    }

    private fun createNewOutboxEvent(aggregateId: UUID?, version: Long, data: Any, eventType: String): OutboxEvent = OutboxEvent(
        eventId = null,
        aggregateId = aggregateId,
        eventType = eventType,
        data = serializer.serializeToBytes(data),
        version = version,
        timestamp = LocalDateTime.now()
    )


    companion object {
        private val log = LoggerFactory.getLogger(BankAccountCommandServiceImpl::class.java)
    }
}