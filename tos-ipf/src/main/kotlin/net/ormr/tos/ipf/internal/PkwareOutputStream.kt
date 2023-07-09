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

import java.io.OutputStream

internal class PkwareOutputStream(password: ByteArray, private val delegate: OutputStream) : OutputStream() {
    private val key = intArrayOf(0x12345678, 0x23456789, 0x34567890)
    private var offset = 0

    init {
        for (byte in password) updateKey(byte)
    }

    override fun write(byte: Int) {
        try {
            if (offset % 2 != 0) {
                delegate.write(byte)
                return
            }
            val magicByte = key[2] and 0xFFFF or 0x02
            val encrypted = byte xor (magicByte * (magicByte xor 1) shr 8 and 0xFF)
            updateKey(byte.toByte())
            delegate.write(encrypted)
        } finally {
            offset++
        }
    }

    override fun flush() {
        delegate.flush()
    }

    override fun close() {
        delegate.close()
    }

    private fun updateKey(data: Byte) {
        key[0] = calculateCrc32(key[0], data)
        key[1] += key[0] and 0xFF
        key[1] = key[1] * 0x08088405 + 1
        key[2] = calculateCrc32(key[2], (key[1] shr 24).toByte())
    }
}