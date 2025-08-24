package com.csuxac.core.packet

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import org.bukkit.plugin.Plugin

class PacketListener(
    plugin: Plugin,
    private val packetFlowAnalyzer: PacketFlowAnalyzer
) : PacketAdapter(
    plugin,
    PacketType.Play.Client.POSITION,
    PacketType.Play.Client.POSITION_LOOK,
    PacketType.Play.Client.LOOK,
    PacketType.Play.Client.FLYING
) {

    override fun onPacketReceiving(event: PacketEvent) {
        // Pass the packet event to the analyzer
        packetFlowAnalyzer.analyzePacket(event)
    }
}
