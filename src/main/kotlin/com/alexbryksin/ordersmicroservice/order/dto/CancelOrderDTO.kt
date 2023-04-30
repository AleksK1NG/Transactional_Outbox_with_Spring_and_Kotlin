package com.alexbryksin.ordersmicroservice.order.dto

import jakarta.validation.constraints.Size

data class CancelOrderDTO(@field:Size(min = 6, max = 1000) val reason: String? = null)
