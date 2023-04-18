package com.alexbryksin.ordersmicroservice.order.events

sealed class BaseEvent(val aggregateId: String, open val version: Long) {
}