package com.alexbryksin.ordersmicroservice.bankAccount.controllers

import com.alexbryksin.ordersmicroservice.bankAccount.commands.*
import com.alexbryksin.ordersmicroservice.bankAccount.dto.BankAccountSuccessResponse
import com.alexbryksin.ordersmicroservice.bankAccount.dto.of
import com.alexbryksin.ordersmicroservice.bankAccount.queries.BankAccountQueryService
import com.alexbryksin.ordersmicroservice.bankAccount.queries.GetBankAccountByIdQuery
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping(path = ["/api/v1/account"])
class BankAccountController(
    private val bankAccountCommandService: BankAccountCommandService,
    private val bankAccountQueryService: BankAccountQueryService
) {

    @PostMapping
    suspend fun createBankAccount(@RequestBody command: CreateBankAccountCommand) = coroutineScope {
        bankAccountCommandService.on(command).also { log.info("response: $it") }
    }

    @PutMapping(path = ["/deposit/{id}"])
    suspend fun depositBalance(@PathVariable id: String, @RequestBody command: DepositBalanceCommand) = coroutineScope {
        ResponseEntity.status(200).body(bankAccountCommandService.on(command.copy(id = id)))
            .also { log.info("PUT deposit balance response: $it") }
    }

    @PutMapping(path = ["/withdraw/{id}"])
    suspend fun withdrawBalance(@PathVariable id: String, @RequestBody command: WithdrawAmountCommand) = coroutineScope {
        ResponseEntity.status(HttpStatus.OK).body(BankAccountSuccessResponse.of(bankAccountCommandService.on(command.copy(id = id))))
            .also { log.info("PUT withdraw balance response: $it") }
    }

    @PutMapping(path = ["/email/{id}"])
    suspend fun changeEmail(@PathVariable id: String, @RequestBody command: ChangeEmailCommand) = coroutineScope {
        ResponseEntity.status(HttpStatus.OK).body(BankAccountSuccessResponse.of(bankAccountCommandService.on(command.copy(id = id))))
            .also { log.info("PUT change email response: $it") }
    }

    @GetMapping(path = ["{id}"])
    suspend fun getBankAccountByID(@PathVariable id: String) = coroutineScope {
        ResponseEntity.status(HttpStatus.OK).body(BankAccountSuccessResponse.of(bankAccountQueryService.on(GetBankAccountByIdQuery(id))))
            .also { log.info("GET bankAccount by id response: $it") }
    }


    companion object {
        private val log = LoggerFactory.getLogger(BankAccountController::class.java)
    }
}