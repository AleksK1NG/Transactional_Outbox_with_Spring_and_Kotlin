package com.alexbryksin.ordersmicroservice.configuration

data class TopicConfiguration(var name: String = "", var partitions: Int = 1, var replication: Int = 1)
