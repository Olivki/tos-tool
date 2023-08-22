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

@file:Suppress("NOTHING_TO_INLINE")

package net.ormr.tos.ies

sealed class IesKind(val id: UShort) {
    data object EP : IesKind(id = 0u)
    data object CP : IesKind(id = 1u) // Calculated Property
    data object VP : IesKind(id = 2u)
    data object NORMAL : IesKind(id = 3u)
    data object CT : IesKind(id = 4u)

    companion object {
        fun fromId(id: UShort): IesKind = when (id) {
            0u.toUShort() -> EP
            1u.toUShort() -> CP
            2u.toUShort() -> VP
            3u.toUShort() -> NORMAL
            4u.toUShort() -> CT
            else -> throw IllegalArgumentException("Unknown IES kind: $id")
        }

        fun fromKey(key: String): IesKind = when {
            key.length >= 3 -> when {
                key.hasPrefix('E', 'P', '_') -> EP
                key.hasPrefix('C', 'P', '_') -> CP
                key.hasPrefix('V', 'P', '_') -> VP
                key.hasPrefix('C', 'T', '_') -> CT
                else -> NORMAL
            }
            else -> NORMAL
        }

        private inline fun String.hasPrefix(a: Char, b: Char, c: Char): Boolean =
            this[0] == a && this[1] == b && this[2] == c
    }
}