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

package net.ormr.tos.ies

import net.ormr.tos.*
import net.ormr.tos.ies.IesType.*
import net.ormr.tos.ies.element.Ies
import net.ormr.tos.ies.element.IesColumn
import net.ormr.tos.ies.element.IesNumber
import net.ormr.tos.ies.element.IesStringField
import net.ormr.tos.ies.internal.*
import net.ormr.tos.ies.internal.struct.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*

object IesBinaryWriter {
    fun writeTo(ies: Ies, target: Path) {
        writeIesStruct(ies.toIesStruct(), target)
    }

    fun writeTo(ies: Ies, target: ByteBuffer) {
        writeIesStruct(ies.toIesStruct(), target)
    }

    fun writeBytes(ies: Ies): ByteArray {
        val struct = ies.toIesStruct()
        val bytes = ByteArray(struct.calculateSize())
        writeIesStruct(ies.toIesStruct(), bytes)
        return bytes
    }

    private fun Ies.toIesStruct(): IesStruct {
        val ies = this
        require(ies.id.isNotEmpty()) { "IES ID must not be empty" }
        val columns = columns.mapToArray { it.toIesStructColumn() }
        val columnSize = (columns.size * COLUMN_SIZE).toUInt()
        val numberColumnsCount = columns.count { it.type.isNumber }
        val stringColumnsCount = columns.count { it.type.isString }
        val classes = classes.mapToArray { clz ->
            val numbers = clz
                .fields
                .filterIsInstance<IesNumber>()
                .mapToFloatArray(IesNumber::value)
            check(numbers.size == numberColumnsCount) {
                "Size of numbers (${numbers.size}) does not match the size of number columns ($numberColumnsCount)"
            }
            val stringFields = clz.fields.filterIsInstance<IesStringField<*>>()
            val strings = stringFields.mapToArray { it.value ?: "" }
            check(strings.size == stringColumnsCount) {
                "Size of strings (${strings.size}) does not match the size of string columns ($stringColumnsCount)"
            }
            val usesScriptFunctions = stringFields.mapToBooleanArray(IesStringField<*>::usesScriptFunction)
            IesStructClass(
                classID = clz.classID,
                className = clz.className,
                numbers = numbers,
                strings = strings,
                usesScriptFunctions = usesScriptFunctions,
            )
        }
        val classSize = classes.sumOf { it.calculateSize() }.toUInt()
        return IesStruct(
            header = IesStructHeader(
                id = ies.id,
                keyID = ies.keyID ?: "",
                version = 1u,
                columnSize = columnSize,
                classSize = classSize,
                totalSize = HEADER_SIZE.toUInt() + columnSize + classSize,
                useClassID = ies.useClassID,
                classCount = classes.size.toUShortSafe { "classCount" },
                columnCount = columns.size.toUShortSafe { "columnCount" },
                numberColumnsCount = numberColumnsCount.toUShortSafe { "numberColumnsCount" },
                stringColumnsCount = stringColumnsCount.toUShortSafe { "stringColumnsCount" },
            ),
            columns = columns,
            classes = classes,
        )
    }

    private fun IesColumn<*>.toIesStructColumn(): IesStructColumn = IesStructColumn(
        stringKey = stringKey,
        name = name,
        type = type,
        kind = kind,
        isStatic = isStatic,
        index = index,
    )

    private inline fun Int.toUShortSafe(name: () -> String): UShort {
        check(this <= UShort.MAX_VALUE.toInt()) { "${name()} is too large for a u16 ($this > ${UShort.MAX_VALUE})" }
        return toUShort()
    }

    private fun writeIesStruct(struct: IesStruct, file: Path) {
        FileChannel.open(file, WRITE, CREATE, TRUNCATE_EXISTING).use { channel ->
            val buffer = DirectByteBuffer(struct.calculateSize() + 1, ByteOrder.LITTLE_ENDIAN)
            writeIesStruct(struct, buffer)
            buffer.flip()
            channel.write(buffer)
        }
    }

    private fun writeIesStruct(struct: IesStruct, buffer: ByteArray) {
        writeIesStruct(struct, ByteBuffer.wrap(buffer))
    }

    private fun writeIesStruct(struct: IesStruct, buffer: ByteBuffer) {
        val structSize = struct.calculateSize()
        val oldOrder = buffer.order()
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        require(buffer.limit() >= structSize) { "Buffer size must be at least equal to the size of the IES struct ($structSize > ${buffer.limit()})" }
        writeIesStructHeader(struct.header, buffer)
        struct.columns.forEach { writeIesStructColumn(it, buffer) }
        struct.classes.forEach { writeIesStructClass(it, buffer) }
        buffer.order(oldOrder)
    }

    private fun writeIesStructHeader(header: IesStructHeader, buffer: ByteBuffer) {
        checkStringLength(header.id, DEFAULT_STRING_LENGTH) { "IES ID" }
        buffer.putNullTerminatedString(header.id, DEFAULT_STRING_LENGTH)
        checkStringLength(header.keyID, DEFAULT_STRING_LENGTH) { "IES Key ID" }
        buffer.putNullTerminatedString(header.keyID, DEFAULT_STRING_LENGTH)
        buffer.putUInt(header.version)
        buffer.putUInt(header.columnSize)
        buffer.putUInt(header.classSize)
        buffer.putUInt(header.totalSize)
        buffer.put2ByteBoolean(header.useClassID)
        buffer.putUShort(header.classCount)
        buffer.putUShort(header.columnCount)
        buffer.putUShort(header.numberColumnsCount)
        buffer.putUShort(header.stringColumnsCount)
        buffer.skip(2)
    }

    private fun writeIesStructColumn(column: IesStructColumn, buffer: ByteBuffer) {
        checkStringLength(column.stringKey, DEFAULT_STRING_LENGTH) { "IES Column Name" }
        buffer.putNullTerminatedXorString(column.stringKey, DEFAULT_STRING_LENGTH)
        checkStringLength(column.name, DEFAULT_STRING_LENGTH) { "IES Column Key" }
        buffer.putNullTerminatedXorString(column.name, DEFAULT_STRING_LENGTH)
        buffer.putUShort(column.type.id)
        buffer.putUShort(column.kind.id)
        buffer.put2ByteBoolean(column.isStatic)
        buffer.putUShort(column.index)
    }

    private fun writeIesStructClass(clz: IesStructClass, buffer: ByteBuffer) {
        check(clz.strings.size == clz.usesScriptFunctions.size) {
            "Corrupt strings to usesScriptFunctions size (${clz.strings.size} != ${clz.usesScriptFunctions.size})"
        }
        buffer.putUInt(clz.classID)
        buffer.putIesString(clz.className)
        for (number in clz.numbers) buffer.putFloat(number)
        for (string in clz.strings) buffer.putIesString(string)
        for (usesScriptFunction in clz.usesScriptFunctions) buffer.putBoolean(usesScriptFunction)
    }

    private inline fun checkStringLength(string: String, maxSize: Int, name: () -> String) {
        check(string.utf8Length <= maxSize) { "${name()} ($string) is bigger than $maxSize bytes" }
    }
}