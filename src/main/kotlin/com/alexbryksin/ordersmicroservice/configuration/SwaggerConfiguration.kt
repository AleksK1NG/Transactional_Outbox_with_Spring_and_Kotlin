package com.alexbryksin.ordersmicroservice.configuration

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.context.annotation.Configuration


@OpenAPIDefinition(
    info = Info(
        title = "Kotlin Spring Outbox Microservice",
        description = "Kotlin Spring Outbox Microservice example",
        version = "1.0.0",
        contact = Contact(
            name = "Alexander Bryksin",
            email = "alexander.bryksin@yandex.ru",
            url = "https://github.com/AleksK1NG"
        )
    )
)
@Configuration
class SwaggerConfiguration {
}