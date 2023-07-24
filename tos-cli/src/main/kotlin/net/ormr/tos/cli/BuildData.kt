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

package net.ormr.tos.cli

import com.google.common.collect.Maps
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.properties.Properties
import net.ormr.tos.cli.utils.loadPropertiesFromResource

sealed interface BuildData {
    val version: String

    companion object Default : BuildData by buildDataInstance
}

@OptIn(ExperimentalSerializationApi::class)
private val buildDataInstance by lazy {
    val properties = loadPropertiesFromResource<BuildData>("/build_data.properties")
    Properties.decodeFromMap(BuildDataImpl.serializer(), Maps.fromProperties(properties))
}

@Serializable
private data class BuildDataImpl(override val version: String) : BuildData