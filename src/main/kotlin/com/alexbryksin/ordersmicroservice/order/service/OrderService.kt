package com.alexbryksin.ordersmicroservice.order.service

import com.alexbryksin.ordersmicroservice.order.domain.Order
import com.alexbryksin.ordersmicroservice.order.domain.ProductItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.*

interface OrderService {

    suspend fun createOrder(order: Order): Order
    suspend fun getOrderByID(id: UUID): Order
    suspend fun addProductItem(productItem: ProductItem)
    suspend fun removeProductItem(orderID: UUID, productItemId: UUID)
    suspend fun pay(id: UUID, paymentId: String): Order
    suspend fun cancel(id: UUID, reason: String?): Order
    suspend fun submit(id: UUID): Order
    suspend fun complete(id: UUID): Order

    suspend fun getOrderWithProductsByID(id: UUID): Order
    suspend fun getAllOrders(pageable: Pageable): Page<Order>

    suspend fun deleteOutboxRecordsWithLock()
}