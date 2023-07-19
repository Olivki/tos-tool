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

import net.ormr.tos.getString
import net.ormr.tos.getUInt
import net.ormr.tos.getUShort
import net.ormr.tos.ipf.internal.TAIL_SIZE
import net.ormr.tos.skip
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_ONLY
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ

object IpfReader {
    fun readFrom(file: Path): Ipf = FileChannel.open(file, READ).use { channel ->
        val buffer = channel.map(READ_ONLY, 0, channel.size()).order(LITTLE_ENDIAN)
        readFrom(buffer)
    }

    fun readFrom(bytes: ByteArray): Ipf = readFrom(ByteBuffer.wrap(bytes))

    fun readFrom(buffer: ByteBuffer): Ipf {
        buffer.order(LITTLE_ENDIAN)
        val copiedBuffer = buffer.duplicate().order(LITTLE_ENDIAN)
        buffer.position(buffer.limit() - TAIL_SIZE)
        val fileCount = buffer.getUShort()
        // probably UInt, but we can't really do anything with that
        val fileTableOffset = buffer.getInt()
        buffer.skip(Short.SIZE_BYTES + (Int.SIZE_BYTES * 2))
        val subversion = buffer.getUInt()
        val version = buffer.getUInt()
        buffer.position(fileTableOffset)
        val elements = List(fileCount.toInt()) { readElement(buffer) }
        return Ipf(buffer = copiedBuffer, subversion = subversion, version = version, elements = elements)
    }

    private fun readElement(buffer: ByteBuffer): IpfElement {
        val pathLength = buffer.getUShort().toInt()
        val crc = buffer.getInt()
        val compressedSize = buffer.getInt()
        val uncompressedSize = buffer.getInt()
        val fileOffset = buffer.getInt()
        val archiveNameLength = buffer.getUShort().toInt()
        val archiveName = buffer.getString(archiveNameLength)
        val path = buffer.getString(pathLength).replace('\\', '/')
        return IpfElement(
            crc = crc,
            compressedSize = compressedSize,
            uncompressedSize = uncompressedSize,
            fileOffset = fileOffset,
            archiveName = archiveName,
            path = path,
        )
    }
}