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

package net.ormr.tos.ies.internal.element

import net.ormr.tos.ies.element.IesColumn
import net.ormr.tos.ies.element.IesFloat32Value
import net.ormr.tos.ies.element.IesString1Value
import net.ormr.tos.ies.element.IesString2Value

internal data class IesFloat32ValueImpl(
    override var data: Float,
    override val column: IesColumn<Float>,
) : IesFloat32Value, InternalIesElementImpl

internal data class IesString1ValueImpl(
    override var data: String,
    override val column: IesColumn<String>,
    override var flag: Boolean,
) : IesString1Value, InternalIesElementImpl

internal data class IesString2ValueImpl(
    override var data: String,
    override val column: IesColumn<String>,
    override var flag: Boolean,
) : IesString2Value, InternalIesElementImpl