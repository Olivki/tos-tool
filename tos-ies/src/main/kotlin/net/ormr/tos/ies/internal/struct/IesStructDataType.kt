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

import net.ormr.tos.getString
import net.ormr.tos.getUShort
import net.ormr.tos.ies.internal.shiftBits
import net.ormr.tos.ies.internal.utf8SizeOf
import net.ormr.tos.putString
import net.ormr.tos.putUShort
import java.nio.ByteBuffer
import kotlin.String as KString

sealed class IesStructDataType(val id: Short, val name: KString) {
    abstract fun decodeFrom(buffer: ByteBuffer): Any

    abstract fun encodeTo(buffer: ByteBuffer, value: Any)

    abstract fun getSizeOf(value: Any): Int

    // TODO: int variants might be unsigned integers?
    data object Float32 : IesStructDataType(id = 0, name = "float32") {
        override fun decodeFrom(buffer: ByteBuffer): Float = buffer.getFloat()

        override fun encodeTo(buffer: ByteBuffer, value: Any) {
            require(value is Float) { "Value must be of type 'Float', was '${value.javaClass.name}'" }
            buffer.putFloat(value)
        }

        override fun getSizeOf(value: Any): Int {
            require(value is Float) { "Value must be of type 'Int', was '${value.javaClass.name}'" }
            return Int.SIZE_BYTES
        }
    }

    sealed class String(id: Short, name: KString) : IesStructDataType(id, name) {
        override fun decodeFrom(buffer: ByteBuffer): KString {
            val length = buffer.getUShort().toInt()
            return buffer.getString(length).shiftBits()
        }

        override fun encodeTo(buffer: ByteBuffer, value: Any) {
            require(value is KString) { "Value must be of type 'String', was '${value.javaClass.name}'" }
            buffer.putUShort(value.length.toUShort())
            buffer.putString(value.shiftBits())
        }

        override fun getSizeOf(value: Any): Int {
            require(value is KString) { "Value must be of type 'String', was '${value.javaClass.name}'" }
            return UShort.SIZE_BYTES + utf8SizeOf(value)
        }
    }

    data object String1 : String(id = 1, name = "string1")

    data object String2 : String(id = 2, name = "string2")
}