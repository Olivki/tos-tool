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

package net.ormr.tos.ies.internal.struct

import java.nio.ByteBuffer

internal class IesStructTable : IesStruct {
    lateinit var header: IesStructHeader
    lateinit var columns: Array<IesStructColumn>
    lateinit var rows: Array<IesStructRow>
    
    val sortedColumns: List<IesStructColumn> by lazy { columns.sorted() }

    override fun readFrom(buffer: ByteBuffer) {
        header = IesStructHeader(this) { readFrom(buffer) }

        buffer.position(header.getColumnsOffset())
        columns = Array(header.columnCount.toInt()) { IesStructColumn { readFrom(buffer) } }

        buffer.position(header.getRowsOffset())
        rows = Array(header.rowCount.toInt()) { IesStructRow(this) { readFrom(buffer) } }
    }

    override fun writeTo(buffer: ByteBuffer) {
        header.writeTo(buffer)
        columns.forEach { it.writeTo(buffer) }
        rows.forEach { it.writeTo(buffer) }
    }

    fun getColumnsSize(): Int = columns.sumOf { it.getSize() }

    fun getRowsSize(): Int = columns.sumOf { it.getSize() }

    override fun getSize(): Int = header.getSize() + getColumnsSize() + getRowsSize()

    inline fun <reified T : IesStructDataType> getDataTypeCount(): Int = columns.count { it.type is T }
    override fun toString(): String =
        "IesStructTable(header=$header, columns=${columns.contentDeepToString()}, rows=${rows.contentDeepToString()})"


    companion object {
        inline operator fun invoke(body: IesStructTable.() -> Unit): IesStructTable = IesStructTable().apply(body)
    }
}