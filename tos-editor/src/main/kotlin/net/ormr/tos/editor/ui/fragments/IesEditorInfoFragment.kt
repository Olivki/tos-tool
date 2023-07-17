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

import javafx.event.EventTarget
import net.ormr.tos.ies.element.IesTable
import tornadofx.*

class IesEditorInfoFragment(table: IesTable) : Fragment("IES Editor Table Info") {
    private val structTable = table.toStructTable().apply {
        header.updateValues()
    }

    // TODO: list view?
    override val root = vbox(spacing = 10.0) {
        infoField("Name", table.name)
        titledpane(title = "Columns") {
            vbox(spacing = 10.0) {
                infoField("Size", structTable.getColumnsSize().toString())
                infoField("Count", structTable.header.columnCount.toString())
                infoField("Number Count", structTable.header.intColumns.toString())
                infoField("String Count", structTable.header.stringColumns.toString())
            }
        }
        titledpane(title = "Rows") {
            vbox(spacing = 10.0) {
                infoField("Size", structTable.getRowsSize().toString())
                infoField("Count", structTable.header.rowCount.toString())
            }
        }
    }

    private fun EventTarget.infoField(name: String, value: String) {
        hbox(spacing = 10.0) {
            label(text = name)
            label(text = value)
        }
    }
}