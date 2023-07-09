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

import net.ormr.tos.ipf.IpfElement
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal data class IpfElementImpl(
    override val crc: Int,
    override val compressedSize: Int,
    override val uncompressedSize: Int,
    override val fileOffset: Int,
    override val archiveName: String,
    override val path: String,
) : IpfElement {
    lateinit var ipf: IpfFileImpl

    override fun readAsByteBuffer(): ByteBuffer {
        // TODO: is 'fileOffset' correct here? or should we do 'fileOffset - 1'
        val buffer = ipf.buffer.slice(fileOffset, compressedSize).order(ByteOrder.LITTLE_ENDIAN)
        val version = ipf.footer.version
        val result = when {
            version >= 11_035u || version == 0u -> Pkware.decrypt(buffer)
            else -> buffer
        }
        return (if (isCompressed()) result.inflateTo(uncompressedSize) else result).order(ByteOrder.LITTLE_ENDIAN)
    }
}