/*
 * Copyright 2022 Oliver Berg
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

import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

inline fun openFileChooser(
    title: String,
    initialDirectory: Path,
    filters: List<FileChooser.ExtensionFilter> = emptyList(),
    builder: FileChooser.() -> Unit = {},
): Path? = with(FileChooser()) {
    this.title = title
    this.initialDirectory = initialDirectory.toFile()
    this.extensionFilters.addAll(filters)
    builder()
    showOpenDialog(null)?.toPath()
}

inline fun openFileSaver(
    title: String,
    initialDirectory: Path,
    filters: List<FileChooser.ExtensionFilter> = emptyList(),
    builder: FileChooser.() -> Unit = {},
): Path? = with(FileChooser()) {
    this.title = title
    this.initialDirectory = initialDirectory.toFile()
    this.extensionFilters.addAll(filters)
    builder()
    showSaveDialog(null)?.toPath()
}

inline fun openMultiFileChooser(
    title: String,
    initialDirectory: Path,
    builder: FileChooser.() -> Unit = {},
): List<Path> = with(FileChooser()) {
    this.title = title
    this.initialDirectory = initialDirectory.toFile()
    builder()
    showOpenMultipleDialog(null)?.map { it.toPath() } ?: emptyList()
}

inline fun openDirectoryChooser(
    title: String,
    initialDirectory: Path,
    builder: DirectoryChooser.() -> Unit = {},
): Path? = with(DirectoryChooser()) {
    this.title = title
    this.initialDirectory = (initialDirectory.takeIf { it.exists() } ?: Path(".")).toFile()
    builder()
    showDialog(null)?.toPath()
}

fun filterOf(description: String, vararg extensions: String): FileChooser.ExtensionFilter =
    FileChooser.ExtensionFilter(description, *extensions)