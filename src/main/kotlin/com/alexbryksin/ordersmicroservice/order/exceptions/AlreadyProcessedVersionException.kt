package com.alexbryksin.ordersmicroservice.order.exceptions

data class AlreadyProcessedVersionException(val id: Any, val version: Long) : RuntimeException("event for id: $id and version: $version is already processed")
