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

import net.ormr.tos.ies.IesKind
import net.ormr.tos.ies.IesType

internal class IesStruct(
    val header: IesStructHeader,
    val columns: Array<IesStructColumn>,
    val classes: Array<IesStructClass>,
)

internal class IesStructHeader(
    val id: String, // 64
    val keyID: String, // 64
    val version: UInt,
    val columnSize: UInt,
    val classSize: UInt,
    val totalSize: UInt,
    val useClassID: Boolean, // UShort
    val classCount: UShort,
    val columnCount: UShort,
    val numberColumnsCount: UShort,
    val stringColumnsCount: UShort,
    // 2 bytes padding
)

internal class IesStructColumn(
    val stringKey: String, // 64
    val name: String, // 64
    val type: IesType<*>, // UShort
    val kind: IesKind, // UShort
    val isStatic: Boolean, // UShort
    val index: UShort,
) : Comparable<IesStructColumn> {
    override fun compareTo(other: IesStructColumn): Int = when {
        type.isSameTypeAs(other.type) -> index.compareTo(other.index)
        type.id < other.type.id -> -1
        else -> 1
    }
}

internal class IesStructClass(
    val classID: UInt,
    val className: String, // max size u16
    val numbers: FloatArray, // header.numberColumnCount
    val strings: Array<String>, // header.stringColumnCount
    val usesScriptFunctions: BooleanArray, // header.stringColumnCount
)