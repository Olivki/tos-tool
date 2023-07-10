/*
 * Copyright 2022 Oliver Berg
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

package net.ormr.ieseditor.utils

import atlantafx.base.controls.CustomTextField
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.scene.Node
import net.ormr.krautils.lang.ifNotNull
import tornadofx.*

inline fun EventTarget.customTextField(
    text: String? = null,
    leftGraphic: Node? = null,
    rightGraphic: Node? = null,
    builder: CustomTextField.() -> Unit,
): CustomTextField = CustomTextField(text).attachTo(this, builder) { element ->
    leftGraphic ifNotNull { element.left = it }
    rightGraphic ifNotNull { element.right = it }
}

inline fun EventTarget.customTextField(
    value: ObservableValue<String>,
    leftGraphic: Node? = null,
    rightGraphic: Node? = null,
    builder: CustomTextField.() -> Unit,
): CustomTextField = customTextField(leftGraphic = leftGraphic, rightGraphic = rightGraphic) {
    bind(value)
    builder()
}