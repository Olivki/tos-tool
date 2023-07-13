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

package net.ormr.tos.editor.utils

import javafx.scene.control.Alert
import net.ormr.tos.editor.ui.views.IesEditorView
import net.ormr.tos.ies.element.IesTable
import tornadofx.*
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.fileSize
import kotlin.io.path.notExists

fun readIesTable(input: Path): IesTable = FileChannel.open(input, StandardOpenOption.READ).use { channel ->
    val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, input.fileSize()).order(ByteOrder.LITTLE_ENDIAN)
    IesTable.readFromByteBuffer(buffer)
}

fun openIesEditorView(file: Path) {
    if (file.notExists()) {
        alert(Alert.AlertType.ERROR, "File Not Found", "File '${file.fileName}' does not exist.")
    } else {
        tosEditorConfig.lastDirectory = file.parent
        tosEditorConfig.lastIesFile = file
        IesEditorView(file).openWindow(
            escapeClosesWindow = false,
        )
    }
}