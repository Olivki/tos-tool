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

package net.ormr.tos.ies.element

import net.ormr.tos.ies.internal.element.IesTableImpl
import net.ormr.tos.ies.internal.element.toIesTable
import net.ormr.tos.ies.internal.struct.IesStructTable
import java.nio.ByteBuffer
import java.nio.ByteOrder

interface IesTable : IesElement {
    val header: IesHeader
    val columns: MutableList<IesColumn<*>>
    val rows: MutableList<IesRow>

    val name: String
        get() = header.name

    fun toByteBuffer(): ByteBuffer

    companion object {
        fun readFromByteBuffer(buffer: ByteBuffer): IesTable = IesStructTable {
            readFrom(buffer.order(ByteOrder.LITTLE_ENDIAN))
        }.toIesTable()
    }
}

fun IesTable(
    header: IesHeader,
    columns: MutableList<IesColumn<*>> = mutableListOf(),
    rows: MutableList<IesRow> = mutableListOf(),
): IesTable = IesTableImpl(header, columns, rows)