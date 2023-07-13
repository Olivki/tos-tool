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

package net.ormr.tos.editor.utils

import atlantafx.base.theme.Styles
import atlantafx.base.theme.Tweaks
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ButtonBase
import javafx.scene.control.MenuButton

fun Node.addStyleClass(className: String) {
    styleClass.add(className)
}

fun Node.addStyleClasses(vararg classNames: String) {
    styleClass.addAll(classNames)
}

fun Node.removeStyleClass(className: String) {
    styleClass.remove(className)
}

fun Node.swapStyleClass(originalClass: String, newClass: String) {
    styleClass.remove(originalClass)
    styleClass.add(newClass)
}

fun Node.removeStyleClasses(vararg classNames: String) {
    styleClass.removeAll(classNames.toSet())
}

fun ButtonBase.applyTitleBarStyle() {
    addStyleClass("window-button")
}

fun Button.applyFlatButtonStyle() {
    addStyleClasses(Styles.FLAT, Styles.BUTTON_ICON)
}

fun MenuButton.applyFlatMenuButtonStyle(noArrow: Boolean = true) {
    addStyleClasses(Styles.FLAT, Styles.BUTTON_ICON)
    if (noArrow) addStyleClass(Tweaks.NO_ARROW)
}