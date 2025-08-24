package com.csuxac.core.detection

import org.bukkit.entity.Player
import org.bukkit.Location

/**
 * Handles movement-related cheat detection, starting with a simple speed check.
 */
class MovementValidator {

    companion object {
        // These are approximations. Fine-tuning will be needed.
        // Speeds are in blocks per tick. 1 tick = 50ms.
        private const val MAX_WALK_SPEED = 0.22 // Blocks per tick
        private const val MAX_SPRINT_SPEED = 0.29
        private const val MAX_FLY_SPEED = 0.5
        private const val MAX_SWIM_SPEED = 0.15
        private const val JUMP_VERTICAL_SPEED = 0.42
        private const val TOLERANCE = 1.1 // 10% tolerance
    }

    /**
     * Checks a player's movement for potential speed hacks.
     *
     * @param player The player to check.
     * @param from The player's starting location.
     * @param to The player's ending location.
     * @return A ValidationResult indicating if the movement was valid.
     */
    fun checkSpeed(player: Player, from: Location, to: Location): ValidationResult {
        val deltaX = to.x - from.x
        val deltaZ = to.z - from.z
        val horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)

        val maxSpeed = when {
            player.isFlying -> MAX_FLY_SPEED
            player.isInWater -> MAX_SWIM_SPEED
            player.isSprinting -> MAX_SPRINT_SPEED
            else -> MAX_WALK_SPEED
        }

        if (horizontalSpeed > maxSpeed * TOLERANCE) {
            return ValidationResult(
                isValid = false,
                reason = "Exceeded max horizontal speed. Speed: %.2f, Max: %.2f".format(horizontalSpeed, maxSpeed)
            )
        }

        // We can add a vertical speed check later if needed.

        return ValidationResult(isValid = true)
    }
}

/**
 * A simple data class to hold the result of a validation check.
 *
 * @param isValid True if the check passed, false otherwise.
 * @param reason An optional reason for the failure.
 */
data class ValidationResult(val isValid: Boolean, val reason: String? = null)