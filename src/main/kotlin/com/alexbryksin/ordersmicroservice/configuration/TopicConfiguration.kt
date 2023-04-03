package com.alexbryksin.ordersmicroservice.configuration

data class TopicConfiguration(val name: String = "", val partitions: Int = 0, val replication: Int = 0)
