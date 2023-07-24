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

package net.ormr.tos.ipf

import net.ormr.tos.ipf.internal.ENCODED_START_VERSION
import net.ormr.tos.ipf.internal.Pkware
import net.ormr.tos.ipf.internal.fileWriteChannel
import net.ormr.tos.ipf.internal.inflate
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN
import java.nio.file.Path
import kotlin.io.path.createParentDirectories

class IpfExtractor(private val ipf: Ipf) {
    fun IpfElement.extractTo(rootDirectory: Path) {
        val extractedFile = rootDirectory.resolve(path)
        extractedFile.createParentDirectories()
        val buffer = getByteBufferFrom(this)
        extractedFile.fileWriteChannel().use { it.write(buffer) }
    }

    private fun getByteBufferFrom(element: IpfElement): ByteBuffer {
        val buffer = ipf.buffer.slice(element.fileOffset, element.compressedSize).order(LITTLE_ENDIAN)
        val version = ipf.version
        val decodedBuffer = when {
            version >= ENCODED_START_VERSION || version == 0u -> Pkware.decryptFrom(buffer)
            else -> buffer
        }
        return when {
            element.isCompressed() -> decodedBuffer.inflate(element.uncompressedSize)
            else -> decodedBuffer
        }.order(LITTLE_ENDIAN)
    }
}