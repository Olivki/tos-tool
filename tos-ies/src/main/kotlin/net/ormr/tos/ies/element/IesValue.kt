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

import net.ormr.tos.ies.internal.element.IesFloat32ValueImpl
import net.ormr.tos.ies.internal.element.IesString1ValueImpl
import net.ormr.tos.ies.internal.element.IesString2ValueImpl

sealed interface IesValue<T : Any, out IT : IesType<T>> : IesElement {
    val column: IesColumn<T>
    val type: IT
    var data: T
}

interface IesFloat32Value : IesValue<Float, IesType.Float32> {
    override val type: IesType.Float32
        get() = IesType.Float32
}

fun IesFloat32Value(
    value: Float,
    column: IesColumn<Float>,
): IesFloat32Value = IesFloat32ValueImpl(value, column)

sealed interface IesStringValue<IT : IesType<String>> : IesValue<String, IT> {
    var flag: Boolean
}

interface IesString1Value : IesStringValue<IesType.String1> {
    override val type: IesType.String1
        get() = IesType.String1
}

fun IesString1Value(
    value: String,
    column: IesColumn<String>,
    flag: Boolean = false,
): IesString1Value = IesString1ValueImpl(value, column, flag)

interface IesString2Value : IesStringValue<IesType.String2> {
    override val type: IesType.String2
        get() = IesType.String2
}

fun IesString2Value(
    value: String,
    column: IesColumn<String>,
    flag: Boolean = false,
): IesString2Value = IesString2ValueImpl(value, column, flag)