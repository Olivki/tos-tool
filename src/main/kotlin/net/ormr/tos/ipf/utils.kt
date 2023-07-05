package net.ormr.tos.ipf

import java.nio.ByteBuffer
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal fun ByteBuffer.getBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    get(bytes)
    return bytes
}

internal fun Duration.toPrettyString(): String = toString(DurationUnit.SECONDS, decimals = 2)