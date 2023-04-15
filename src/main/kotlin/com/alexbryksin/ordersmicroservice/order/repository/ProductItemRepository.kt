package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.ProductItem
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

interface ProductItemRepository : CoroutineCrudRepository<ProductItem, UUID>, ProductItemBaseRepository {
}