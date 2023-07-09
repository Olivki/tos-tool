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

@file:Suppress("UsePropertyAccessSyntax")

package net.ormr.tos.ipf.internal

import net.ormr.tos.getString
import net.ormr.tos.getUInt
import net.ormr.tos.getUShort
import net.ormr.tos.ipf.IpfElement
import net.ormr.tos.ipf.IpfFile
import net.ormr.tos.ipf.IpfFooter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_ONLY
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ

internal class IpfFileImpl(
    val buffer: ByteBuffer,
    override val footer: IpfFooter,
) : IpfFile {
    override val elements: MutableList<IpfElement> = mutableListOf()

    override fun addElement(element: IpfElement) {
        require(element is IpfElementImpl) { "Element must be of type 'IpfElementImpl'." }
        element.ipf = this
        elements += element
    }
}

internal fun readIpfFile(file: Path): IpfFile {
    val channel = FileChannel.open(file, READ)
    val buffer = channel.map(READ_ONLY, 0, channel.size()).order(ByteOrder.LITTLE_ENDIAN)
    val copiedBuffer = buffer.slice()
    buffer.position(buffer.limit() - 0x18)

    val fileCount = buffer.getUShort()

    // this is most likely a UInt, but because 'buffer.position' is an Int,
    // we can't do any meaningful conversions from UInt
    val fileTableOffset = buffer.getInt()
    // TODO: just do 'buffer.position' with the sizes instead?
    // buffer.position(buffer.position() + Short.SIZE_BYTES + (Int.SIZE_BYTES * 2))
    buffer.getShort()
    buffer.getInt()
    buffer.getInt() // compression
    val footer = readIpfFooter(buffer)
    val ipf = IpfFileImpl(copiedBuffer, footer)
    buffer.position(fileTableOffset)

    repeat(fileCount.toInt()) {
        val pathLength = buffer.getUShort().toInt()
        val crc = buffer.getInt()
        val compressedSize = buffer.getInt()
        val uncompressedSize = buffer.getInt()
        val fileOffset = buffer.getInt()
        val archiveNameLength = buffer.getUShort().toInt()
        val archiveName = buffer.getString(archiveNameLength)
        val path = buffer.getString(pathLength).replace('\\', '/')
        ipf.addElement(
            IpfElementImpl(
                crc = crc,
                compressedSize = compressedSize,
                uncompressedSize = uncompressedSize,
                fileOffset = fileOffset,
                archiveName = archiveName,
                path = path,
            )
        )
    }

    return ipf
}

private fun readIpfFooter(buffer: ByteBuffer): IpfFooter = IpfFooter(
    subversion = buffer.getUInt(),
    version = buffer.getUInt(),
)