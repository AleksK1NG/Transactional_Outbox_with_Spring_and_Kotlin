package com.alexbryksin.ordersmicroservice.order.exceptions

data class InvalidVersionException(val version: Long) : RuntimeException("invalid version: $version")
