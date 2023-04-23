package com.alexbryksin.ordersmicroservice.utils.serializer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Serializer(private val objectMapper: ObjectMapper) {

    fun <T> deserialize(data: ByteArray, clazz: Class<T>): T {
        return try {
            objectMapper.readValue(data, clazz)
        } catch (ex: Exception) {
            log.error("error while deserializing data: ${ex.localizedMessage}")
            throw SerializationException(ex)
        }
    }

    fun serializeToBytes(data: Any): ByteArray {
        return try {
            objectMapper.writeValueAsBytes(data)
        } catch (ex: Exception) {
            log.error("error while serializing data: ${ex.localizedMessage}")
            throw SerializationException(ex)
        }
    }

    fun serializeToString(data: Any): String {
        return try {
            objectMapper.writeValueAsString(data)
        } catch (ex: Exception) {
            log.error("error while serializing data: ${ex.localizedMessage}")
            throw SerializationException(ex)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(Serializer::class.java)
    }
}