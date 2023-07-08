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
import java.nio.charset.Charset

private const val NULL: Char = Char.MIN_VALUE

@Suppress("FunctionName")
inline fun DirectByteBuffer(capacity: Int, order: ByteOrder = ByteOrder.BIG_ENDIAN): ByteBuffer =
    ByteBuffer.allocateDirect(capacity).order(order)

fun ByteBuffer.getBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    get(bytes)
    return bytes
}

fun ByteBuffer.getString(length: Int, charset: Charset = Charsets.UTF_8): String = String(getBytes(length), charset)

fun ByteBuffer.putString(value: String, charset: Charset = Charsets.UTF_8): ByteBuffer = put(value.toByteArray(charset))


/*fun ByteBuffer.getNullTerminatedString(length: Int, charset: Charset = Charsets.UTF_8): String =
    String(getBytes(length), charset).trimEnd { it == NULL }*/

/**
 * Reads a null-terminated string from the buffer.
 */
fun ByteBuffer.getNullTerminatedString(length: Int): String {
    var value = getUByte()
    var count = 0
    val string = buildString(length) {
        while (value != 0.toUByte()) {
            append(value.toInt().toChar())
            value = getUByte()
            count++
        }
    }
    position(position() + length - count - 1)
    return string
}

/**
 * Writes a string to the buffer, padding it with null characters to the specified [length].
 */
fun ByteBuffer.putNullTerminatedString(value: String, length: Int, charset: Charset = Charsets.UTF_8): ByteBuffer =
    putString(value.padEnd(length, NULL), charset)

fun ByteBuffer.getUByte(): UByte = get().toUByte()

fun ByteBuffer.putUByte(value: UByte): ByteBuffer = put(value.toByte())

fun ByteBuffer.getUShort(): UShort = getShort().toUShort()

fun ByteBuffer.putUShort(value: UShort): ByteBuffer = putShort(value.toShort())

fun ByteBuffer.getUInt(): UInt = getInt().toUInt()

fun ByteBuffer.putUInt(value: UInt): ByteBuffer = putInt(value.toInt())