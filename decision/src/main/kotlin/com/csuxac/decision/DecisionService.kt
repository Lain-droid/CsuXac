package com.csuxac.decision

import com.csuxac.common.events.DetectionResultEvent
import com.csuxac.common.events.SanctionEvent
import com.csuxac.common.events.SanctionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import kotlinx.serialization.decodeFromString
import java.time.Duration
import java.util.Properties

class DecisionService(private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
    private val json = Json { ignoreUnknownKeys = true }

    private val consumer by lazy { createConsumer() }
    private val producer by lazy { createProducer() }

    fun start() {
        consumer.subscribe(listOf("detection-results"))
        scope.launch {
            while (true) {
                val records = consumer.poll(Duration.ofMillis(100))
                for (record in records) {
                    val result = parse(record.value())
                    val sanction = decide(result)
                    if (sanction != null) {
                        send(sanction)
                    }
                }
            }
        }
    }

    private fun decide(result: DetectionResultEvent): SanctionEvent? {
        val type = when {
            result.confidence > 0.9 -> SanctionType.BAN
            result.confidence > 0.75 -> SanctionType.KICK
            result.confidence > 0.6 -> SanctionType.WARN
            else -> null
        } ?: return null
        return SanctionEvent(
            playerId = result.playerId,
            timestamp = System.currentTimeMillis(),
            sanctionType = type,
            reason = "${result.cheatType} detected",
            confidence = result.confidence
        )
    }

    private fun createConsumer(): KafkaConsumer<String, String> {
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv()["KAFKA_BOOTSTRAP"] ?: "localhost:9092")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.qualifiedName)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.qualifiedName)
            put(ConsumerConfig.GROUP_ID_CONFIG, "decision")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
        }
        return KafkaConsumer(props)
    }

    private fun createProducer(): KafkaProducer<String, String> {
        val props = Properties().apply {
            put("bootstrap.servers", System.getenv()["KAFKA_BOOTSTRAP"] ?: "localhost:9092")
            put("key.serializer", StringSerializer::class.qualifiedName)
            put("value.serializer", StringSerializer::class.qualifiedName)
            put("enable.idempotence", true)
        }
        return KafkaProducer(props)
    }

    private fun parse(raw: String): DetectionResultEvent = json.decodeFromString(raw)

    private fun send(event: SanctionEvent) {
        val jsonStr = json.encodeToString(event)
        val record = ProducerRecord("sanctions", event.playerId, jsonStr)
        producer.send(record)
    }
}