package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.OrderEntity
import com.alexbryksin.ordersmicroservice.order.dto.OrderBaseProjection
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderRepository : CoroutineCrudRepository<OrderEntity, UUID>, OrderBaseRepository {

    @Query("""SELECT id, email FROM microservices.orders WHERE id = :orderId""")
    fun getOrderWithItemsByID(@Param("orderId") id: UUID): Flow<OrderBaseProjection>
}