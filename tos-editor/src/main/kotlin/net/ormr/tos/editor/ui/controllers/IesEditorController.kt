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
import net.ormr.tos.editor.utils.readIesTable
import net.ormr.tos.ies.element.*
import tornadofx.*
import java.nio.file.Path
import kotlin.error

class IesEditorController(val file: Path) : Controller() {
    val selectedIndexProperty = SimpleIntegerProperty(0)
    var selectedIndex by selectedIndexProperty
    val iesTable: IesTable by lazy {
        readIesTable(file)
    }

    // TODO: more efficient method
    val pageCount by lazy { iesTable.rows.asSequence().chunked(100).count() }
    val dataRows by lazy {
        iesTable
            .rows
            .map { row ->
                val dataRow = DataRow(row)
                for (value in row.values) {
                    dataRow.data.add(
                        when (value) {
                            is IesString1Value -> Data.IesString(value, SimpleStringProperty(value.data))
                            is IesString2Value -> Data.IesString(value, SimpleStringProperty(value.data))
                            is IesFloat32Value -> Data.IesFloat(value, SimpleFloatProperty(value.data))
                        }
                    )
                }
                dataRow
            }
            .toObservable()
    }

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
        val data = mutableListOf<Data<*, *>>()
    }

    sealed interface Data<T : Any, out V : IesValue<T, *>> {
        val value: V

        data class IesString(
            override val value: IesStringValue,
            val property: SimpleStringProperty,
        ) : Data<String, IesStringValue>

        data class IesFloat(
            override val value: IesFloat32Value,
            val property: SimpleFloatProperty,
        ) : Data<Float, IesFloat32Value>
    }
}