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

import net.ormr.tos.cli.attr
import net.ormr.tos.cli.createDocument
import net.ormr.tos.cli.element
import net.ormr.tos.cli.writeTo
import net.ormr.tos.ies.element.*
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

    override fun decodeFromFile(file: Path): IesTable {
        TODO("Not yet implemented")
    }
}