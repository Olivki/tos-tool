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

package net.ormr.tos.editor.views

import atlantafx.base.theme.Styles
import atlantafx.base.theme.Tweaks
import javafx.scene.control.Label
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView
import javafx.util.StringConverter
import net.ormr.tos.editor.controllers.IesEditorController
import net.ormr.tos.editor.utils.addStyleClasses
import net.ormr.tos.editor.utils.customTextField
import net.ormr.tos.editor.utils.openIesEditorView
import net.ormr.tos.editor.utils.tosEditorConfig
import net.ormr.tos.ies.element.IesColumn
import net.ormr.tos.ies.element.IesType
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*
import java.nio.file.Path
import kotlin.io.path.name

class IesEditorView(file: Path) : View("IES Editor: ${file.name}") {
    private val controller = IesEditorController(file)
    private val iesTable get() = controller.iesTable
    private lateinit var tableView: TableView<IesEditorController.DataRow>

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
                        val file = tosEditorConfig.lastIesFile
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
        center = vbox {
            tableView = tableview<IesEditorController.DataRow> {
                addStyleClasses(Styles.STRIPED, Styles.BORDERED, Styles.DENSE, Tweaks.EDGE_TO_EDGE)
                for ((i, column) in iesTable.columns.sortedWith(IesColumn.BINARY_COMPARATOR).withIndex()) {
                    when (column.type) {
                        is IesType.Float32 -> column(column.name) {
                            controller.getFloatProperty(it.value.data[i])
                        }.useTextField(FloatConverter as StringConverter<Number>)
                        is IesType.String -> column(column.name) {
                            controller.getStringProperty(it.value.data[i])
                        }.makeEditable()
                    }
                }
                prefHeightProperty().bind(this@borderpane.prefHeightProperty())
                selectionModel.apply {
                    selectionMode = SelectionMode.SINGLE
                    isCellSelectionEnabled = true
                }
            }
        }
        bottom = customTextField {
            left = FontIcon(Feather.SEARCH)
            promptText = "Search..."
            right = FontIcon(Feather.X)
        }
    }

    override fun onBeforeShow() {
        val node = MaskPane().apply {
            center = Label("Loading..")
        }
        root.runAsyncWithOverlay(node) {
            controller.dataRows
        } success {
            tableView.items = it
        }
    }

    private object FloatConverter : StringConverter<Float>() {
        override fun toString(obj: Float?): String {
            requireNotNull(obj) { "null float not allowed" }
            return if (obj % 1 != 0F) obj.toString() else obj.toInt().toString()
        }

        override fun fromString(string: String?): Float = if (string.isNullOrBlank()) 0F else string.toFloat()
    }


}