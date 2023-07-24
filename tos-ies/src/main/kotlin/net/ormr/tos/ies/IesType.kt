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
            key.hasPrefix('C', 'P', '_') -> LocalizedString
            isNumber(value) -> Number
            else -> LocalizedString
        }

        private fun String.hasPrefix(a: Char, b: Char, c: Char): Boolean =
            this.length >= 3 && this[0] == a && this[1] == b && this[2] == c

        private fun isNumber(value: String): Boolean {
            if (value.isEmpty()) return false
            if (value[0] == '-') return true
            for (char in value) {
                if (char != ' ' && char != '.' && (char !in '0'..'9')) {
                    return false
                }
            }
            return true
        }
    }
}