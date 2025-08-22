package com.csuxac.detector.api

import com.csuxac.common.events.Event
import com.csuxac.common.events.DetectionResultEvent

interface Rule {
    val name: String
    fun evaluate(event: Event): DetectionResultEvent?
}