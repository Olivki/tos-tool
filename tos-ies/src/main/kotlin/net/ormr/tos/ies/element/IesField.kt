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

package net.ormr.tos.ies.element

import net.ormr.tos.ies.IesType

sealed interface IesField<V, T : IesType<V & Any>> {
    val column: IesColumn<T>
    val value: V

    val type: T
        get() = column.type

    val name: String
        get() = column.name
}

data class IesNumber(
    override val column: IesColumn<IesType.Number>,
    override val value: Float,
) : IesField<Float, IesType.Number>

sealed interface IesStringField<T : IesType<String>> : IesField<String?, T> {
    val usesScriptFunction: Boolean
}

data class IesLocalizedString(
    override val column: IesColumn<IesType.LocalizedString>,
    override val value: String?,
    override val usesScriptFunction: Boolean,
) : IesStringField<IesType.LocalizedString>

data class IesCalculatedString(
    override val column: IesColumn<IesType.CalculatedString>,
    override val value: String?,
    override val usesScriptFunction: Boolean,
) : IesStringField<IesType.CalculatedString>