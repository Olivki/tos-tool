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
import javafx.collections.ObservableList
import javafx.util.StringConverter
import net.ormr.ieseditor.controllers.IesEditorCsvController
import net.ormr.ieseditor.utils.*
import net.ormr.tos.ies.element.IesColumn
import net.ormr.tos.ies.element.IesFloat32Value
import net.ormr.tos.ies.element.IesString1Value
import net.ormr.tos.ies.element.IesString2Value
import org.controlsfx.control.spreadsheet.*
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*
import java.nio.file.Path
import kotlin.io.path.name

class IesEditorViewCsv(file: Path) : View("IES Editor: ${file.name}") {
    private val controller = IesEditorCsvController(file)
    private val iesTable get() = controller.iesTable
    private lateinit var tableView: SpreadsheetView

    //private lateinit var pagination: Pagination
    private var hasLoaded = false

    private fun createGrid(): Grid {
        val grid = GridBase(iesTable.rows.size, iesTable.columns.size)
        println(grid.columnHeaders)
        val headers = grid.columnHeaders
        for (column in iesTable.columns.sortedWith(IesColumn.BINARY_COMPARATOR)) {
            headers.add(column.name)
        }
        val rows = observableListOf<ObservableList<SpreadsheetCell>>()
        for ((rowIndex, row) in iesTable.rows.withIndex()) {
            val cells = observableListOf<SpreadsheetCell>()
            for ((columnIndex, value) in row.values.withIndex()) {
                when (value) {
                    is IesFloat32Value -> cells += SpreadsheetCellType.DOUBLE.createCell(
                        rowIndex,
                        columnIndex,
                        1,
                        1,
                        value.data.toDouble(),
                    )
                    is IesString1Value -> cells += SpreadsheetCellType.STRING.createCell(
                        rowIndex,
                        columnIndex,
                        1,
                        1,
                        value.data,
                    )
                    is IesString2Value -> cells += SpreadsheetCellType.STRING.createCell(
                        rowIndex,
                        columnIndex,
                        1,
                        1,
                        value.data,
                    )
                }
            }
            rows.add(cells)
        }
        grid.setRows(rows)
        return grid
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
        center = vbox {
            tableView = spreadsheetView(createGrid()) {
                addStyleClasses(Styles.STRIPED, Styles.DENSE, Tweaks.EDGE_TO_EDGE)
                prefHeightProperty().bind(this@borderpane.prefHeightProperty())
            }
            /*pagination = pagination {
                controller.selectedIndexProperty.bindBidirectional(currentPageIndexProperty())
                setPageFactory { i ->
                    if (hasLoaded) {
                        tableView.items[i].tableView.items.setAll(controller.dataRows[i])
                    }
                    StackPane()
                }
            }*/
        }
        bottom = customTextField {
            left = FontIcon(Feather.SEARCH)
            promptText = "Search..."
            right = FontIcon(Feather.X)
        }
    }

    /*override fun onBeforeShow() {
        val node = MaskPane().apply {
            center = Label("Loading..")
        }
        root.runAsyncWithOverlay(node) {
            controller.dataRows[0]
        } success {
            hasLoaded = true
            pagination.pageCount = controller.dataRows.size
            tableView.items.setAll(it)
        }
    }*/

    private object FloatConverter : StringConverter<Float>() {
        override fun toString(obj: Float?): String {
            requireNotNull(obj) { "null float not allowed" }
            return if (obj % 1 != 0F) obj.toString() else obj.toInt().toString()
        }

        override fun fromString(string: String?): Float = if (string.isNullOrBlank()) 0F else string.toFloat()
    }


}