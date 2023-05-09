package com.alexbryksin.ordersmicroservice.order.repository

import com.alexbryksin.ordersmicroservice.order.domain.ProductItem
import com.alexbryksin.ordersmicroservice.order.domain.ProductItemEntity
import com.alexbryksin.ordersmicroservice.order.domain.of
import com.alexbryksin.ordersmicroservice.utils.tracing.coroutineScopeWithObservation
import io.micrometer.observation.ObservationRegistry
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*


@Repository
class ProductItemBaseRepositoryImpl(
    private val entityTemplate: R2dbcEntityTemplate,
    private val or: ObservationRegistry,
) : ProductItemBaseRepository {

    override suspend fun upsert(productItem: ProductItem): ProductItem = coroutineScopeWithObservation(UPDATE, or) { observation ->
        val query = Query.query(
            Criteria.where("id").`is`(UUID.fromString(productItem.id))
                .and("order_id").`is`(UUID.fromString(productItem.orderId))
        )

        val product = entityTemplate.selectOne(query, ProductItemEntity::class.java).awaitSingleOrNull()
        if (product != null) {
            val update = Update
                .update("quantity", (productItem.quantity + product.quantity))
                .set("version", product.version + 1)
                .set("updated_at", LocalDateTime.now())

            val updatedProduct = product.copy(quantity = (productItem.quantity + product.quantity), version = product.version + 1)
            val updateResult = entityTemplate.update(query, update, ProductItemEntity::class.java).awaitSingle()
            log.info("updateResult product: $updateResult")
            log.info("updateResult updatedProduct: $updatedProduct")
            return@coroutineScopeWithObservation updatedProduct.toProductItem()
        }

        entityTemplate.insert(ProductItemEntity.of(productItem)).awaitSingle().toProductItem()
            .also { productItem ->
                log.info("saved productItem: $productItem")
                observation.highCardinalityKeyValue("productItem", productItem.toString())
            }
    }

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
        private const val UPDATE = "ProductItemBaseRepository.update"
    }
}