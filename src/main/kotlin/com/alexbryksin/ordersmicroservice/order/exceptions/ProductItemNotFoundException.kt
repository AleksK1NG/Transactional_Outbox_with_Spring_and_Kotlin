package com.alexbryksin.ordersmicroservice.order.exceptions

data class ProductItemNotFoundException(val id: Any) : RuntimeException("product item with id: $id not found") {
}