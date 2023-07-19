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

package net.ormr.tos.ipf.internal

private const val POLYNOMIAL = -0x12477CE0
private val LOOKUP_TABLE = IntArray(256) { i ->
    var value = i
    repeat(8) {
        value = (if (value and 1 == 1) value ushr 1 xor POLYNOMIAL else value ushr 1)
    }
    value
}

internal fun calculateCrc32(crc: Int, byte: Byte): Int = LOOKUP_TABLE[crc xor byte.toInt() and 0xFF] xor (crc ushr 8)