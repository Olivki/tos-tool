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

package net.ormr.tos.editor.ui.controllers

import javafx.beans.property.SimpleFloatProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TableColumn
import net.ormr.tos.editor.utils.readIesTable
import net.ormr.tos.ies.element.*
import tornadofx.*
import java.nio.file.Path
import kotlin.error

class IesEditorController(val file: Path) : Controller() {
    val iesTable: IesTable by lazy {
        readIesTable(file)
    }
    val columnByKey by lazy {
        iesTable.columns.associateBy { it.key }
    }
    private val duplicatedNames = hashSetOf<String>()
    private val columnKeyToColumn = hashMapOf<String, IesColumn<*>>()
    val dataRows = observableListOf<DataRow>()

    fun load() {
        val stored = hashMapOf<String, String>()

        fun getTitle(column: IesColumn<*>): String {
            val storedName = stored[column.name]
            val title = when {
                storedName == column.key -> column.name
                storedName == null -> {
                    stored[column.name] = column.key
                    column.name
                }
                storedName != column.key -> {
                    duplicatedNames += column.key
                    column.key
                }
                else -> error("This should never happen")
            }
            columnKeyToColumn[column.key] = column
            return title
        }

        val rows = iesTable
            .rows
            .map { row ->
                val dataRow = DataRow(row)
                for (value in row.values) {
                    val title = getTitle(value.column)
                    dataRow.columns.add(
                        when (value) {
                            is IesStringValue -> Data.IesString(title = title, value = value)
                            is IesFloat32Value -> Data.IesFloat(title = title, value = value)
                        }
                    )
                }
                dataRow
            }

        dataRows.setAll(rows)
    }

    fun hasDuplicatedName(key: String): Boolean = key in duplicatedNames

    fun getTitle(column: IesColumn<*>): String = when {
        hasDuplicatedName(column.key) -> column.key
        else -> column.name
    }

    fun getColumn(key: String): IesColumn<*> = columnKeyToColumn.getValue(key)

    fun getFloatProperty(data: Data<*, *>): SimpleFloatProperty = when (data) {
        is Data.IesFloat -> data.property
        else -> error("Expected 'Data.IesFloat', but got '${data::class.simpleName}'")
    }

    fun getStringProperty(data: Data<*, *>): SimpleStringProperty = when (data) {
        is Data.IesString -> data.property
        else -> error("Expected 'Data.IesFloat', but got '${data::class.simpleName}'")
    }

    class DataRow(row: IesRow) {
        val idProperty = SimpleIntegerProperty(row.id)
        val keyProperty = SimpleStringProperty(row.key)
        val columns = mutableListOf<Data<*, *>>()

        fun getColumn(index: Int): Data<*, *> = columns[index]
    }

    sealed interface Data<T : Any, out V : IesValue<T, *>> {
        val title: String
        val value: V

        data class IesString(
            override val title: String,
            override val value: IesStringValue,
        ) : Data<String, IesStringValue> {
            val property: SimpleStringProperty = SimpleStringProperty(null, title, value.data)
        }

        data class IesFloat(
            override val title: String,
            override val value: IesFloat32Value,
        ) : Data<Float, IesFloat32Value> {
            val property: SimpleFloatProperty = SimpleFloatProperty(null, title, value.data)
        }
    }
}

private var TableColumn<*, *>.iesKey: String
    get() = properties.getValue("net.ormr.iesKey") as String
    set(value) {
        properties["net.ormr.iesKey"] = value
    }