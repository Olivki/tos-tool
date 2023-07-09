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

import kotlin.String as KString

sealed class IesType<T : Any>(val id: Short, val name: KString) : IesElement {
    fun isSameTypeAs(other: IesType<*>): Boolean = (this == other) || (this is String && other is String)

    data object Float32 : IesType<Float>(id = 0, name = "float32")

    sealed class String(id: Short, name: KString) : IesType<KString>(id, name)

    data object String1 : String(id = 1, name = "string1")

    data object String2 : String(id = 2, name = "string2")
}