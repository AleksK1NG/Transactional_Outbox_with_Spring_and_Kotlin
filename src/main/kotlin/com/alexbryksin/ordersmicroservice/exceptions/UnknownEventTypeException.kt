package com.alexbryksin.ordersmicroservice.exceptions

data class UnknownEventTypeException(val eventType: Any?) : RuntimeException("unknown event type: $eventType")
