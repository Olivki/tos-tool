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

import net.ormr.tos.ies.internal.struct.COLUMN_SIZE
import net.ormr.tos.ies.internal.struct.HEADER_SIZE
import net.ormr.tos.utf8Length

internal fun Ies.calculateSize(): Int {
    val headerSize = HEADER_SIZE
    val columnSize = calculateColumnSize()
    val fieldSize = calculateFieldSize()
    return headerSize + columnSize + fieldSize
}

internal fun Ies.calculateColumnSize(): Int = columns.size * COLUMN_SIZE

internal fun Ies.calculateFieldSize(): Int = classes.sumOf { it.calculateSize() }

internal fun IesClass.calculateSize(): Int {
    val numbersSize = fields.count { it is IesNumber } * Float.SIZE_BYTES
    val strings = fields.filterIsInstance<IesStringField<*>>()
    val stringsSize = strings.sumOf { UShort.SIZE_BYTES + (it.value?.utf8Length ?: 0) }
    val usesScriptFunctionsSize = strings.size * Byte.SIZE_BYTES
    return UInt.SIZE_BYTES + (UShort.SIZE_BYTES + className.utf8Length) + numbersSize + stringsSize + usesScriptFunctionsSize
}