package com.alexbryksin.ordersmicroservice.bank_account.controllers

import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(path = ["/api/v1/account"])
class BankAccountController {

    @GetMapping
    suspend fun getBankAccountByID() = coroutineScope {
        ResponseEntity.status(200).body("OK").also { log.info("response: $it") }
    }


    companion object {
        private val log = LoggerFactory.getLogger(BankAccountController::class.java)
    }
}