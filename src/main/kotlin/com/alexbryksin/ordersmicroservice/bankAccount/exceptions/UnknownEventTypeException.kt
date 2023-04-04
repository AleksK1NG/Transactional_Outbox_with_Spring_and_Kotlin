package com.alexbryksin.ordersmicroservice.bankAccount.exceptions

data class UnknownEventTypeException(val eventType: String?) : RuntimeException("unknown event type: $eventType")
