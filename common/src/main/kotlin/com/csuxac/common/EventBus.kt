package com.csuxac.common

import com.csuxac.common.events.Event
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1024)
    val events = _events.asSharedFlow()

    suspend fun publish(event: Event) {
        _events.emit(event)
    }
}