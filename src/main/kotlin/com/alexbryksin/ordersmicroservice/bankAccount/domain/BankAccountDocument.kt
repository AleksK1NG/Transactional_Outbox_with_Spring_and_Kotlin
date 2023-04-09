package com.alexbryksin.ordersmicroservice.bankAccount.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*


@Document(collection = "bankAccounts")
data class BankAccountDocument(
    @Id @Field(name = "id") var id: UUID?,
    @Field("email") var email: String?,
    @Field("address") var address: String? = null,
    @Field("firstName") var firstName: String? = null,
    @Field("lastName") var lastName: String? = null,
    @Field("phone") var phone: String? = null,
    @Field("balance") var balance: BigDecimal = BigDecimal.ZERO,
    @Field("currency") var currency: Currency = Currency.USD,
    @Version @Field("version") var version: Int = 0,
    @CreatedDate @Field("createdAt") var createdAt: LocalDateTime? = null,
    @LastModifiedDate @Field("updatedAt") var updatedAt: LocalDateTime? = null
) {
}