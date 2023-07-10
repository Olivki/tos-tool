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

package net.ormr.ieseditor.models

import javafx.beans.property.SimpleObjectProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.ormr.ieseditor.records.ConfigRecord
import net.ormr.ieseditor.utils.launchEffect
import tornadofx.*
import java.nio.file.Path

class ConfigModel(record: ConfigRecord) {
    val lastDirectoryProperty: SimpleObjectProperty<Path?> = SimpleObjectProperty(record.lastDirectory)
    var lastDirectory: Path? by lastDirectoryProperty

    val lastIesFileProperty: SimpleObjectProperty<Path?> = SimpleObjectProperty(record.lastIesFile)
    var lastIesFile: Path? by lastIesFileProperty

    private val _events = MutableSharedFlow<Unit>()

    val events: Flow<Unit> = _events.asSharedFlow()

    init {
        val listenerInstance = ChangeListener<Any?> { _, _, _ ->
            launchEffect {
                _events.emit(Unit)
            }
        }
        lastDirectoryProperty.addListener(listenerInstance)
        lastIesFileProperty.addListener(listenerInstance)
    }

    fun toRecord(): ConfigRecord = ConfigRecord(
        lastDirectory = lastDirectory,
        lastIesFile = lastIesFile,
    )
}