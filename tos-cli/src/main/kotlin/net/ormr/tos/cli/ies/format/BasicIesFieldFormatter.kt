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

import net.ormr.tos.ies.element.IesClass
import net.ormr.tos.ies.element.IesField
import net.ormr.tos.ies.element.IesNumber
import net.ormr.tos.ies.element.IesStringField
import java.util.*

class BasicIesFieldFormatter private constructor(private val intLikeNumbers: Set<String>) : IesFieldFormatter {
    override fun formatValue(field: IesField<*, *>): String = when (field) {
        is IesNumber -> when {
            field.name in intLikeNumbers -> field.value.toInt().toString()
            field.value == 0.0F -> DEFAULT_NUMBER
            else -> String.format(Locale.ROOT, "%f", field.value)
        }
        is IesStringField<*> -> field.value ?: DEFAULT_STRING
    }

    fun isIntLike(name: String): Boolean = name in intLikeNumbers

    companion object {
        fun using(classes: List<IesClass>): BasicIesFieldFormatter {
            val potentialInts = hashMapOf<String, Boolean>()
            for (clz in classes) {
                for (field in clz.fields) {
                    if (field is IesNumber) {
                        val hasDecimal = field.value % 1 != 0F
                        val previousCheck = potentialInts[field.name]
                        if (previousCheck != null) {
                            if (previousCheck) {
                                if (hasDecimal) {
                                    potentialInts[field.name] = false
                                }
                            }
                        } else {
                            if (!hasDecimal) {
                                potentialInts[field.name] = true
                            }
                        }
                    }
                }
            }
            val intLikeNumbers = potentialInts
                .entries
                .asSequence()
                .filter { (_, value) -> value }
                .mapTo(hashSetOf()) { it.key }
            return BasicIesFieldFormatter(intLikeNumbers)
        }
    }
}