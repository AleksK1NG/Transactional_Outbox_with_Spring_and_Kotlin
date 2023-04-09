package com.alexbryksin.ordersmicroservice.utils.kafkaUtils

import org.apache.kafka.common.header.Headers

fun getHeader(name: String, headers: Headers): String {
    val headersMap = headers.associateBy({ it.key() }, { it.value() })
    return String(headersMap[name] ?: byteArrayOf())
}

fun headersToMap(headers: Headers) = headers.associateBy({ it.key() }, { it.value() })

fun addMapToHeaders(headersMap: Map<String, ByteArray>, headers: Headers) = headersMap.forEach { (key, value) -> headers.add(key, value) }

fun addHeadersToMap(headersMap: Map<String, ByteArray>, headers: Headers) = headers.forEach { headersMap[it.key()] to it.value() }