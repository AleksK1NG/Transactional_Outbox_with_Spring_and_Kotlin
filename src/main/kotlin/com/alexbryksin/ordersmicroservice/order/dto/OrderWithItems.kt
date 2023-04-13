package com.alexbryksin.ordersmicroservice.order.dto

import java.util.*

interface OrderWithItems {
    var orderId: UUID
    var email: String
    var itemId: UUID
    var title: String
}