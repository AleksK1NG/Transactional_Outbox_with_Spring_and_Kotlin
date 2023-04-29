package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.ProductItemEntity
import com.alexbryksin.ordersmicroservice.utils.tracing.coroutineScopeWithObservation
import io.micrometer.observation.ObservationRegistry
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.stereotype.Repository


@Repository
class ProductItemBaseRepositoryImpl(
    private val entityTemplate: R2dbcEntityTemplate,
    private val or: ObservationRegistry,
) : ProductItemBaseRepository {

    override suspend fun insert(productItemEntity: ProductItemEntity): ProductItemEntity = coroutineScopeWithObservation(INSERT, or) { observation ->
        val product = entityTemplate.insert(productItemEntity).awaitSingle()

        log.info("saved product: $product")
        observation.highCardinalityKeyValue("product", product.toString())
        product
    }

    override suspend fun insertAll(productItemEntities: List<ProductItemEntity>) = coroutineScopeWithObservation(INSERT_ALL, or) { observation ->
        val result = productItemEntities.map { entityTemplate.insert(it) }.map { it.awaitSingle() }
        log.info("inserted product items: $result")
        observation.highCardinalityKeyValue("result", result.toString())
        result
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProductItemBaseRepositoryImpl::class.java)

        private const val INSERT = "ProductItemBaseRepository.insert"
        private const val INSERT_ALL = "ProductItemBaseRepository.insertAll"
    }
}