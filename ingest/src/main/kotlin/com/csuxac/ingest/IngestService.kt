package com.csuxac.ingest

import com.csuxac.common.EventBus
import com.csuxac.common.events.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Properties

class IngestService(private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
    private val json = Json { ignoreUnknownKeys = true }

    private val producer by lazy { createProducer() }

    fun start() {
        scope.launch {
            EventBus.events.collect { event ->
                send(event)
            }
        }
    }

    private fun createProducer(): KafkaProducer<String, String> {
        val props = Properties().apply {
            put("bootstrap.servers", System.getenv()["KAFKA_BOOTSTRAP"] ?: "localhost:9092")
            put("key.serializer", StringSerializer::class.qualifiedName)
            put("value.serializer", StringSerializer::class.qualifiedName)
            put("enable.idempotence", true)
            put("acks", "all")
        }
        return KafkaProducer(props)
    }

    private fun send(event: Event) {
        val jsonStr = json.encodeToString(event)
        val record = ProducerRecord("player-events", event.playerId, jsonStr)
        producer.send(record)
    }
}