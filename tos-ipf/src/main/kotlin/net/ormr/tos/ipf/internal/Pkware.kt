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

import net.ormr.tos.DirectByteBuffer
import java.nio.ByteBuffer

internal object Pkware {
    private val PASSWORD = byteArrayOf(
        0x6F, 0x66, 0x4F, 0x31, 0x61, 0x30, 0x75, 0x65, 0x58, 0x41,
        0x3F, 0x20, 0x5B, 0xFF.toByte(), 0x73, 0x20, 0x68, 0x20, 0x25, 0x3F
    )

    fun decrypt(buffer: ByteBuffer, password: ByteArray = PASSWORD): ByteBuffer {
        val key = Key(password)
        val result = DirectByteBuffer(buffer.capacity(), order = buffer.order())
        var offset = 0
        while (buffer.hasRemaining()) {
            if (offset % 2 != 0) {
                result.put(buffer.get())
            } else {
                var byte = buffer.get().toInt() and 0xFF
                val magicByte = key[2] and 0xFFFF or 0x02
                byte = byte xor (magicByte * (magicByte xor 1) shr 8 and 0xFF)
                key.update(byte.toByte())
                result.put(byte.toByte())
            }
            offset++
        }
        return result.rewind()
    }

    private class Key(password: ByteArray) {
        private val data = intArrayOf(0x12345678, 0x23456789, 0x34567890)

        init {
            for (value in password) update(value)
        }

        fun update(value: Byte) {
            data[0] = calculateCrc32(data[0], value)
            data[1] += data[0] and 0xFF
            data[1] = data[1] * 0x08088405 + 1
            data[2] = calculateCrc32(data[2], (data[1] shr 24).toByte())
        }

        operator fun get(index: Int): Int = data[index]
    }
}