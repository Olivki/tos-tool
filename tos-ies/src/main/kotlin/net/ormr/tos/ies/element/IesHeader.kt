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

import net.ormr.tos.ies.internal.element.IesHeaderImpl

interface IesHeader : IesElement {
    var name: String // 128 byte length
    var flag1: Int
    var flag2: Short
    var unknown: Short
}

fun IesHeader(name: String, flag1: Int = 0, flag2: Short = 0, unknown: Short = 0): IesHeader =
    IesHeaderImpl(name, flag1, flag2, unknown)