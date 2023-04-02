package com.alexbryksin.ordersmicroservice.bank_account.commands

import com.alexbryksin.ordersmicroservice.bank_account.domain.BankAccount
import com.alexbryksin.ordersmicroservice.bank_account.domain.BankAccountEntity
import com.alexbryksin.ordersmicroservice.bank_account.domain.of
import com.alexbryksin.ordersmicroservice.bank_account.exceptions.BankAccountNotFoundException
import com.alexbryksin.ordersmicroservice.bank_account.repository.BankAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*


@Service
class BankAccountCommandServiceImpl(private val bankAccountRepository: BankAccountRepository) : BankAccountCommandService {


    @Transactional(timeout = 3)
    override suspend fun on(command: CreateBankAccountCommand): BankAccount = withContext(Dispatchers.IO) {
        val bankAccount = BankAccount.of(command)
        val savedBankAccount = bankAccountRepository.save(BankAccountEntity.of(bankAccount))
        log.info("saved bank account: $savedBankAccount")
        savedBankAccount.toBankAccount()
    }


//    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 3)
//    override suspend fun on(command: DepositBalanceCommand): Long = withContext(Dispatchers.IO) {
//        val bankAccountEntity =
//            bankAccountRepository.findById(UUID.fromString(command.id)) ?: throw BankAccountNotFoundException(command.id)
//        val bankAccount = bankAccountEntity.toBankAccount().depositBalance(command.amount)
//        bankAccountRepository.save(BankAccountEntity.of(bankAccount)).toBankAccount()
//        1
//    }

    override suspend fun on(command: DepositBalanceCommand): Long = withContext(Dispatchers.IO) {
       bankAccountRepository.updateBalance(command.id, command.amount)
    }

    @Transactional(timeout = 3)
    override suspend fun on(command: WithdrawAmountCommand): BankAccount = withContext(Dispatchers.IO) {
        val bankAccountEntity =
            bankAccountRepository.findById(UUID.fromString(command.id)) ?: throw BankAccountNotFoundException(command.id)
        val bankAccount = bankAccountEntity.toBankAccount().withdrawBalance(command.amount)
        bankAccountRepository.save(BankAccountEntity.of(bankAccount)).toBankAccount()
    }

    @Transactional(timeout = 3)
    override suspend fun on(command: ChangeEmailCommand): BankAccount = withContext(Dispatchers.IO) {
        val bankAccountEntity =
            bankAccountRepository.findById(UUID.fromString(command.id)) ?: throw BankAccountNotFoundException(command.id)
        bankAccountEntity.email = command.newEmail
        bankAccountRepository.save(bankAccountEntity).toBankAccount()
    }

    companion object {
        private val log = LoggerFactory.getLogger(BankAccountCommandServiceImpl::class.java)
    }
}