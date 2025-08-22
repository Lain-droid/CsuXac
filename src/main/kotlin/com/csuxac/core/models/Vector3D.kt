package com.csuxac.core.models

import kotlin.math.sqrt
import kotlin.math.pow

/**
 * 3D Vector representation for Minecraft coordinates
 * Implements mathematical operations for physics calculations
 */
data class Vector3D(
    val x: Double,
    val y: Double,
    val z: Double
) {
    companion object {
        val ZERO = Vector3D(0.0, 0.0, 0.0)
        val UP = Vector3D(0.0, 1.0, 0.0)
        val DOWN = Vector3D(0.0, -1.0, 0.0)
    }
    
    operator fun plus(other: Vector3D): Vector3D {
        return Vector3D(x + other.x, y + other.y, z + other.z)
    }
    
    operator fun minus(other: Vector3D): Vector3D {
        return Vector3D(x - other.x, y - other.y, z - other.z)
    }
    
    operator fun times(scalar: Double): Vector3D {
        return Vector3D(x * scalar, y * scalar, z * scalar)
    }
    
    operator fun div(scalar: Double): Vector3D {
        return Vector3D(x / scalar, y / scalar, z / scalar)
    }
    
    fun magnitude(): Double {
        return sqrt(x * x + y * y + z * z)
    }
    
    fun normalize(): Vector3D {
        val mag = magnitude()
        return if (mag > 0.0) this / mag else ZERO
    }
    
    fun distanceTo(other: Vector3D): Double {
        return (this - other).magnitude()
    }
    
    fun dot(other: Vector3D): Double {
        return x * other.x + y * other.y + z * other.z
    }
    
    fun cross(other: Vector3D): Vector3D {
        return Vector3D(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
    }
    
    fun toBlockCoordinates(): BlockPosition {
        return BlockPosition(x.toInt(), y.toInt(), z.toInt())
    }
    
    fun toChunkCoordinates(): ChunkPosition {
        return ChunkPosition(x.toInt() shr 4, z.toInt() shr 4)
    }
}

data class BlockPosition(
    val x: Int,
    val y: Int,
    val z: Int
)

data class ChunkPosition(
    val x: Int,
    val z: Int
)