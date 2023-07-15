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

import java.nio.ByteBuffer

class IesStructData(val column: IesStructColumn) : IesStruct {
    lateinit var data: Any
    var flag: Byte = 0 // boolean

    val isString: Boolean
        get() = column.type is IesStructDataType.String

    inline fun readFrom(buffer: ByteBuffer, readData: (ByteBuffer) -> Any) {
        data = readData(buffer)
    }

    override fun readFrom(buffer: ByteBuffer) {
        readFrom(buffer, column.type::decodeFrom)
    }

    override fun writeTo(buffer: ByteBuffer) {
        column.type.encodeTo(buffer, data)
    }

    override fun getSize(): Int = column.type.getSizeOf(data)

    override fun toString(): String = "IesStructData(column=$column, data=$data, flag=$flag)"

    companion object {
        inline operator fun invoke(column: IesStructColumn, body: IesStructData.() -> Unit): IesStructData =
            IesStructData(column).apply(body)
    }
}