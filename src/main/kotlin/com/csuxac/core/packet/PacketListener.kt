package com.csuxac.core.packet

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import org.bukkit.plugin.Plugin

class PacketListener(plugin: Plugin) : PacketAdapter(
    plugin,
    PacketType.Play.Client.POSITION,
    PacketType.Play.Client.POSITION_LOOK,
    PacketType.Play.Client.LOOK,
    PacketType.Play.Client.FLYING
) {

    override fun onPacketReceiving(event: PacketEvent) {
        // Pass the packet event to the analyzer
        PacketFlowAnalyzer.analyzePacket(event)
    }
}
