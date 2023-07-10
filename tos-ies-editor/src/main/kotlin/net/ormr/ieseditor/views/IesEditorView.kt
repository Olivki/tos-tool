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

package net.ormr.ieseditor.views

import atlantafx.base.theme.Styles
import atlantafx.base.theme.Tweaks
import javafx.beans.property.SimpleFloatProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Alert
import javafx.util.StringConverter
import net.ormr.ieseditor.utils.addStyleClasses
import net.ormr.ieseditor.utils.customTextField
import net.ormr.ieseditor.utils.iesEditorConfig
import net.ormr.tos.ies.element.*
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_ONLY
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import kotlin.io.path.fileSize
import kotlin.io.path.name
import kotlin.io.path.notExists

class IesEditorView(private val file: Path) : View("IES Editor: ${file.name}") {
    private val iesTable by lazy {
        readIesTable(file)
    }

    @Suppress("UNCHECKED_CAST")
    private val gamer by lazy {
        iesTable.rows.map { row ->
            val dataRow = DataRow(row)
            for (value in row.values) {
                dataRow.data.add(
                    when (value) {
                        is IesStringValue<*> -> Data.IesString(
                            value as IesStringValue<IesType.String>,
                            SimpleStringProperty(value.data),
                        )
                        is IesFloat32Value -> Data.IesFloat(value, SimpleFloatProperty(value.data))
                    }
                )
            }
            dataRow
        }.toObservable()
    }

    private fun readIesTable(input: Path): IesTable = FileChannel.open(input, READ).use { channel ->
        val buffer = channel.map(READ_ONLY, 0, input.fileSize()).order(ByteOrder.LITTLE_ENDIAN)
        IesTable.readFromByteBuffer(buffer)
    }


    @Suppress("UNCHECKED_CAST")
    override val root = borderpane {
        prefWidth = 1280.0
        prefHeight = 720.0
        top = menubar {
            menu("File") {
                item("Open File..", keyCombination = "Ctrl+O") {
                    action {
                        val file = chooseIesFile()
                        if (file != null) openIesEditorView(file)
                    }
                }
                item("Open Recent File", keyCombination = "Ctrl+Shift+O") {
                    action {
                        val file = iesEditorConfig.lastIesFile
                        if (file != null) openIesEditorView(file)
                    }
                }
                menu("Recent Files") {

                }
                separator()
                item("Save", keyCombination = "Ctrl+S")
                item("Save As..", keyCombination = "Ctrl+Shift+S")
            }
            menu("Settings")
        }
        center = tableview(items = gamer) {
            addStyleClasses(Styles.STRIPED, Styles.DENSE, Tweaks.EDGE_TO_EDGE)
            for ((i, column) in iesTable.columns.sortedWith(IesColumn.BINARY_COMPARATOR).withIndex()) {
                when (column.type) {
                    is IesType.Float32 -> column(column.name) {
                        (it.value.data[i] as Data.IesFloat).property
                    }.useTextField(FloatConverter as StringConverter<Number>)
                    is IesType.String -> column(column.name) {
                        (it.value.data[i] as Data.IesString).property
                    }.makeEditable()
                }
            }
        }
        bottom = customTextField {
            left = FontIcon(Feather.SEARCH)
            promptText = "Search..."
            right = FontIcon(Feather.X)
        }
    }

    private object FloatConverter : StringConverter<Float>() {
        override fun toString(obj: Float?): String {
            requireNotNull(obj) { "null float not allowed" }
            return if (obj % 1 != 0F) obj.toString() else obj.toInt().toString()
        }

        override fun fromString(string: String?): Float = if (string.isNullOrBlank()) 0F else string.toFloat()
    }

    class DataRow(row: IesRow) {
        val idProperty = SimpleIntegerProperty(row.id)
        val keyProperty = SimpleStringProperty(row.key)
        val data = mutableListOf<Data<*, *>>()
    }

    sealed interface Data<T : Any, out V : IesValue<T, *>> {
        val value: V

        data class IesString(
            override val value: IesStringValue<IesType.String>,
            val property: SimpleStringProperty,
        ) : Data<String, IesStringValue<IesType.String>>

        data class IesFloat(
            override val value: IesFloat32Value,
            val property: SimpleFloatProperty,
        ) : Data<Float, IesValue<Float, IesType.Float32>>
    }

    private fun openIesEditorView(file: Path) {
        if (file.notExists()) {
            alert(Alert.AlertType.ERROR, "File Not Found", "File '${file.fileName}' does not exist.")
        } else {
            iesEditorConfig.lastDirectory = file.parent
            iesEditorConfig.lastIesFile = file
            IesEditorView(file).openWindow(
                escapeClosesWindow = false,
            )
        }
    }
}