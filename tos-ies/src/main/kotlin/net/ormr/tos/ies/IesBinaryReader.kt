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
import net.ormr.tos.ies.element.*
import net.ormr.tos.ies.internal.DEFAULT_STRING_LENGTH
import net.ormr.tos.ies.internal.getIesString
import net.ormr.tos.ies.internal.getNullTerminatedXorString
import net.ormr.tos.ies.internal.struct.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_ONLY
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ

object IesBinaryReader {
    fun readFrom(file: Path): Ies = readIesStruct(file).toIes()

    fun readFrom(buffer: ByteBuffer): Ies = readIesStruct(buffer).toIes()

    fun readFrom(bytes: ByteArray): Ies = readIesStruct(bytes).toIes()

    @Suppress("UNCHECKED_CAST")
    private fun IesStruct.toIes(): Ies {
        val struct = this
        val columns = columns.map { it.toIesColumn() }
        val classes = buildList(header.classCount.toInt()) {
            for (clz in struct.classes) {
                val fieldsArray = arrayOfNulls<IesField<*, *>>(clz.numbers.size + clz.strings.size)
                for (j in 0..<header.columnCount.toInt()) {
                    val column = columns[j]
                    val columnIndex = column.index.toInt()
                    val field: IesField<*, *> = when (column.type) {
                        IesType.Number -> IesNumber(column as IesColumn<IesType.Number>, clz.numbers[columnIndex])
                        IesType.LocalizedString -> iesString(column, clz, columnIndex, ::IesLocalizedString)
                        IesType.CalculatedString -> iesString(column, clz, columnIndex, ::IesCalculatedString)
                    }
                    val arrayIndex = when (column.type) {
                        IesType.Number -> columnIndex
                        IesType.LocalizedString, IesType.CalculatedString -> clz.numbers.size + columnIndex
                    }
                    fieldsArray[arrayIndex] = field
                }
                val fields = fieldsArray.requireNoNulls().asList()
                add(IesClass(classID = clz.classID, className = clz.className.ifEmpty { null }, fields = fields))
            }
        }
        return Ies(
            id = struct.header.id,
            keyID = struct.header.keyID.ifEmpty { null },
            useClassID = struct.header.useClassID,
            columns = columns,
            classes = classes
        )
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <T : IesType<*>, V : IesStringField<T>> iesString(
        column: IesColumn<*>,
        field: IesStructClass,
        index: Int,
        constructor: (IesColumn<T>, String?, Boolean) -> V,
    ): V = constructor(
        column as IesColumn<T>,
        field.strings[index].ifEmpty { null },
        field.usesScriptFunctions[index],
    )

    private fun IesStructColumn.toIesColumn(): IesColumn<*> = IesColumn(
        stringKey = stringKey,
        name = name,
        type = type,
        kind = kind,
        isNT = isNT,
        index = index,
    )

    private fun readIesStruct(file: Path): IesStruct = FileChannel.open(file, READ).use { channel ->
        val buffer = channel.map(READ_ONLY, 0, channel.size()).order(ByteOrder.LITTLE_ENDIAN)
        readIesStruct(buffer)
    }

    private fun readIesStruct(bytes: ByteArray): IesStruct = readIesStruct(ByteBuffer.wrap(bytes))

    private fun readIesStruct(buffer: ByteBuffer): IesStruct {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val header = readIesStructHeader(buffer)
        val columns = Array(header.columnCount.toInt()) { readIesStructColumn(buffer) }
        val classes = Array(header.classCount.toInt()) { readIesStructClass(header, buffer) }
        val struct = IesStruct(header, columns, classes)
        val structSize = struct.calculateSize().toUInt()
        check(structSize == header.totalSize) {
            "Calculated totalSize not same as read totalSize. ($structSize != ${header.totalSize})"
        }
        return struct
    }

    private fun readIesStructHeader(buffer: ByteBuffer): IesStructHeader {
        val id = buffer.getNullTerminatedString(DEFAULT_STRING_LENGTH)
        val keyID = buffer.getNullTerminatedString(DEFAULT_STRING_LENGTH)
        val version = buffer.getUInt()
        check(version == 1u) { "Unsupported version: $version" }
        val columnSize = buffer.getUInt()
        val fieldSize = buffer.getUInt()
        val totalSize = buffer.getUInt()
        val useClassID = buffer.get2ByteBoolean()
        val fieldCount = buffer.getUShort()
        val columnCount = buffer.getUShort()
        val numberColumnsCount = buffer.getUShort()
        val stringColumnsCount = buffer.getUShort()
        check((numberColumnsCount + stringColumnsCount).toUShort() == columnCount) {
            "(numberColumnsCount ($numberColumnsCount) + stringColumnsCount ($stringColumnsCount)) != columnCount ($columnCount)"
        }
        // checking around 4k files, this is always two bytes of 0
        buffer.skipPadding(2)
        return IesStructHeader(
            id = id,
            keyID = keyID,
            version = version,
            columnSize = columnSize,
            classSize = fieldSize,
            totalSize = totalSize,
            useClassID = useClassID,
            classCount = fieldCount,
            columnCount = columnCount,
            numberColumnsCount = numberColumnsCount,
            stringColumnsCount = stringColumnsCount,
        )
    }

    private fun readIesStructColumn(buffer: ByteBuffer): IesStructColumn {
        val name = buffer.getNullTerminatedXorString(DEFAULT_STRING_LENGTH)
        val key = buffer.getNullTerminatedXorString(DEFAULT_STRING_LENGTH)
        val type = IesType.fromId(buffer.getUShort())
        val kind = IesKind.fromId(buffer.getUShort())
        val isNT = buffer.get2ByteBoolean()
        val index = buffer.getUShort()
        return IesStructColumn(
            stringKey = name,
            name = key,
            type = type,
            kind = kind,
            isNT = isNT,
            index = index,
        )
    }

    private fun readIesStructClass(header: IesStructHeader, buffer: ByteBuffer): IesStructClass {
        val classID = buffer.getUInt()
        val className = buffer.getIesString()
        val numbersSize = header.numberColumnsCount.toInt()
        val stringsSize = header.stringColumnsCount.toInt()
        val numbers = FloatArray(numbersSize) { buffer.getFloat() }
        val strings = Array(stringsSize) { buffer.getIesString() }
        val useScriptFunctions = BooleanArray(stringsSize) { buffer.getBoolean() }
        return IesStructClass(
            classID = classID,
            className = className,
            numbers = numbers,
            strings = strings,
            usesScriptFunctions = useScriptFunctions,
        )
    }
}