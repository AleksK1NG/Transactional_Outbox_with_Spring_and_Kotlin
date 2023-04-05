package com.alexbryksin.ordersmicroservice.utils.serializer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class Serializer(private val objectMapper: ObjectMapper) {

    fun <T> deserialize(data: ByteArray, clazz: Class<T>): T {
        return try {
            objectMapper.readValue(data, clazz)
        } catch (ex: Exception) {
            throw SerializationException(ex)
        }
    }

    fun serializeToBytes(data: Any): ByteArray {
        return try {
            objectMapper.writeValueAsBytes(data)
        } catch (ex: Exception) {
            throw SerializationException(ex)
        }
    }

    fun serializeToString(data: Any): String {
        return try {
            objectMapper.writeValueAsString(data)
        } catch (ex: Exception) {
            throw SerializationException(ex)
        }
    }
}