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

package net.ormr.tos.ies

// CP_ -> Computed Property
// SP_ -> Static Property

sealed class IesType<T : Any>(val id: UShort) {
    data object Number : IesType<Float>(id = 0u)
    data object LocalizedString : IesType<String>(id = 1u)
    data object CalculatedString : IesType<String>(id = 2u)

    val isNumber: Boolean
        get() = id == Number.id

    val isString: Boolean
        get() = id == LocalizedString.id || id == CalculatedString.id

    fun isSameTypeAs(other: IesType<*>): Boolean = id == other.id || (isString && other.isString)

    companion object {
        fun fromId(id: UShort): IesType<*> = when (id) {
            0u.toUShort() -> Number
            1u.toUShort() -> LocalizedString
            2u.toUShort() -> CalculatedString
            else -> throw IllegalArgumentException("Invalid IES type ID: $id")
        }

        fun fromKeyValue(key: String, value: String): IesType<*> = when {
            key[0] == 'C' && key[1] == 'P' && key[2] == '_' -> CalculatedString
            isNumber(value) -> Number
            else -> LocalizedString
        }

        // TODO: better way to check if string is a number, this is ugly and hackish
        private fun isNumber(value: String): Boolean = try {
            value.toFloat()
            true
        } catch (_: NumberFormatException) {
            false
        }
    }
}