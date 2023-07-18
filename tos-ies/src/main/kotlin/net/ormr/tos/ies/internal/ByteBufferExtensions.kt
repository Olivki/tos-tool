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

package net.ormr.tos.ies.internal

import net.ormr.tos.*
import java.nio.ByteBuffer

internal fun ByteBuffer.getIesString(): String {
    val length = getUShort().toInt()
    return getXorString(length)
}

internal fun ByteBuffer.putIesString(value: String): ByteBuffer {
    val length = value.utf8Length
    require(length <= UShort.MAX_VALUE.toInt()) { "String is too long to be encoded as an IES string ($value)" }
    putUShort(length.toUShort())
    return putXorString(value)
}

internal fun ByteBuffer.getXorString(length: Int): String = getString0(length, ::xorBytes)

internal fun ByteBuffer.putXorString(value: String): ByteBuffer = putString0(value, ::xorBytes)

internal fun ByteBuffer.getNullTerminatedXorString(
    maxLength: Int,
): String = getNullTerminatedString0(maxLength, ::xorBytes)

internal fun ByteBuffer.putNullTerminatedXorString(
    value: String,
    maxLength: Int,
): ByteBuffer = putNullTerminatedString0(value, maxLength, ::xorBytes)

private fun xorBytes(bytes: ByteArray) {
    for (i in bytes.indices) {
        bytes[i] = (bytes[i].toInt() xor 0x1).toByte()
    }
}