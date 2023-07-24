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

import net.ormr.tos.*
import net.ormr.tos.ipf.internal.*
import java.nio.ByteOrder.LITTLE_ENDIAN
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_ONLY
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.CRC32
import kotlin.io.path.name
import kotlin.io.path.relativeTo

class IpfFileBuilder(
    target: Path,
    private val root: Path,
    private val compressionLevel: Int,
    private val archiveName: String,
    private val subversion: UInt,
    private val version: UInt,
) {
    private val crc = CRC32()
    private val offset = AtomicInteger(0)
    private val elements = mutableListOf<IpfElement>()
    private val channel = target.fileWriteChannel()
    private val lock = LOCK()

    fun importFile(file: Path) {
        FileChannel.open(file, READ).use { channel ->
            val buffer = channel.map(READ_ONLY, 0, channel.size()).order(LITTLE_ENDIAN)
            val compressedBuffer = when {
                NO_COMPRESSION.any(file.name::endsWith) -> buffer
                else -> buffer.deflate(compressionLevel)
            }
            val encodedBuffer = when {
                version >= ENCODED_START_VERSION || version == 0u -> Pkware.encrypt(compressedBuffer)
                else -> compressedBuffer
            }
            synchronized(lock) {
                crc.reset()
                crc.update(encodedBuffer.orderAwareDuplicate())
                val element = IpfElement(
                    crc = crc.value.toInt(),
                    compressedSize = encodedBuffer.limit(),
                    uncompressedSize = buffer.limit(),
                    fileOffset = offset.get(),
                    archiveName = archiveName,
                    path = file.relativeTo(root).joinToString(separator = "\\") { it.name }, // TODO: correct?
                )
                offset.addAndGet(encodedBuffer.limit())
                elements.add(element)
                this.channel.write(encodedBuffer)
            }
        }
    }

    fun writeEnd() {
        // file table
        var fileTableOffset = offset.get()
        val buffer = DirectByteBuffer(1044, order = LITTLE_ENDIAN)
        elements.forEach { element ->
            buffer.putUShort(element.path.utf8Length.toUShort()) // TODO: check length?
            buffer.putInt(element.crc)
            buffer.putInt(element.compressedSize)
            buffer.putInt(element.uncompressedSize)
            buffer.putInt(element.fileOffset)
            buffer.putUShort(archiveName.utf8Length.toUShort()) // TODO: check length?
            buffer.putString(archiveName)
            buffer.putString(element.path)
            buffer.flip()
            fileTableOffset += buffer.limit()
            channel.write(buffer)
            buffer.clear()
        }

        // data
        buffer.putUShort(elements.size.toUShort()) // TODO: check size?
        buffer.putInt(offset.get())
        buffer.putShort(0)
        buffer.putInt(fileTableOffset)

        // tail
        buffer.put(ZIP_MAGIC)
        buffer.putUInt(subversion)
        buffer.putUInt(version)
        buffer.flip()

        channel.write(buffer)
        channel.close()
    }

    private class LOCK
}