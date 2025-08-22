package com.csuxac.detector

import com.csuxac.common.events.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.time.Duration
import java.util.Properties

class DetectorService(private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
    private val json = Json { ignoreUnknownKeys = true }

    private val consumer by lazy { createConsumer() }

    fun start() {
        consumer.subscribe(listOf("player-events"))
        scope.launch {
            while (true) {
                val records = consumer.poll(Duration.ofMillis(100))
                for (record in records) {
                    val event = parse(record.value())
                    // TODO apply detection rules and publish results
                }
            }
        }
    }

    private fun createConsumer(): KafkaConsumer<String, String> {
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv()["KAFKA_BOOTSTRAP"] ?: "localhost:9092")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.qualifiedName)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.qualifiedName)
            put(ConsumerConfig.GROUP_ID_CONFIG, "detector")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
        }
        return KafkaConsumer(props)
    }

    private fun parse(jsonStr: String): Event = json.decodeFromString(jsonStr)
}