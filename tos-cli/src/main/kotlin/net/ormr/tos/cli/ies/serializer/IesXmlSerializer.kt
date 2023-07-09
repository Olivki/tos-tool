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

package net.ormr.tos.cli.ies.serializer

import net.ormr.tos.cli.*
import net.ormr.tos.ies.element.*
import org.jdom2.Element
import org.jdom2.xpath.XPathHelper.getAbsolutePath
import java.nio.file.Path

data object IesXmlSerializer : IesSerializer {
    override fun encodeToFile(table: IesTable, file: Path) {
        val document = createDocument("table") {
            attr("name", table.name)
            attr("flag1", table.header.flag1)
            attr("flag2", table.header.flag2)
            attr("unknown", table.header.unknown)
            element("columns") {
                for (column in table.columns) {
                    element("column") {
                        attr("name", column.name)
                        attr("key", column.key)
                        attr("type", column.type.name)
                        attr("unk1", column.unk1)
                        attr("unk2", column.unk2)
                        attr("pos", column.position)
                    }
                }
            }
            element("rows") {
                for (row in table.rows) {
                    element("row") {
                        attr("id", row.id)
                        attr("key", row.key)
                        for (value in row.values) {
                            element("data") {
                                attr("ref", value.column.key)
                                val valueAttr = when (value) {
                                    is IesFloat32Value -> {
                                        val data = value.data
                                        // if data is a whole number, we don't want to print the decimal point
                                        if (data % 1 != 0F) data.toString() else data.toInt().toString()
                                    }
                                    is IesString1Value, is IesString2Value -> value.data
                                }
                                attr("value", valueAttr)
                                if (value is IesStringValue) attr("flag", value.flag)
                            }
                        }
                    }
                }
            }
        }
        document.writeTo(file)
    }

    @Suppress("UNCHECKED_CAST")
    override fun decodeFromFile(file: Path): IesTable {
        val document = loadDocument(file)
        val root = document.rootElement
        val table = IesTable(
            header = IesHeader(
                name = root.attr("name"),
                flag1 = root.intAttr("flag1"),
                flag2 = root.shortAttr("flag2"),
                unknown = root.shortAttr("unknown"),
            ),
        )

        val mappedColumns = hashMapOf<String, IesColumn<*>>()
        for (columnElement in root.child("columns").getChildren("column")) {
            val column: IesColumn<*> = IesColumn(
                name = columnElement.attr("name"),
                key = columnElement.attr("key"),
                type = when (val typeName = columnElement.attr("type")) {
                    "float32" -> IesType.Float32
                    "string1" -> IesType.String1
                    "string2" -> IesType.String2
                    else -> error("Unknown column type $typeName at ${getAbsolutePath(columnElement)}")
                },
                unk1 = columnElement.shortAttr("unk1"),
                unk2 = columnElement.shortAttr("unk2"),
                position = columnElement.shortAttr("pos"),
            )
            if (column.key in mappedColumns) {
                error("Duplicate column name ${column.name} at ${getAbsolutePath(columnElement)}")
            }
            mappedColumns[column.key] = column
            table.columns.add(column)
        }

        for (rowElement in root.child("rows").getChildren("row")) {
            val row = IesRow(
                id = rowElement.intAttr("id"),
                key = rowElement.attr("key"),
                values = rowElement.getChildren("data").mapTo(mutableListOf()) { dataElement ->
                    val ref = dataElement.attr("ref")
                    val column = mappedColumns[ref]
                        ?: error("Unknown column $ref at ${getAbsolutePath(dataElement)}")
                    val value: IesValue<*, *> = when (column.type) {
                        IesType.Float32 -> IesFloat32Value(
                            value = dataElement.floatAttr("value"),
                            column = column as IesColumn<Float>,
                        )
                        IesType.String1 -> IesString1Value(
                            value = dataElement.attr("value"),
                            column = column as IesColumn<String>,
                            flag = dataElement.booleanAttr("flag"),
                        )
                        IesType.String2 -> IesString2Value(
                            value = dataElement.attr("value"),
                            column = column as IesColumn<String>,
                            flag = dataElement.booleanAttr("flag"),
                        )
                    }
                    value
                },
            )
            table.rows.add(row)
        }

        return table
    }

    private fun Element.attr(name: String): String =
        getAttributeValue(name) ?: error("Attribute $name not found at ${getAbsolutePath(this)}")

    private fun Element.byteAttr(name: String): Byte = try {
        attr(name).toByte()
    } catch (e: NumberFormatException) {
        error("Attribute $name is not a byte at ${getAbsolutePath(this)}")
    }

    private fun Element.booleanAttr(name: String): Boolean = try {
        attr(name).toBooleanStrict()
    } catch (e: IllegalArgumentException) {
        error("Attribute $name is not a boolean at ${getAbsolutePath(this)}")
    }

    private fun Element.shortAttr(name: String): Short = try {
        attr(name).toShort()
    } catch (e: NumberFormatException) {
        error("Attribute $name is not a short at ${getAbsolutePath(this)}")
    }

    private fun Element.intAttr(name: String): Int = try {
        attr(name).toInt()
    } catch (e: NumberFormatException) {
        error("Attribute $name is not an integer at ${getAbsolutePath(this)}")
    }

    private fun Element.floatAttr(name: String): Float = try {
        attr(name).toFloat()
    } catch (e: NumberFormatException) {
        error("Attribute $name is not a float at ${getAbsolutePath(this)}")
    }

    private fun Element.child(name: String): Element =
        getChild(name) ?: error("Child $name not found at ${getAbsolutePath(this)}")
}