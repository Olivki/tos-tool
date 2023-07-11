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

package net.ormr.tos.cli.ies.format

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import net.ormr.tos.cli.ies.serializer.IesSerializer
import net.ormr.tos.ies.element.IesTable
import java.nio.file.Path

sealed class IesFormat(name: String) : OptionGroup(name = name) {
    abstract val serializer: IesSerializer

    abstract fun load(file: Path): IesTable

    abstract fun save(table: IesTable, file: Path)
}