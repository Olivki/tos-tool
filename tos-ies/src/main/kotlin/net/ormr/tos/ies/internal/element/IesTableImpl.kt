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

package net.ormr.tos.ies.internal.element

import net.ormr.tos.DirectByteBuffer
import net.ormr.tos.ies.element.*
import net.ormr.tos.ies.internal.struct.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal data class IesTableImpl(
    override val header: IesHeader,
    override val columns: MutableList<IesColumn<*>>,
    override val rows: MutableList<IesRow>,
) : IesTable, InternalIesElementImpl {
    override fun toByteBuffer(): ByteBuffer {
        val self = this
        val structTable = IesStructTable {
            this.header = IesStructHeader(this) {
                this.name = self.header.name
                this.flag1 = self.header.flag1
                this.flag2 = self.header.flag2
                this.unkColumns = self.header.unknown
            }
            this.columns = Array(self.columns.size) { i ->
                val column = self.columns[i]
                IesStructColumn {
                    this.name = column.name
                    this.key = column.key
                    this.type = when (column.type) {
                        IesType.Float32 -> IesStructDataType.Float32
                        IesType.String1 -> IesStructDataType.String1
                        IesType.String2 -> IesStructDataType.String2
                    }
                    this.unk1 = column.unk1
                    this.unk2 = column.unk2
                    this.pos = column.position
                }
            }
            val structColumns = columns.associateBy(IesStructColumn::key)
            this.rows = Array(self.rows.size) { i ->
                val row = self.rows[i]
                IesStructRow(this) {
                    this.id = row.id
                    this.key = row.key
                    this.entries = Array(row.values.size) { j ->
                        val entry = row.values[j]
                        val structColumn = structColumns.getValue(entry.column.key)
                        IesStructData(structColumn) {
                            this.data = entry.data
                            if (entry is IesStringValue) {
                                this.flag = entry.flag.toByte()
                            }
                        }
                    }
                }
            }
        }
        val buffer = DirectByteBuffer(structTable.getSize() + 1, ByteOrder.LITTLE_ENDIAN)
        structTable.writeTo(buffer)
        return buffer.flip()
    }
}

private fun Boolean.toByte(): Byte = if (this) 1 else 0

private fun Byte.toBoolean(): Boolean = when (this) {
    0.toByte() -> false
    1.toByte() -> true
    else -> throw IllegalArgumentException("Byte must be either 0 or 1, but was $this")
}

@Suppress("UNCHECKED_CAST")
internal fun IesStructTable.toIesTable(): IesTable {
    val convertedColumns = columns.map { column ->
        IesColumn(
            name = column.name,
            key = column.key,
            type = when (column.type) {
                IesStructDataType.Float32 -> IesType.Float32
                IesStructDataType.String1 -> IesType.String1
                IesStructDataType.String2 -> IesType.String2
            },
            position = column.pos,
            unk1 = column.unk1,
            unk2 = column.unk2,
        )
    }.toMutableList<IesColumn<*>>()
    val mappedColumns = convertedColumns.associateBy(IesColumn<*>::key)
    return IesTable(
        header = IesHeader(
            name = header.name,
            flag = header.flag1,
            flag2 = header.flag2,
            unknown = header.unkColumns,
        ),
        columns = convertedColumns,
        rows = rows.map { row ->
            IesRow(
                id = row.id,
                key = row.key,
                values = row.entries.map { entry ->
                    val column = mappedColumns.getValue(entry.column.key)
                    when (column.type) {
                        IesType.Float32 -> IesFloat32Value(
                            value = entry.data as Float,
                            column = column as IesColumn<Float>,
                        )
                        IesType.String1 -> IesString1Value(
                            value = entry.data as String,
                            column = column as IesColumn<String>,
                            flag = entry.flag.toBoolean(),
                        )
                        IesType.String2 -> IesString2Value(
                            value = entry.data as String,
                            column = column as IesColumn<String>,
                            flag = entry.flag.toBoolean(),
                        )
                    }
                }.toMutableList(),
            )
        }.toMutableList(),
    )
}