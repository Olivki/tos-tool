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

import net.ormr.tos.getNullTerminatedString
import net.ormr.tos.ies.internal.shiftBits
import net.ormr.tos.putNullTerminatedString
import java.nio.ByteBuffer

internal class IesStructColumn : IesStruct, Comparable<IesStructColumn> {
    lateinit var name: String // 64 bytes
    lateinit var key: String // 64 bytes
    lateinit var type: IesStructDataType
    var unk1: Short = 0
    var unk2: Short = 0
    var pos: Short = 0

    override fun readFrom(buffer: ByteBuffer) {
        name = buffer.getNullTerminatedString(64).shiftBits()
        key = buffer.getNullTerminatedString(64).shiftBits()
        type = when (val typeId = buffer.getShort()) {
            0.toShort() -> IesStructDataType.Float32
            1.toShort() -> IesStructDataType.String1
            2.toShort() -> IesStructDataType.String2
            else -> error("Unknown type: $typeId")
        }
        unk1 = buffer.getShort()
        unk2 = buffer.getShort()
        pos = buffer.getShort()
    }

    override fun writeTo(buffer: ByteBuffer) {
        buffer.putNullTerminatedString(name.shiftBits(), 64)
        buffer.putNullTerminatedString(key.shiftBits(), 64)
        buffer.putShort(type.id)
        buffer.putShort(unk1)
        buffer.putShort(unk2)
        buffer.putShort(pos)
    }

    override fun getSize(): Int = (64 * 2) + (Short.SIZE_BYTES * 2) + Int.SIZE_BYTES

    override fun compareTo(other: IesStructColumn): Int = when {
        type.isSameTypeAs(other.type) -> pos.compareTo(other.pos)
        type.id < other.type.id -> -1
        else -> 1
    }

    override fun toString(): String =
        "IesStructColumn(name='$name', name2='$key', type=$type, unk1=$unk1, unk2=$unk2, pos=$pos)"

    companion object {
        inline operator fun invoke(body: IesStructColumn.() -> Unit): IesStructColumn = IesStructColumn().apply(body)
    }
}