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

package net.ormr.tos.editor.ui.fragments

import atlantafx.base.theme.Styles
import atlantafx.base.theme.Tweaks
import javafx.scene.control.Alert.AlertType.INFORMATION
import javafx.scene.control.Label
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView
import javafx.scene.control.skin.TableColumnHeader
import javafx.scene.text.Text
import javafx.stage.Modality
import javafx.util.StringConverter
import net.ormr.tos.editor.ui.controllers.IesEditorController
import net.ormr.tos.editor.ui.controllers.IesEditorController.DataRow
import net.ormr.tos.editor.ui.views.chooseIesFile
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

class IesEditorFragment(file: Path) : Fragment("IES Editor: ${file.name}") {
    private val controller = IesEditorController(file)
    private val iesTable get() = controller.iesTable
    private lateinit var tableView: TableView<DataRow>

    // TODO: add support for adding new rows, and potentially removing rows?
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
                item("Save", keyCombination = "Ctrl+S") {
                    action {
                        alert(INFORMATION, "Not Implemented", "This feature is not implemented yet.")
                    }
                }
                item("Save As..", keyCombination = "Ctrl+Shift+S") {
                    action {
                        alert(INFORMATION, "Not Implemented", "This feature is not implemented yet.")
                    }
                }
            }
            menu("Settings")
            menu("Table") {
                menu("Columns")
                item("Open Info..") {
                    action {
                        IesEditorInfoFragment(iesTable).openWindow(
                            modality = Modality.WINDOW_MODAL,
                        )
                    }
                }
            }
        }
        center = vbox {
            tableView = tableview<DataRow> {
                addStyleClasses(Styles.STRIPED, Styles.BORDERED, Styles.DENSE, Tweaks.EDGE_TO_EDGE)
                column(title = "Row ID", DataRow::idProperty)
                // TODO: limit size of row key to the correct max utf8 length
                column(title = "Row Key", DataRow::keyProperty)
                for ((i, column) in iesTable.columns.sortedWith(IesColumn.BINARY_COMPARATOR).withIndex()) {
                    val title = column.key
                    when (column.type) {
                        is IesType.Float32 -> column(title) {
                            controller.getFloatProperty(it.value.getColumn(i))
                        }.useTextField(FloatConverter as StringConverter<Number>)
                        is IesType.String -> column(title) {
                            controller.getStringProperty(it.value.getColumn(i))
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

    override fun onDock() {
        val node = MaskPane().apply {
            center = Label("Loading..")
        }
        root.runAsyncWithOverlay(node) {
            controller.load()
        } success {
            tableView.items = controller.dataRows
            title =
                "IES Editor: ${controller.file.name} (${iesTable.columns.size} columns, ${iesTable.rows.size} rows)"
            // TODO: this is naive in case 'Row ID' is not the first column for some reason
            tableView.sortOrder.add(tableView.columns[0])
            // because JavaFX is such a good framework, we have to do this to add tooltips to the column headers
            tableView
                .lookupAll(".column-header")
                .asSequence()
                .filterIsInstance<TableColumnHeader>()
                .forEach { header ->
                    val label = header.lookup(".label") as? Label
                    if (label != null) {
                        val columnKey = label.text
                        label.contextmenu {
                            item(name = "Minimize") {
                                action {
                                    val tableColumn = header.tableColumn
                                    tableColumn.prefWidth = tableColumn.minWidth
                                }
                            }
                        }
                        if (columnKey.startsWith("Row")) return@forEach
                        val column = controller.getColumn(columnKey)
                        label.text = controller.getTitle(column)
                        val tooltipText = when {
                            controller.hasDuplicatedName(columnKey) -> column.name
                            else -> column.key
                        }
                        label.tooltip(text = tooltipText)
                    }
                }
            // god i love javafx so much
            tableView.columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY
            tableView.columns.forEach { column ->
                // TODO: add enough space for the title to show up
                val title = Text(column.text)
                val titleWidth = title.layoutBounds.width
                var width = titleWidth
                for (i in 0..<tableView.items.size) {
                    //cell must not be empty
                    if (column.getCellData(i) != null) {
                        val text = Text(column.getCellData(i).toString())
                        val textWidth = text.layoutBounds.width
                        if (textWidth > width) {
                            width = textWidth
                        }
                    }
                }
                //set the new max-widht with some extra space
                //column.setPrefWidth(width.coerceAtLeast(titleWidth) + 10.0)
                column.setPrefWidth(width.coerceAtLeast(titleWidth) + 10.0)
            }
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