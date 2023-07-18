/*
 * Copyright 2023 Oliver Berg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE")

package net.ormr.tos

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.text.Charsets.UTF_8

@PublishedApi
internal const val EMPTY_STRING = ""

fun ByteBuffer.orderAwareSlice(): ByteBuffer = slice().order(order())

fun ByteBuffer.copyBuffer(): ByteBuffer {
    val buffer = DirectByteBuffer(capacity(), order())
    buffer.put(this)
    return buffer
}

fun ByteBuffer.skip(byteCount: Int): ByteBuffer = position(position() + byteCount)

@OptIn(ExperimentalStdlibApi::class)
fun ByteBuffer.skipPadding(byteCount: Int): ByteBuffer {
    repeat(byteCount) {
        val byte = get()
        check(byte == 0.toByte()) { "Supposed padding at ${(position() - 1).toHexString(HexFormat.UpperCase)} is not zero but $byte" }
    }
    return this
}

@Suppress("FunctionName")
inline fun DirectByteBuffer(capacity: Int, order: ByteOrder = ByteOrder.BIG_ENDIAN): ByteBuffer =
    ByteBuffer.allocateDirect(capacity).order(order)

fun ByteBuffer.getBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    get(bytes)
    return bytes
}

fun ByteBuffer.getAllBytes(): ByteArray = getBytes(remaining())

fun ByteBuffer.getString(length: Int): String = getString0(length)

inline fun ByteBuffer.getString0(
    length: Int,
    mutateArray: (ByteArray) -> Unit = {},
): String {
    if (length == 0) return EMPTY_STRING
    val bytes = getBytes(length)
    mutateArray(bytes)
    return UTF_8.decode(ByteBuffer.wrap(bytes)).toString()
}

fun ByteBuffer.putString(value: String): ByteBuffer = putString0(value)

inline fun ByteBuffer.putString0(
    value: String,
    mutateArray: (ByteArray) -> Unit = {},
): ByteBuffer {
    if (value.isEmpty()) return this
    val bytes = UTF_8.encode(value).getAllBytes()
    mutateArray(bytes)
    // TODO: do we need to change the order?
    put(bytes)
    return this
}

fun ByteBuffer.getNullTerminatedString(maxLength: Int): String = getNullTerminatedString0(maxLength)

inline fun ByteBuffer.getNullTerminatedString0(
    maxLength: Int,
    mutateArray: (ByteArray) -> Unit = {},
): String {
    val stringSlice = orderAwareSlice()
    var value = getUByte()
    // TODO: is having 'count' at 0 here correct?
    var count = 0
    while (value != 0.toUByte() && (count < maxLength)) {
        value = getUByte()
        count++
    }
    if (count == 0) {
        position((position() + maxLength) - 1)
        return EMPTY_STRING
    }
    val bytes = stringSlice.getBytes(count)
    mutateArray(bytes)
    position((position() + maxLength) - count - 1)
    return UTF_8.decode(ByteBuffer.wrap(bytes)).toString()
}

/**
 * Writes a string to the buffer, padding it with null characters to the specified [maxLength].
 */
fun ByteBuffer.putNullTerminatedString(value: String, maxLength: Int): ByteBuffer =
    putNullTerminatedString0(value, maxLength)

inline fun ByteBuffer.putNullTerminatedString0(
    value: String,
    maxLength: Int,
    mutateArray: (ByteArray) -> Unit = {},
): ByteBuffer {
    require(maxLength >= value.utf8Length) { "Value length must not be larger than length. (${value.length} > $maxLength)" }
    val bytes = UTF_8.encode(value).getAllBytes()
    mutateArray(bytes)
    // TODO: do we need to change the order?
    put(bytes)
    for (i in 1..(maxLength - value.utf8Length)) {
        put(0)
    }
    return this
}

fun ByteBuffer.getUByte(): UByte = get().toUByte()

fun ByteBuffer.putUByte(value: UByte): ByteBuffer = put(value.toByte())

fun ByteBuffer.getBoolean(): Boolean = when (val byte = get()) {
    0.toByte() -> false
    1.toByte() -> true
    else -> throw IllegalArgumentException("Expected 0 or 1, got $byte")
}

fun ByteBuffer.putBoolean(value: Boolean): ByteBuffer = put(if (value) 1 else 0)

fun ByteBuffer.get2ByteBoolean(): Boolean = when (val short = getShort()) {
    0.toShort() -> false
    1.toShort() -> true
    else -> throw IllegalArgumentException("Expected 0 or 1, got $short")
}

fun ByteBuffer.put2ByteBoolean(value: Boolean): ByteBuffer = putShort(if (value) 1 else 0)

fun ByteBuffer.getUShort(): UShort = getShort().toUShort()

fun ByteBuffer.putUShort(value: UShort): ByteBuffer = putShort(value.toShort())

fun ByteBuffer.getUInt(): UInt = getInt().toUInt()

fun ByteBuffer.putUInt(value: UInt): ByteBuffer = putInt(value.toInt())