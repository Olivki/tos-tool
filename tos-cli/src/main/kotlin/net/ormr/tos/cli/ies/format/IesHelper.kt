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

import net.ormr.tos.ies.IesKind

object IesHelper {
    private val kindSuffixes = listOf("CT_", "VP_", "EP_", "CP_")
    private val ntSuffixes = listOf("_NT", "_NT_DS1", "_NT_DS2")

    fun columnNameToStringKey(name: String, kind: IesKind): String {
        var value = name

        if (kind != IesKind.NORMAL) {
            value = value.drop(3)
        }

        return value.substringBefore("_NT")
    }

    fun isNTColumn(name: String): Boolean = "_NT" in name
}