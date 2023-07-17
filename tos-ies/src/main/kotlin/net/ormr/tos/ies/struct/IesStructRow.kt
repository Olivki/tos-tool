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

package net.ormr.tos.ies.struct

import net.ormr.tos.getString
import net.ormr.tos.getUShort
import net.ormr.tos.ies.internal.utf8SizeOf
import net.ormr.tos.ies.internal.xor
import net.ormr.tos.putUShort
import java.nio.ByteBuffer
import net.ormr.tos.ies.struct.IesStructDataType.Float32 as IesFloat32
import net.ormr.tos.ies.struct.IesStructDataType.String1 as IesString1
import net.ormr.tos.ies.struct.IesStructDataType.String2 as IesString2

class IesStructRow(val table: IesStructTable) : IesStruct {
    var id: Int = 0 // classId
    lateinit var key: String // className
    lateinit var entries: Array<IesStructData>

    override fun readFrom(buffer: ByteBuffer) {
        id = buffer.getInt() // TODO: UInt
        key = buffer.getString(length = buffer.getUShort().toInt()).xor()
        val entries = mutableListOf<IesStructData>()
        val sortedColumns = table.sortedColumns
        // number columns -> buffer.position() + (Float.SIZE_BYTES * table.header.numColumnNumber)
        for (i in 0..<table.header.columnCount.toInt()) {
            val column = sortedColumns[i]
            entries += IesStructData(column) {
                data = when (column.type) {
                    IesFloat32 -> IesFloat32.decodeFrom(buffer)
                    IesString1 -> IesString1.decodeFrom(buffer)
                    IesString2 -> IesString2.decodeFrom(buffer)
                }
            }
        }
        for (entry in entries) {
            if (entry.isString) entry.flag = buffer.get()
        }
        this.entries = entries.toTypedArray()
    }

    override fun writeTo(buffer: ByteBuffer) {
        buffer.putInt(id)
        val keyBytes = key.xor().toByteArray()
        buffer.putUShort(keyBytes.size.toUShort())
        buffer.put(keyBytes)
        for (entry in entries) {
            entry.writeTo(buffer)
        }
        for (entry in entries) {
            if (entry.isString) buffer.put(entry.flag)
        }
    }

    override fun getSize(): Int {
        val dataSize = entries.sumOf { it.getSize() }
        return Int.SIZE_BYTES + Short.SIZE_BYTES + utf8SizeOf(key) + dataSize + table.getDataTypeCount<IesStructDataType.String>()
    }

    override fun toString(): String = "IesStructRow(id=$id, key='$key', entries=${entries.contentDeepToString()})"

    companion object {
        inline operator fun invoke(table: IesStructTable, body: IesStructRow.() -> Unit): IesStructRow =
            IesStructRow(table).apply(body)
    }
}