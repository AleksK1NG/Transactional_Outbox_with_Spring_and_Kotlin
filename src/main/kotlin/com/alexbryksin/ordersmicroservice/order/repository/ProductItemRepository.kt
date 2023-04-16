package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.ProductItemEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

interface ProductItemRepository : CoroutineCrudRepository<ProductItemEntity, UUID>, ProductItemBaseRepository {
}