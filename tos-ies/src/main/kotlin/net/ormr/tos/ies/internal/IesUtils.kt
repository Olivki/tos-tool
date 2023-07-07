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

internal fun String.shiftBits(): String {
    val bytes = toByteArray()
    for (i in bytes.indices) bytes[i] = (bytes[i].toInt() xor 0x1).toByte()
    return String(bytes)
}

internal fun utf8SizeOf(value: String): Int = value.toByteArray(Charsets.UTF_8).size