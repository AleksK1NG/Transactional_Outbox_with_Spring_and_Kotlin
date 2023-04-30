package com.alexbryksin.ordersmicroservice.order.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PayOrderDTO(@field:NotBlank @field:Size(min = 6, max = 250) val paymentId: String)
