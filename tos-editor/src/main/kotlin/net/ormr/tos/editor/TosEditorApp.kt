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

package net.ormr.tos.editor

import atlantafx.base.theme.NordDark
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.ormr.tos.editor.Constants.configFile
import net.ormr.tos.editor.models.ConfigModel
import net.ormr.tos.editor.records.ConfigRecord
import net.ormr.tos.editor.utils.launchEffect
import net.ormr.tos.editor.views.MainView
import tornadofx.*
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class TosEditorApp : App(MainView::class) {
    val configModel: ConfigModel by lazy {
        val record = loadConfigOrNull() ?: ConfigRecord()
        ConfigModel(record)
    }

    init {
        importStylesheet("/assets/styles/fonts.css")
        importStylesheet("/assets/styles/style.css")
    }

    @OptIn(FlowPreview::class)
    override fun start(stage: Stage) {
        launchEffect {
            withContext(Dispatchers.IO) {
                Constants.directory.createDirectories()
            }

            configModel
                .events
                .debounce(1_000)
                .collectLatest {
                    withContext(Dispatchers.IO) {
                        saveConfig()
                    }
                }
        }
        setUserAgentStylesheet(NordDark().userAgentStylesheet)
        super.start(stage)
    }

    private fun loadConfigOrNull(): ConfigRecord? {
        if (configFile.notExists()) return null
        return Json.decodeFromString(ConfigRecord.serializer(), configFile.readText())
    }

    private fun saveConfig() {
        configFile.writeText(Json.encodeToString(ConfigRecord.serializer(), configModel.toRecord()))
    }
}