### Transactional Outbox pattern step by step with Spring and Kotlin 💫

<img src="https://i.postimg.cc/V6sRgD4Z/outbox-logo-v1.jpg" alt="Transactional_Outbox_Logo"/>

The reason why we need **Transactional Outbox** is that a service often needs to publish messages as part of a transaction that updates the database.
Both the database update and the sending of the message must happen within a transaction.
Otherwise, if the service doesn’t perform these two operations atomically, a failure could leave the system in an inconsistent state.
The [GitHub repository with source code](https://github.com/AleksK1NG/Transactional_Outbox_with_Spring_and_Kotlin) for this article.

<img src="https://i.postimg.cc/ZKK0fPNp/Chris-Richardson-Microservices-Patterns-With-ex-1-pdf-2023-05-12-17-13-43.png" alt="Transactional Outbox"/>

In this article we will implement it using Reactive Spring and Kotlin with coroutines.
Full list of used dependencies: **Kotlin with coroutines**, **Spring Boot 3**, **WebFlux**, **R2DBC**, **Postgres**, **MongoDB**,
**Kafka**, **Grafana**, **Prometheus**, **Zipkin** and **Micrometer** for observability.


**Transactional Outbox** pattern is solving the problem of the implementation where usually the transaction tries to update the database table, then publish a message to the broker and commit the transaction.
But here is the problem, if at the last step commit of the transaction fails, the transaction will rollback database changes, but the event has been already published to the broker.
So we need to find a way how to guarantee both, database writing and publishing to the broker.
The idea of how we can solve it is next: in the one transaction, save to the orders table, and in the same transaction, save to the outbox table, and commit the transaction.
then we have to publish saved events from the outbox table to the broker,
we have two ways to do that, **CDC(Change data capture)** tool like [**Debezium**](https://debezium.io/), which continuously monitors your databases
and lets any of your applications stream every row-level change in the same order they were committed to the database,
and [**Polling publisher**](https://microservices.io/patterns/data/polling-publisher.html), for this project used polling publisher.
Highly recommend **Chris Richardson** Book: **Microservices patterns**, where Transactional Outbox pattern is very well explained.

And one more important thing is we have to be ready for cases when the same event can be published more than one time, so the consumer must be **idempotent**.
**Idempotence** describes the reliability of messages in a distributed system, specifically about the reception of duplicated messages. Because of retries or message broker features, a message sent once can be received multiple times by consumers.
A service is idempotent if processing the same event multiple times results in the same state and output as processing that event just a single time. The reception of a duplicated event does not change the application state or behavior.
Most of the time, an idempotent service detects these events and ignores them. Idempotence can be implemented using unique identifiers.


So let's implement it, business logic of our example microservice is simple, orders with product items, it's two tables for simplicity and outbox table of course.
Usually, an outbox table looks like, when at data field we store serialized event, most common is JSON format, but it's up to you and concrete microservice,
we can put as data field state changes or can simply put every time the last updated full order domain entity, of course, state changes take much less size, but again it's up to you,
other fields in the outbox table usually event type, timestamp, version, and other metadata,
it again depends on each concrete implementation, but often it's required minimum, the version field is for concurrency control.

All UI interfaces will be available on ports:

#### Swagger UI: http://localhost:8000/webjars/swagger-ui/index.html
<img src="https://i.postimg.cc/d1367JLP/Swagger-UI-2023-05-05-13-04-59.png" alt="Transactional Outbox"/>

#### Grafana UI: http://localhost:3000
<img src="https://i.postimg.cc/DZBtYjSv/JVM-Actuator-Dashboards-Grafana-2023-05-10-12-58-14.png" alt="Grafana"/>

#### Zipkin UI: http://localhost:9411
<img src="https://i.postimg.cc/hGjtsK7n/Zipkin-2023-05-12-17-25-11.png" alt="Zipkin"/>

#### Kafka UI: http://localhost:8086/
<img src="https://i.postimg.cc/W4trBZwQ/UI-for-Apache-Kafka-2023-05-09-14-07-56.png" alt="Topics"/>

#### Prometheus UI: http://localhost:9090
<img src="https://i.postimg.cc/0N7rbPF0/Prometheus-Time-Series-Collection-and-Processing-Server-2023-05-12-17-41-54.png" alt="Prometheus"/>


The docker-compose file for this article has postgres, mongodb, zookeeper, kafka, kafka-ui, zipkin, prometheus and grafana,
for local development run: **make local** or **make develop**, first run only docker-compose, second same include the microservice image.

```yaml
version: "3.9"

services:
  microservices_postgresql:
    image: postgres:latest
    container_name: microservices_postgresql
    expose:
      - "5432"
    ports:
      - "5432:5432"
    restart: always
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
      - POSTGRES_DB=microservices
      - POSTGRES_HOST=5432
    command: -p 5432
    volumes:
      - ./docker_data/microservices_pgdata:/var/lib/postgresql/data
    networks: [ "microservices" ]

  zoo1:
    image: confluentinc/cp-zookeeper:7.3.0
    hostname: zoo1
    container_name: zoo1
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_SERVER_ID: 1
      ZOOKEEPER_SERVERS: zoo1:2888:3888
    volumes:
      - "./zookeeper:/zookeeper"
    networks: [ "microservices" ]

  kafka1:
    image: confluentinc/cp-kafka:7.3.0
    hostname: kafka1
    container_name: kafka1
    ports:
      - "9092:9092"
      - "29092:29092"
      - "9999:9999"
    environment:
      KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka1:19092,EXTERNAL://${DOCKER_HOST_IP:-127.0.0.1}:9092,DOCKER://host.docker.internal:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT,DOCKER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_ZOOKEEPER_CONNECT: "zoo1:2181"
      KAFKA_BROKER_ID: 1
      KAFKA_LOG4J_LOGGERS: "kafka.controller=INFO,kafka.producer.async.DefaultEventHandler=INFO,state.change.logger=INFO"
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_JMX_PORT: 9999
      KAFKA_JMX_HOSTNAME: ${DOCKER_HOST_IP:-127.0.0.1}
      KAFKA_AUTHORIZER_CLASS_NAME: kafka.security.authorizer.AclAuthorizer
      KAFKA_ALLOW_EVERYONE_IF_NO_ACL_FOUND: "true"
    depends_on:
      - zoo1
    volumes:
      - "./kafka_data:/kafka"
    networks: [ "microservices" ]

  kafka-ui:
    image: provectuslabs/kafka-ui
    container_name: kafka-ui
    ports:
      - "8086:8080"
    restart: always
    environment:
      - KAFKA_CLUSTERS_0_NAME=local
      - KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=kafka1:19092
    networks: [ "microservices" ]

  zipkin-all-in-one:
    image: openzipkin/zipkin:latest
    restart: always
    ports:
      - "9411:9411"
    networks: [ "microservices" ]

  mongo:
    image: mongo
    restart: always
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: admin
      MONGODB_DATABASE: bank_accounts
    networks: [ "microservices" ]

  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    command:
      - --config.file=/etc/prometheus/prometheus.yml
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    networks: [ "microservices" ]

  node_exporter:
    container_name: microservices_node_exporter
    restart: always
    image: prom/node-exporter
    ports:
      - '9101:9100'
    networks: [ "microservices" ]

  grafana:
    container_name: microservices_grafana
    restart: always
    image: grafana/grafana
    ports:
      - '3000:3000'
    networks: [ "microservices" ]


networks:
  microservices:
    name: microservices
```

The Postgres database schema for this project is:

<img src="https://i.postimg.cc/rwtGr8Bp/orders-microservice-microservices-2023-05-09-14-11-11.png" alt="schema"/>


Orders domain REST Controller has the following methods:

```kotlin
@RestController
@RequestMapping(path = ["/api/v1/orders"])
class OrderController(private val orderService: OrderService, private val or: ObservationRegistry) {

    @GetMapping
    @Operation(method = "getOrders", summary = "get order with pagination", operationId = "getOrders")
    suspend fun getOrders(
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
    ) = coroutineScopeWithObservation(GET_ORDERS, or) { observation ->
        ResponseEntity.ok()
            .body(orderService.getAllOrders(PageRequest.of(page, size))
                .map { it.toSuccessResponse() }
                .also { response -> observation.highCardinalityKeyValue("response", response.toString()) }
            )
    }

    @GetMapping(path = ["{id}"])
    @Operation(method = "getOrderByID", summary = "get order by id", operationId = "getOrderByID")
    suspend fun getOrderByID(@PathVariable id: String) = coroutineScopeWithObservation(GET_ORDER_BY_ID, or) { observation ->
        ResponseEntity.ok().body(orderService.getOrderWithProductsByID(UUID.fromString(id)).toSuccessResponse())
            .also { response ->
                observation.highCardinalityKeyValue("response", response.toString())
                log.info("getOrderByID response: $response")
            }
    }

    @PostMapping
    @Operation(method = "createOrder", summary = "create new order", operationId = "createOrder")
    suspend fun createOrder(@Valid @RequestBody createOrderDTO: CreateOrderDTO) = coroutineScopeWithObservation(CREATE_ORDER, or) { observation ->
        ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(createOrderDTO.toOrder()).toSuccessResponse())
            .also {
                log.info("created order: $it")
                observation.highCardinalityKeyValue("response", it.toString())
            }
    }

    @PutMapping(path = ["/add/{id}"])
    @Operation(method = "addProductItem", summary = "add to the order product item", operationId = "addProductItem")
    suspend fun addProductItem(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: CreateProductItemDTO
    ) = coroutineScopeWithObservation(ADD_PRODUCT, or) { observation ->
        ResponseEntity.ok().body(orderService.addProductItem(dto.toProductItem(id)))
            .also {
                observation.highCardinalityKeyValue("CreateProductItemDTO", dto.toString())
                observation.highCardinalityKeyValue("id", id.toString())
                log.info("addProductItem id: $id, dto: $dto")
            }
    }

    @PutMapping(path = ["/remove/{orderId}/{productItemId}"])
    @Operation(method = "removeProductItem", summary = "remove product from the order", operationId = "removeProductItem")
    suspend fun removeProductItem(
        @PathVariable orderId: UUID,
        @PathVariable productItemId: UUID
    ) = coroutineScopeWithObservation(REMOVE_PRODUCT, or) { observation ->
        ResponseEntity.ok().body(orderService.removeProductItem(orderId, productItemId))
            .also {
                observation.highCardinalityKeyValue("productItemId", productItemId.toString())
                observation.highCardinalityKeyValue("orderId", orderId.toString())
                log.info("removeProductItem orderId: $orderId, productItemId: $productItemId")
            }
    }

    @PutMapping(path = ["/pay/{id}"])
    @Operation(method = "payOrder", summary = "pay order", operationId = "payOrder")
    suspend fun payOrder(@PathVariable id: UUID, @Valid @RequestBody dto: PayOrderDTO) = coroutineScopeWithObservation(PAY_ORDER, or) { observation ->
        ResponseEntity.ok().body(orderService.pay(id, dto.paymentId).toSuccessResponse())
            .also {
                observation.highCardinalityKeyValue("response", it.toString())
                log.info("payOrder result: $it")
            }
    }

    @PutMapping(path = ["/cancel/{id}"])
    @Operation(method = "cancelOrder", summary = "cancel order", operationId = "cancelOrder")
    suspend fun cancelOrder(@PathVariable id: UUID, @Valid @RequestBody dto: CancelOrderDTO) = coroutineScopeWithObservation(CANCEL_ORDER, or) { observation ->
        ResponseEntity.ok().body(orderService.cancel(id, dto.reason).toSuccessResponse())
            .also {
                observation.highCardinalityKeyValue("response", it.toString())
                log.info("cancelOrder result: $it")
            }
    }

    @PutMapping(path = ["/submit/{id}"])
    @Operation(method = "submitOrder", summary = "submit order", operationId = "submitOrder")
    suspend fun submitOrder(@PathVariable id: UUID) = coroutineScopeWithObservation(SUBMIT_ORDER, or) { observation ->
        ResponseEntity.ok().body(orderService.submit(id).toSuccessResponse())
            .also {
                observation.highCardinalityKeyValue("response", it.toString())
                log.info("submitOrder result: $it")
            }
    }

    @PutMapping(path = ["/complete/{id}"])
    @Operation(method = "completeOrder", summary = "complete order", operationId = "completeOrder")
    suspend fun completeOrder(@PathVariable id: UUID) = coroutineScopeWithObservation(COMPLETE_ORDER, or) { observation ->
        ResponseEntity.ok().body(orderService.complete(id).toSuccessResponse())
            .also {
                observation.highCardinalityKeyValue("response", it.toString())
                log.info("completeOrder result: $it")
            }
    }
}
```

As typed earlier the main idea of implementation for the transactional outbox is at the first step in the one transaction write to orders and outbox tables and commit the transaction, additional, but not required optimization, we can in the same methods after successfully committed a transaction, then publish the event and delete it from the outbox table,
but here if any one step of publishing to the broker or deleting from the outbox table fails, it's ok, because we have **polling producer** as scheduled process, which anyway will do that,
again it's small optimization and improvement, and it's not mandatory to implement an outbox pattern, so do it or not, it's up to you, try both variants and chose the best for your case.
In our case we use Kafka, so we have to remember that producers has **acks setting**,

when **acks=0** producers consider messages as "written successfully" the moment the message was sent without waiting for the broker to accept it at all.
If the broker goes offline or an exception happens, we won’t know and will lose data, so be careful with this setting and don't use **acks=0**,
When **acks=1** , producers consider messages as "written successfully" when the message was acknowledged by only the leader.
When **acks=all**, producers consider messages as "written successfully" when the message is accepted by all in-sync replicas (ISR).

<img src="https://i.postimg.cc/5yhzFdPx/Swagger-UI-2023-05-09-13-57-44.png" alt="Swagger"/>

In the Simplified sequence diagram for service layer business logic, steps 5 and 6 are optional and not required optimization, because we have polling publisher anyway: <br/>

<img src="https://i.postimg.cc/MHBx2D93/outbox.png" alt="Sequence"/>

The order service implementation:

```kotlin
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
```

```kotlin
@Service
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val productItemRepository: ProductItemRepository,
    private val outboxRepository: OrderOutboxRepository,
    private val orderMongoRepository: OrderMongoRepository,
    private val txOp: TransactionalOperator,
    private val eventsPublisher: EventsPublisher,
    private val kafkaTopicsConfiguration: KafkaTopicsConfiguration,
    private val or: ObservationRegistry,
    private val outboxEventSerializer: OutboxEventSerializer
) : OrderService {

    override suspend fun createOrder(order: Order): Order = coroutineScopeWithObservation(CREATE, or) { observation ->
        txOp.executeAndAwait {
            orderRepository.insert(order).let {
                val productItemsEntityList = ProductItemEntity.listOf(order.productsList(), UUID.fromString(it.id))
                val insertedItems = productItemRepository.insertAll(productItemsEntityList).toList()

                it.addProductItems(insertedItems.map { item -> item.toProductItem() })

                Pair(it, outboxRepository.save(outboxEventSerializer.orderCreatedEventOf(it)))
            }
        }.run {
            observation.highCardinalityKeyValue("order", first.toString())
            observation.highCardinalityKeyValue("outboxEvent", second.toString())

            publishOutboxEvent(second)
            first
        }
    }

    override suspend fun addProductItem(productItem: ProductItem): Unit = coroutineScopeWithObservation(ADD_PRODUCT, or) { observation ->
        txOp.executeAndAwait {
            val order = orderRepository.findOrderByID(UUID.fromString(productItem.orderId))
            order.incVersion()

            val updatedProductItem = productItemRepository.upsert(productItem)

            val savedRecord = outboxRepository.save(
                outboxEventSerializer.productItemAddedEventOf(
                    order,
                    productItem.copy(version = updatedProductItem.version).toEntity()
                )
            )

            orderRepository.updateVersion(UUID.fromString(order.id), order.version)
                .also { result -> log.info("addOrderItem result: $result, version: ${order.version}") }

            savedRecord
        }.run {
            observation.highCardinalityKeyValue("outboxEvent", this.toString())
            publishOutboxEvent(this)
        }
    }

    override suspend fun removeProductItem(orderID: UUID, productItemId: UUID): Unit = coroutineScopeWithObservation(REMOVE_PRODUCT, or) { observation ->
        txOp.executeAndAwait {
            if (!productItemRepository.existsById(productItemId)) throw ProductItemNotFoundException(productItemId)

            val order = orderRepository.findOrderByID(orderID)
            productItemRepository.deleteById(productItemId)

            order.incVersion()

            val savedRecord = outboxRepository.save(outboxEventSerializer.productItemRemovedEventOf(order, productItemId))

            orderRepository.updateVersion(UUID.fromString(order.id), order.version)
                .also { log.info("removeProductItem update order result: $it, version: ${order.version}") }

            savedRecord
        }.run {
            observation.highCardinalityKeyValue("outboxEvent", this.toString())
            publishOutboxEvent(this)
        }
    }

    override suspend fun pay(id: UUID, paymentId: String): Order = coroutineScopeWithObservation(PAY, or) { observation ->
        txOp.executeAndAwait {
            val order = orderRepository.getOrderWithProductItemsByID(id)
            order.pay(paymentId)

            val updatedOrder = orderRepository.update(order)
            Pair(updatedOrder, outboxRepository.save(outboxEventSerializer.orderPaidEventOf(updatedOrder, paymentId)))
        }.run {
            observation.highCardinalityKeyValue("order", first.toString())
            observation.highCardinalityKeyValue("outboxEvent", second.toString())

            publishOutboxEvent(second)
            first
        }
    }

    override suspend fun cancel(id: UUID, reason: String?): Order = coroutineScopeWithObservation(CANCEL, or) { observation ->
        txOp.executeAndAwait {
            val order = orderRepository.findOrderByID(id)
            order.cancel()

            val updatedOrder = orderRepository.update(order)
            Pair(updatedOrder, outboxRepository.save(outboxEventSerializer.orderCancelledEventOf(updatedOrder, reason)))
        }.run {
            observation.highCardinalityKeyValue("order", first.toString())
            observation.highCardinalityKeyValue("outboxEvent", second.toString())

            publishOutboxEvent(second)
            first
        }
    }

    override suspend fun submit(id: UUID): Order = coroutineScopeWithObservation(SUBMIT, or) { observation ->
        txOp.executeAndAwait {
            val order = orderRepository.getOrderWithProductItemsByID(id)
            order.submit()

            val updatedOrder = orderRepository.update(order)
            updatedOrder.addProductItems(order.productsList())

            Pair(updatedOrder, outboxRepository.save(outboxEventSerializer.orderSubmittedEventOf(updatedOrder)))
        }.run {
            observation.highCardinalityKeyValue("order", first.toString())
            observation.highCardinalityKeyValue("outboxEvent", second.toString())

            publishOutboxEvent(second)
            first
        }
    }

    override suspend fun complete(id: UUID): Order = coroutineScopeWithObservation(COMPLETE, or) { observation ->
        txOp.executeAndAwait {
            val order = orderRepository.findOrderByID(id)
            order.complete()

            val updatedOrder = orderRepository.update(order)
            log.info("order submitted: ${updatedOrder.status} for id: $id")

            Pair(updatedOrder, outboxRepository.save(outboxEventSerializer.orderCompletedEventOf(updatedOrder)))
        }.run {
            observation.highCardinalityKeyValue("order", first.toString())
            observation.highCardinalityKeyValue("outboxEvent", second.toString())

            publishOutboxEvent(second)
            first
        }
    }

    @Transactional(readOnly = true)
    override suspend fun getOrderWithProductsByID(id: UUID): Order = coroutineScopeWithObservation(GET_ORDER_WITH_PRODUCTS_BY_ID, or) { observation ->
        orderRepository.getOrderWithProductItemsByID(id).also { observation.highCardinalityKeyValue("order", it.toString()) }
    }

    override suspend fun getAllOrders(pageable: Pageable): Page<Order> = coroutineScopeWithObservation(GET_ALL_ORDERS, or) { observation ->
        orderMongoRepository.getAllOrders(pageable).also { observation.highCardinalityKeyValue("pageResult", it.toString()) }
    }

    override suspend fun deleteOutboxRecordsWithLock() = coroutineScopeWithObservation(DELETE_OUTBOX_RECORD_WITH_LOCK, or) { observation ->
        outboxRepository.deleteOutboxRecordsWithLock {
            observation.highCardinalityKeyValue("outboxEvent", it.toString())
            eventsPublisher.publish(getTopicName(it.eventType), it)
        }
    }

    override suspend fun getOrderByID(id: UUID): Order = coroutineScopeWithObservation(GET_ORDER_BY_ID, or) { observation ->
        orderMongoRepository.getByID(id.toString())
            .also { log.info("getOrderByID: $it") }
            .also { observation.highCardinalityKeyValue("order", it.toString()) }
    }

    private suspend fun publishOutboxEvent(event: OutboxRecord) = coroutineScopeWithObservation(PUBLISH_OUTBOX_EVENT, or) { observation ->
        try {
            log.info("publishing outbox event: $event")

            outboxRepository.deleteOutboxRecordByID(event.eventId!!) {
                eventsPublisher.publish(getTopicName(event.eventType), event.aggregateId.toString(), event)
            }

            log.info("outbox event published and deleted: $event")
            observation.highCardinalityKeyValue("event", event.toString())
        } catch (ex: Exception) {
            log.error("exception while publishing outbox event: ${ex.localizedMessage}")
            observation.error(ex)
        }
    }
}
```

<img src="https://i.postimg.cc/P5hK59PW/Zipkin-2023-05-09-13-59-29.png" alt="Zipkin"/>

<img src="https://i.postimg.cc/3RXg1TBR/UI-for-Apache-Kafka-2023-05-09-14-08-33.png" alt="Kafka"/>

Order and product items postgres repositories is combination of **CoroutineCrudRepository** and custom implementation using **DatabaseClient** and **R2dbcEntityTemplate**,
supports optimistic and pessimistic locking, depending on method requirements.

```kotlin
@Repository
interface OrderRepository : CoroutineCrudRepository<OrderEntity, UUID>, OrderBaseRepository

@Repository
interface OrderBaseRepository {
    suspend fun getOrderWithProductItemsByID(id: UUID): Order
    suspend fun updateVersion(id: UUID, newVersion: Long): Long
    suspend fun findOrderByID(id: UUID): Order
    suspend fun insert(order: Order): Order
    suspend fun update(order: Order): Order
}

@Repository
class OrderBaseRepositoryImpl(
    private val dbClient: DatabaseClient,
    private val entityTemplate: R2dbcEntityTemplate,
    private val or: ObservationRegistry
) : OrderBaseRepository {

    override suspend fun updateVersion(id: UUID, newVersion: Long): Long = coroutineScopeWithObservation(UPDATE_VERSION, or) { observation ->
        dbClient.sql("UPDATE microservices.orders SET version = (version + 1) WHERE id = :id AND version = :version")
            .bind(ID, id)
            .bind(VERSION, newVersion - 1)
            .fetch()
            .rowsUpdated()
            .awaitSingle()
            .also { log.info("for order with id: $id version updated to $newVersion") }
            .also {
                observation.highCardinalityKeyValue("id", id.toString())
                observation.highCardinalityKeyValue("newVersion", newVersion.toString())
            }
    }

    override suspend fun getOrderWithProductItemsByID(id: UUID): Order = coroutineScopeWithObservation(GET_ORDER_WITH_PRODUCTS_BY_ID, or) { observation ->
        dbClient.sql(
            """SELECT o.id, o.email, o.status, o.address, o.version, o.payment_id, o.created_at, o.updated_at, 
            |pi.id as productId, pi.price, pi.title, pi.quantity, pi.order_id, pi.version as itemVersion, pi.created_at as itemCreatedAt, pi.updated_at as itemUpdatedAt
            |FROM microservices.orders o 
            |LEFT JOIN microservices.product_items pi on o.id = pi.order_id 
            |WHERE o.id = :id""".trimMargin()
        )
            .bind(ID, id)
            .map { row, _ -> Pair(OrderEntity.of(row), ProductItemEntity.of(row)) }
            .flow()
            .toList()
            .let { orderFromList(it) }
            .also {
                log.info("getOrderWithProductItemsByID order: $it")
                observation.highCardinalityKeyValue("order", it.toString())
            }
    }

    override suspend fun findOrderByID(id: UUID): Order = coroutineScopeWithObservation(FIND_ORDER_BY_ID, or) { observation ->
        val query = Query.query(Criteria.where(ID).`is`(id))
        entityTemplate.selectOne(query, OrderEntity::class.java).awaitSingleOrNull()?.toOrder()
            .also { observation.highCardinalityKeyValue("order", it.toString()) }
            ?: throw OrderNotFoundException(id)
    }

    override suspend fun insert(order: Order): Order = coroutineScopeWithObservation(INSERT, or) { observation ->
        entityTemplate.insert(order.toEntity()).awaitSingle().toOrder()
            .also {
                log.info("inserted order: $it")
                observation.highCardinalityKeyValue("order", it.toString())
            }
    }

    override suspend fun update(order: Order): Order = coroutineScopeWithObservation(UPDATE, or) { observation ->
        entityTemplate.update(order.toEntity()).awaitSingle().toOrder()
            .also {
                log.info("updated order: $it")
                observation.highCardinalityKeyValue("order", it.toString())
            }
    }
}
```

```kotlin
interface ProductItemBaseRepository {
    suspend fun insert(productItemEntity: ProductItemEntity): ProductItemEntity
    suspend fun insertAll(productItemEntities: List<ProductItemEntity>): List<ProductItemEntity>
    suspend fun upsert(productItem: ProductItem): ProductItem
}

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
}
```

The **outbox repository**, important detail here is to be able to handle the case of multiple pod instances processing in parallel outbox table,
of course, we have idempotent consumers, but as we can, we have to avoid processing the same table events more than one time, to prevent multiple instances select and publish the same events,
we use here **FOR UPDATE SKIP LOCKED** - this combination does the next thing, when one instance tries to select a batch of outbox events if some other instance already selected these records,
first, one will skip locked records and select the next available and not locked, and so on.

<img src="https://i.postimg.cc/pVh8Hw9Y/select-for-update.png" alt="Select_for_update"/>


```kotlin
@Repository
interface OutboxBaseRepository {
    suspend fun deleteOutboxRecordByID(id: UUID, callback: suspend () -> Unit): Long
    suspend fun deleteOutboxRecordsWithLock(callback: suspend (outboxRecord: OutboxRecord) -> Unit)
}

class OutboxBaseRepositoryImpl(
    private val dbClient: DatabaseClient,
    private val txOp: TransactionalOperator,
    private val or: ObservationRegistry,
    private val transactionalOperator: TransactionalOperator
) : OutboxBaseRepository {

    override suspend fun deleteOutboxRecordByID(id: UUID, callback: suspend () -> Unit): Long =
        coroutineScopeWithObservation(DELETE_OUTBOX_RECORD_BY_ID, or) { observation ->
            withTimeout(DELETE_OUTBOX_RECORD_TIMEOUT_MILLIS) {
                txOp.executeAndAwait {

                    callback()

                    dbClient.sql("DELETE FROM microservices.outbox_table WHERE event_id = :eventId")
                        .bind("eventId", id)
                        .fetch()
                        .rowsUpdated()
                        .awaitSingle()
                        .also {
                            log.info("outbox event with id: $it deleted")
                            observation.highCardinalityKeyValue("id", it.toString())
                        }
                }
            }
        }

    override suspend fun deleteOutboxRecordsWithLock(callback: suspend (outboxRecord: OutboxRecord) -> Unit) =
        coroutineScopeWithObservation(DELETE_OUTBOX_RECORD_WITH_LOCK, or) { observation ->
            withTimeout(DELETE_OUTBOX_RECORD_TIMEOUT_MILLIS) {
                txOp.executeAndAwait {

                    dbClient.sql("SELECT * FROM microservices.outbox_table ORDER BY timestamp ASC LIMIT 10 FOR UPDATE SKIP LOCKED")
                        .map { row, _ -> OutboxRecord.of(row) }
                        .flow()
                        .onEach {
                            log.info("deleting outboxEvent with id: ${it.eventId}")

                            callback(it)

                            dbClient.sql("DELETE FROM microservices.outbox_table WHERE event_id = :eventId")
                                .bind("eventId", it.eventId!!)
                                .fetch()
                                .rowsUpdated()
                                .awaitSingle()

                            log.info("outboxEvent with id: ${it.eventId} published and deleted")
                            observation.highCardinalityKeyValue("eventId", it.eventId.toString())
                        }
                        .collect()
                }
            }
        }
}
```

The polling producer implementation is a scheduled process which doing the same job for publishing and delete events at the given interval as typed earlier and uses the same service method:

```kotlin
@Component
@ConditionalOnProperty(prefix = "schedulers", value = ["outbox.enable"], havingValue = "true")
class OutboxScheduler(private val orderService: OrderService, private val or: ObservationRegistry) {

    @Scheduled(initialDelayString = "\${schedulers.outbox.initialDelayMillis}", fixedRateString = "\${schedulers.outbox.fixedRate}")
    fun publishAndDeleteOutboxRecords() = runBlocking {
        coroutineScopeWithObservation(PUBLISH_AND_DELETE_OUTBOX_RECORDS, or) {
            log.debug("starting scheduled outbox table publishing")
            orderService.deleteOutboxRecordsWithLock()
            log.debug("completed scheduled outbox table publishing")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OutboxScheduler::class.java)
        private const val PUBLISH_AND_DELETE_OUTBOX_RECORDS = "OutboxScheduler.publishAndDeleteOutboxRecords"
    }
}
```

Usually, transactional outbox is more often required to guarantee data consistency between microservices, here, for example, consumers in the same microservice process it and save to mongodb,
the one more important detail here, as we're processing kafka events in multiple consumer processes, it can randomize the order,
in Kafka we have a keys feature, and it helps us because it sends messages with the same key to one partition.
But if the broker has not had this feature, we have to handle it manually, cases when for example
fist some of the consumers trying to process event #6 before events #4,#5 were processed, so, for this reason, have a domain entity version field in outbox events,
so we can simply look at the version and validate, if in our database we have order version #3, but now processing event with version #6, we need first wait for #4,#5 and process them first,
but of course, these details depend on each concrete business logic of the application, here shows only the idea that it's a possible case.
And one more important detail - is retry topics, if we need to retry process of the messages, better to create a retry topic and process retry here,
how much time to retry and other advanced logic detail depends on your concrete case.
In the example, we have two listeners, where one of them is for retry topic message processing:

```kotlin
@Component
class OrderConsumer(
    private val kafkaTopicsConfiguration: KafkaTopicsConfiguration,
    private val serializer: Serializer,
    private val eventsPublisher: EventsPublisher,
    private val orderEventProcessor: OrderEventProcessor,
    private val or: ObservationRegistry,
) {

    @KafkaListener(
        groupId = "\${kafka.consumer-group-id:order-service-group-id}",
        topics = [
            "\${topics.orderCreated.name}",
            "\${topics.productAdded.name}",
            "\${topics.productRemoved.name}",
            "\${topics.orderPaid.name}",
            "\${topics.orderCancelled.name}",
            "\${topics.orderSubmitted.name}",
            "\${topics.orderCompleted.name}",
        ],
        id = "orders-consumer"
    )
    fun process(ack: Acknowledgment, consumerRecord: ConsumerRecord<String, ByteArray>) = runBlocking {
        coroutineScopeWithObservation(PROCESS, or) { observation ->
            try {
                observation.highCardinalityKeyValue("consumerRecord", getConsumerRecordInfoWithHeaders(consumerRecord))

                processOutboxRecord(serializer.deserialize(consumerRecord.value(), OutboxRecord::class.java))
                ack.acknowledge()

                log.info("committed record: ${getConsumerRecordInfo(consumerRecord)}")
            } catch (ex: Exception) {
                observation.highCardinalityKeyValue("consumerRecord", getConsumerRecordInfoWithHeaders(consumerRecord))
                observation.error(ex)

                if (ex is SerializationException || ex is UnknownEventTypeException || ex is AlreadyProcessedVersionException) {
                    log.error("ack not serializable, unknown or already processed record: ${getConsumerRecordInfoWithHeaders(consumerRecord)}")
                    ack.acknowledge()
                    return@coroutineScopeWithObservation
                }

                if (ex is InvalidVersionException || ex is NoSuchElementException || ex is OrderNotFoundException) {
                    publishRetryTopic(kafkaTopicsConfiguration.retryTopic.name, consumerRecord, 1)
                    ack.acknowledge()
                    log.warn("ack concurrency write or version exception ${ex.localizedMessage}")
                    return@coroutineScopeWithObservation
                }

                publishRetryTopic(kafkaTopicsConfiguration.retryTopic.name, consumerRecord, 1)
                ack.acknowledge()
                log.error("ack exception while processing record: ${getConsumerRecordInfoWithHeaders(consumerRecord)}", ex)
            }
        }
    }


    @KafkaListener(groupId = "\${kafka.consumer-group-id:order-service-group-id}", topics = ["\${topics.retryTopic.name}"], id = "orders-retry-consumer")
    fun processRetry(ack: Acknowledgment, consumerRecord: ConsumerRecord<String, ByteArray>): Unit = runBlocking {
        coroutineScopeWithObservation(PROCESS_RETRY, or) { observation ->
            try {
                log.warn("processing retry topic record >>>>>>>>>>>>> : ${getConsumerRecordInfoWithHeaders(consumerRecord)}")
                observation.highCardinalityKeyValue("consumerRecord", getConsumerRecordInfoWithHeaders(consumerRecord))

                processOutboxRecord(serializer.deserialize(consumerRecord.value(), OutboxRecord::class.java))
                ack.acknowledge()

                log.info("committed retry record: ${getConsumerRecordInfo(consumerRecord)}")
            } catch (ex: Exception) {
                observation.highCardinalityKeyValue("consumerRecord", getConsumerRecordInfoWithHeaders(consumerRecord))
                observation.error(ex)

                val currentRetry = String(consumerRecord.headers().lastHeader(RETRY_COUNT_HEADER).value()).toInt()
                observation.highCardinalityKeyValue("currentRetry", currentRetry.toString())

                if (ex is InvalidVersionException || ex is NoSuchElementException || ex is OrderNotFoundException) {
                    publishRetryTopic(kafkaTopicsConfiguration.retryTopic.name, consumerRecord, currentRetry)
                    log.warn("ack concurrency write or version exception ${ex.localizedMessage},record: ${getConsumerRecordInfoWithHeaders(consumerRecord)}")
                    ack.acknowledge()
                    return@coroutineScopeWithObservation
                }

                if (currentRetry > MAX_RETRY_COUNT) {
                    publishRetryTopic(kafkaTopicsConfiguration.deadLetterQueue.name, consumerRecord, currentRetry + 1)
                    ack.acknowledge()
                    log.error("MAX_RETRY_COUNT exceed, send record to DLQ: ${getConsumerRecordInfoWithHeaders(consumerRecord)}")
                    return@coroutineScopeWithObservation
                }

                if (ex is SerializationException || ex is UnknownEventTypeException || ex is AlreadyProcessedVersionException) {
                    ack.acknowledge()
                    log.error("commit not serializable, unknown or already processed record: ${getConsumerRecordInfoWithHeaders(consumerRecord)}")
                    return@coroutineScopeWithObservation
                }

                log.error("exception while processing: ${ex.localizedMessage}, record: ${getConsumerRecordInfoWithHeaders(consumerRecord)}")
                publishRetryTopic(kafkaTopicsConfiguration.retryTopic.name, consumerRecord, currentRetry + 1)
                ack.acknowledge()
            }
        }
    }


    private suspend fun publishRetryTopic(topic: String, record: ConsumerRecord<String, ByteArray>, retryCount: Int) =
        coroutineScopeWithObservation(PUBLISH_RETRY_TOPIC, or) { observation ->
            observation.highCardinalityKeyValue("topic", record.topic())
                .highCardinalityKeyValue("key", record.key())
                .highCardinalityKeyValue("offset", record.offset().toString())
                .highCardinalityKeyValue("value", String(record.value()))
                .highCardinalityKeyValue("retryCount", retryCount.toString())

            record.headers().remove(RETRY_COUNT_HEADER)
            record.headers().add(RETRY_COUNT_HEADER, retryCount.toString().toByteArray())

            mono { publishRetryRecord(topic, record, retryCount) }
                .retryWhen(Retry.backoff(PUBLISH_RETRY_COUNT, Duration.ofMillis(PUBLISH_RETRY_BACKOFF_DURATION_MILLIS))
                    .filter { it is SerializationException })
                .awaitSingle()
        }
}
```

The role of the orders events processor at this microservice is validating of the events version and update mongodb:
<img src="https://i.postimg.cc/vHHjRXDP/orders-microservice-console-4-localhost-2023-05-10-12-24-26.png" alt="MongoDB"/>

```kotlin
interface OrderEventProcessor {
    suspend fun on(orderCreatedEvent: OrderCreatedEvent)
    suspend fun on(productItemAddedEvent: ProductItemAddedEvent)
    suspend fun on(productItemRemovedEvent: ProductItemRemovedEvent)
    suspend fun on(orderPaidEvent: OrderPaidEvent)
    suspend fun on(orderCancelledEvent: OrderCancelledEvent)
    suspend fun on(orderSubmittedEvent: OrderSubmittedEvent)
    suspend fun on(orderCompletedEvent: OrderCompletedEvent)
}

@Service
class OrderEventProcessorImpl(
    private val orderMongoRepository: OrderMongoRepository,
    private val or: ObservationRegistry,
) : OrderEventProcessor {

    override suspend fun on(orderCreatedEvent: OrderCreatedEvent): Unit = coroutineScopeWithObservation(ON_ORDER_CREATED_EVENT, or) { observation ->
        orderMongoRepository.insert(orderCreatedEvent.order).also {
            log.info("created order: $it")
            observation.highCardinalityKeyValue("order", it.toString())
        }
    }

    override suspend fun on(productItemAddedEvent: ProductItemAddedEvent): Unit =
        coroutineScopeWithObservation(ON_ORDER_PRODUCT_ADDED_EVENT, or) { observation ->
            val order = orderMongoRepository.getByID(productItemAddedEvent.orderId)
            validateVersion(order.id, order.version, productItemAddedEvent.version)

            order.addProductItem(productItemAddedEvent.productItem)
            order.version = productItemAddedEvent.version

            orderMongoRepository.update(order).also {
                log.info("productItemAddedEvent updatedOrder: $it")
                observation.highCardinalityKeyValue("order", it.toString())
            }
        }

    override suspend fun on(productItemRemovedEvent: ProductItemRemovedEvent): Unit =
        coroutineScopeWithObservation(ON_ORDER_PRODUCT_REMOVED_EVENT, or) { observation ->
            val order = orderMongoRepository.getByID(productItemRemovedEvent.orderId)
            validateVersion(order.id, order.version, productItemRemovedEvent.version)

            order.removeProductItem(productItemRemovedEvent.productItemId)
            order.version = productItemRemovedEvent.version

            orderMongoRepository.update(order).also {
                log.info("productItemRemovedEvent updatedOrder: $it")
                observation.highCardinalityKeyValue("order", it.toString())
            }
        }

    override suspend fun on(orderPaidEvent: OrderPaidEvent): Unit = coroutineScopeWithObservation(ON_ORDER_PAID_EVENT, or) { observation ->
        val order = orderMongoRepository.getByID(orderPaidEvent.orderId)
        validateVersion(order.id, order.version, orderPaidEvent.version)

        order.pay(orderPaidEvent.paymentId)
        order.version = orderPaidEvent.version

        orderMongoRepository.update(order).also {
            log.info("orderPaidEvent updatedOrder: $it")
            observation.highCardinalityKeyValue("order", it.toString())
        }
    }

    override suspend fun on(orderCancelledEvent: OrderCancelledEvent): Unit = coroutineScopeWithObservation(ON_ORDER_CANCELLED_EVENT, or) { observation ->
        val order = orderMongoRepository.getByID(orderCancelledEvent.orderId)
        validateVersion(order.id, order.version, orderCancelledEvent.version)

        order.cancel()
        order.version = orderCancelledEvent.version

        orderMongoRepository.update(order).also {
            log.info("orderCancelledEvent updatedOrder: $it")
            observation.highCardinalityKeyValue("order", it.toString())
        }
    }

    override suspend fun on(orderSubmittedEvent: OrderSubmittedEvent): Unit = coroutineScopeWithObservation(ON_ORDER_SUBMITTED_EVENT, or) { observation ->
        val order = orderMongoRepository.getByID(orderSubmittedEvent.orderId)
        validateVersion(order.id, order.version, orderSubmittedEvent.version)

        order.submit()
        order.version = orderSubmittedEvent.version

        orderMongoRepository.update(order).also {
            log.info("orderSubmittedEvent updatedOrder: $it")
            observation.highCardinalityKeyValue("order", it.toString())
        }
    }

    override suspend fun on(orderCompletedEvent: OrderCompletedEvent): Unit = coroutineScopeWithObservation(ON_ORDER_COMPLETED_EVENT, or) { observation ->
        val order = orderMongoRepository.getByID(orderCompletedEvent.orderId)
        validateVersion(order.id, order.version, orderCompletedEvent.version)

        order.complete()
        order.version = orderCompletedEvent.version

        orderMongoRepository.update(order).also {
            log.info("orderCompletedEvent updatedOrder: $it")
            observation.highCardinalityKeyValue("order", it.toString())
        }
    }

    private fun validateVersion(id: Any, currentDomainVersion: Long, eventVersion: Long) {
        log.info("validating version for id: $id, currentDomainVersion: $currentDomainVersion, eventVersion: $eventVersion")
        if (currentDomainVersion >= eventVersion) {
            log.warn("currentDomainVersion >= eventVersion validating version for id: $id, currentDomainVersion: $currentDomainVersion, eventVersion: $eventVersion")
            throw AlreadyProcessedVersionException(id, eventVersion)
        }
        if ((currentDomainVersion + 1) < eventVersion) {
            log.warn("currentDomainVersion + 1) < eventVersion validating version for id: $id, currentDomainVersion: $currentDomainVersion, eventVersion: $eventVersion")
            throw InvalidVersionException(eventVersion)
        }
    }
}
```

The mongodb repository code is quite simple:

```kotlin
interface OrderMongoRepository {
    suspend fun insert(order: Order): Order
    suspend fun update(order: Order): Order
    suspend fun getByID(id: String): Order
    suspend fun getAllOrders(pageable: Pageable): Page<Order>
}

@Repository
class OrderMongoRepositoryImpl(
    private val mongoTemplate: ReactiveMongoTemplate,
    private val or: ObservationRegistry,
) : OrderMongoRepository {

    override suspend fun insert(order: Order): Order = coroutineScopeWithObservation(INSERT, or) { observation ->
        withContext(Dispatchers.IO) {
            mongoTemplate.insert(OrderDocument.of(order)).awaitSingle().toOrder()
                .also { log.info("inserted order: $it") }
                .also { observation.highCardinalityKeyValue("order", it.toString()) }
        }
    }

    override suspend fun update(order: Order): Order = coroutineScopeWithObservation(UPDATE, or) { observation ->
        withContext(Dispatchers.IO) {
            val query = Query.query(Criteria.where(ID).`is`(order.id).and(VERSION).`is`(order.version - 1))

            val update = Update()
                .set(EMAIL, order.email)
                .set(ADDRESS, order.address)
                .set(STATUS, order.status)
                .set(VERSION, order.version)
                .set(PAYMENT_ID, order.paymentId)
                .set(PRODUCT_ITEMS, order.productsList())

            val options = FindAndModifyOptions.options().returnNew(true).upsert(false)
            val updatedOrderDocument = mongoTemplate.findAndModify(query, update, options, OrderDocument::class.java)
                .awaitSingleOrNull() ?: throw OrderNotFoundException(order.id.toUUID())

            observation.highCardinalityKeyValue("order", updatedOrderDocument.toString())
            updatedOrderDocument.toOrder().also { orderDocument -> log.info("updated order: $orderDocument") }
        }
    }

    override suspend fun getByID(id: String): Order = coroutineScopeWithObservation(GET_BY_ID, or) { observation ->
        withContext(Dispatchers.IO) {
            mongoTemplate.findById(id, OrderDocument::class.java).awaitSingle().toOrder()
                .also { log.info("found order: $it") }
                .also { observation.highCardinalityKeyValue("order", it.toString()) }
        }
    }

    override suspend fun getAllOrders(pageable: Pageable): Page<Order> = coroutineScopeWithObservation(GET_ALL_ORDERS, or) { observation ->
        withContext(Dispatchers.IO) {
            val query = Query().with(pageable)
            val data = async { mongoTemplate.find(query, OrderDocument::class.java).collectList().awaitSingle() }.await()
            val count = async { mongoTemplate.count(Query(), OrderDocument::class.java).awaitSingle() }.await()
            PageableExecutionUtils.getPage(data.map { it.toOrder() }, pageable) { count }
                .also { observation.highCardinalityKeyValue("pageResult", it.pageable.toString()) }
        }
    }
}
```

More details and source code of the full project you can find [here](https://github.com/AleksK1NG/Transactional_Outbox_with_Spring_and_Kotlin),
of course in real-world applications, we have to implement many more necessary features, like k8s health checks, rate limiters, etc.,
depending on the project it can be implemented in different ways, for example, you can use Kubernetes and Istio for some of them.
I hope this article is usefully and helpfully, and be happy to receive any feedback or questions, feel free to contact [me](https://www.linkedin.com/in/alexander-bryksin/) by [email](alexander.bryksin@yandex.ru) or any [messengers](https://t.me/AlexanderBryksin) :)
