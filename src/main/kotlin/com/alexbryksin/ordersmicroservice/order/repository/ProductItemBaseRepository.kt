package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.ProductItem

interface ProductItemBaseRepository {

    suspend fun insert(productItem: ProductItem): ProductItem
    suspend fun insertAll(productItems: List<ProductItem>): List<ProductItem>
}