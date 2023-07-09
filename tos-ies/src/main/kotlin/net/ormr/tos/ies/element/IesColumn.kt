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

import net.ormr.tos.ies.internal.element.IesColumnImpl

interface IesColumn<T : Any> : IesElement {
    var name: String // 64 bytes
    var key: String // 64 bytes // should be unique
    val type: IesType<T>
    var position: Short
    var unk1: Short
    var unk2: Short

    companion object {
        val VISUAL_COMPARATOR: Comparator<IesColumn<*>> = Comparator { o1, o2 ->
            when {
                o1.position > o2.position -> 1
                o1.position < o2.position -> -1
                o1.type.id > o2.type.id -> 1
                o1.type.id < o2.type.id -> -1
                else -> 0
            }
        }

        val BINARY_COMPARATOR: Comparator<IesColumn<*>> = Comparator { o1, o2 ->
            when {
                o1.type.isSameTypeAs(o2.type) -> o1.position.compareTo(o2.position)
                o1.type.id < o2.type.id -> -1
                else -> 1
            }
        }
    }
}

fun <T : Any> IesColumn(
    name: String,
    key: String,
    type: IesType<T>,
    position: Short,
    unk1: Short,
    unk2: Short,
): IesColumn<T> = IesColumnImpl(name, key, type, position, unk1, unk2)