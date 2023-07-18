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

import net.ormr.tos.utf8Length

internal const val HEADER_SIZE = (64 * 2) + (UInt.SIZE_BYTES * 4) + (UShort.SIZE_BYTES * 6)

internal const val COLUMN_SIZE = (64 * 2) + (UShort.SIZE_BYTES * 4)

internal fun IesStruct.calculateSize(): Int {
    val headerSize = HEADER_SIZE
    val columnSize = calculateColumnSize()
    val fieldSize = calculateFieldSize()
    return headerSize + columnSize + fieldSize
}

internal fun IesStruct.calculateColumnSize(): Int = columns.size * COLUMN_SIZE

internal fun IesStruct.calculateFieldSize(): Int = classes.sumOf { it.calculateSize() }

internal fun IesStructClass.calculateSize(): Int {
    val numbersSize = numbers.size * Float.SIZE_BYTES
    val stringsSize = strings.sumOf { UShort.SIZE_BYTES + it.utf8Length }
    val usesScriptFunctionsSize = usesScriptFunctions.size * Byte.SIZE_BYTES
    return UInt.SIZE_BYTES + (UShort.SIZE_BYTES + className.utf8Length) + numbersSize + stringsSize + usesScriptFunctionsSize
}

internal fun IesStructHeader.calculateSize(): Int = HEADER_SIZE

internal fun IesStructColumn.calculateSize(): Int = COLUMN_SIZE