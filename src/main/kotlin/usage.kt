package com.example

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.time.Duration.Companion.hours
import kotlin.time.TimeSource

class ApiKeyState(
    val timeMark: TimeSource.Monotonic.ValueTimeMark,
    val availableUnits: Double
)

const val unitsPerHour = 100.0
val unitsPerNanoSecond = unitsPerHour / 1.hours.inWholeNanoseconds
val apiKeyStates = ConcurrentHashMap<String, ApiKeyState>()

@Serializable
data class ChargeResult(
    val allowed: Boolean,
    val availableUnits: Int
)

fun chargeUnits(apiKey: String, requestedUnits: Int): ChargeResult {
    require(requestedUnits >= 0) { "requestedUnits must be >= 0" }
    var result: ChargeResult? = null

    apiKeyStates.compute(apiKey) { _, existing ->
        val now = TimeSource.Monotonic.markNow()
        val state = existing ?: ApiKeyState(
            timeMark = now,
            availableUnits = unitsPerHour
        )
        val elapsedNanoSeconds = (now - state.timeMark).inWholeNanoseconds
        val replenishedUnits =
            min(unitsPerHour, state.availableUnits + elapsedNanoSeconds * unitsPerNanoSecond)

        val allowed = replenishedUnits >= requestedUnits
        val newAvailableUnits =
            if (allowed) replenishedUnits - requestedUnits
            else replenishedUnits

        result = ChargeResult(
            allowed = allowed,
            availableUnits = newAvailableUnits.toInt()
        )

        ApiKeyState(
            timeMark = now,
            availableUnits = newAvailableUnits
        )
    }

    return result!!
}
