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

import net.ormr.tos.getNullTerminatedString
import net.ormr.tos.putNullTerminatedString
import java.nio.ByteBuffer

class IesStructHeader(val table: IesStructTable) : IesStruct {
    lateinit var name: String // 128 byte length name
    var flag1: Int = 0
    var columnSize: Int = 0
    var rowSize: Int = 0
    var fileSize: Int = 0
    var flag2: Short = 0
    var rowCount: Short = 0
    var columnCount: Short = 0
    var intColumns: Short = 0
    var stringColumns: Short = 0
    var unkColumns: Short = 0

    override fun readFrom(buffer: ByteBuffer) {
        name = buffer.getNullTerminatedString(128)
        flag1 = buffer.getInt()  // should be 1
        columnSize = buffer.getInt()
        rowSize = buffer.getInt()
        fileSize = buffer.getInt()
        flag2 = buffer.getShort()
        rowCount = buffer.getShort()
        columnCount = buffer.getShort()
        intColumns = buffer.getShort()
        stringColumns = buffer.getShort()
        unkColumns = buffer.getShort()
    }

    override fun writeTo(buffer: ByteBuffer) {
        updateValues()

        buffer.putNullTerminatedString(name, 128)
        buffer.putInt(flag1)
        buffer.putInt(columnSize)
        buffer.putInt(rowSize)
        buffer.putInt(fileSize)
        buffer.putShort(flag2)
        buffer.putShort(rowCount)
        buffer.putShort(columnCount)
        buffer.putShort(intColumns)
        buffer.putShort(stringColumns)
        buffer.putShort(unkColumns)
    }

    fun updateValues() {
        fileSize = table.getSize()
        columnSize = table.getColumnsSize()
        rowSize = table.getRowsSize()
        columnCount = table.columns.size.toShort()
        rowCount = table.rows.size.toShort()
        intColumns = table.getDataTypeCount<IesStructDataType.Float32>().toShort()
        stringColumns = table.getDataTypeCount<IesStructDataType.String>().toShort()
    }

    fun getColumnsOffset(): Int = fileSize - (columnSize + rowSize)

    fun getRowsOffset(): Int = fileSize - rowSize

    override fun getSize(): Int = 128 + (Int.SIZE_BYTES * 4) + (Short.SIZE_BYTES * 6)
    override fun toString(): String =
        "IesStructHeader(name='$name', flag=$flag1, columnSize=$columnSize, rowSize=$rowSize, fileSize=$fileSize, flag2=$flag2, rowCount=$rowCount, columnCount=$columnCount, intColumns=$intColumns, stringColumns=$stringColumns, unkColumns=$unkColumns)"


    companion object {
        inline operator fun invoke(table: IesStructTable, body: IesStructHeader.() -> Unit): IesStructHeader =
            IesStructHeader(table).apply(body)
    }
}