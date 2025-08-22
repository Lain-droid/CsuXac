package com.csuxac.detector

import com.csuxac.common.events.DetectionResultEvent
import com.csuxac.common.events.Event
import javax.script.ScriptEngineManager

class RuleEngine {
    private val engine = ScriptEngineManager().getEngineByExtension("kts")

    private val compiledScripts = mutableListOf<(Event) -> DetectionResultEvent?>()

    fun loadScript(source: String) {
        val invocable = engine.eval(source) as? (Event) -> DetectionResultEvent?
            ?: throw IllegalArgumentException("Script must return lambda")
        compiledScripts += invocable
    }

    fun evaluate(event: Event): List<DetectionResultEvent> =
        compiledScripts.mapNotNull { it(event) }
}