package com.alexbryksin.ordersmicroservice.utils.serializer

data class SerializationException(val ex: Throwable) : RuntimeException(ex)
