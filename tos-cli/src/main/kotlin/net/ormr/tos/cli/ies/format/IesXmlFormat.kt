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

import net.ormr.tos.cli.*
import net.ormr.tos.cli.ies.IesFormatCommand
import net.ormr.tos.ies.IesKind
import net.ormr.tos.ies.IesType
import net.ormr.tos.ies.element.*
import org.jdom2.Element
import org.jdom2.xpath.XPathHelper.getAbsolutePath
import java.nio.file.Path

class IesXmlFormat(command: IesFormatCommand) : IesFormat(name = "xml", command = command) {
    override val fileExtension: String
        get() = "xml"

    // Here be dragons, this code is *not* the best, and could definitely be rewritten to be a lot better,
    // but it works and that's good enough for now.

    // the order of the columns don't get kept the *exact* same because we put ClassID and ClassName at the start
    // but that's fine because the order of the columns *shouldn't* matter, as what's important is that
    // the class fields get mapped to the correct columns, which they do.

    // TODO: looking at the files we have access to, I don't think the current system does anything with
    //       non standard xml files, so we might not want to support arbitrary nesting?
    override fun loadFrom(file: Path): Ies? {
        val columns = linkedMapOf<String, IesColumn<*>>()
        val columnTypes = hashMapOf<String, IesType<*>>()
        val tracker = DataTracker()
        val document = loadDocument(file)
        val root = document.rootElement
        // if the loaded file isn't idspace file, we can't pack it, so we return null
        if (root.name != "idspace") return null
        val id = root.attr("id")
        val keyID = root.getAttributeValue("keyid")?.let { it.ifEmpty { null } }
        fillColumnTypes(file, root, columnTypes)
        fillColumns(root, columnTypes, columns, tracker)
        val classes = getIesClasses(root, columns, tracker)
        return Ies(
            id = id,
            keyID = keyID,
            useClassID = tracker.useClassID,
            columns = columns.values.toList().sorted(),
            classes = classes,
        )
    }

    private class DataTracker(
        var numbers: UShort = 0u,
        var strings: UShort = 0u,
        var useClassID: Boolean = false,
    )

    private fun fillColumnTypes(file: Path, root: Element, columnTypes: MutableMap<String, IesType<*>>) {
        for (child in root.children) {
            if (child.name != "Class") {
                fillColumnTypes(file, child, columnTypes)
                continue
            }
            for (attribute in child.attributes) {
                val name = attribute.name
                val value = attribute.value
                val cachedType = columnTypes[name]
                val inferredType = IesType.fromKeyValue(name, value)

                if (cachedType == null) {
                    columnTypes[name] = inferredType
                } else {
                    // if a value we previously inferred as a number is used as a string,
                    // then it's most likely a string that can contain number values, so we change it accordingly
                    if (cachedType == IesType.Number && inferredType == IesType.LocalizedString) {
                        columnTypes[name] = IesType.LocalizedString
                    }
                }
            }
        }
    }

    private fun fillColumns(
        root: Element,
        columnTypes: Map<String, IesType<*>>,
        columns: MutableMap<String, IesColumn<*>>,
        tracker: DataTracker,
    ) {
        for (child in root.children) {
            if (child.name != "Class") {
                fillColumns(child, columnTypes, columns, tracker)
                continue
            }

            for (attribute in child.attributes) {
                val name = attribute.name
                columns.getOrPut(name) {
                    val type = columnTypes[name]
                        ?: error("Unknown column type for '$name' @ ${getAbsolutePath(attribute)}")
                    val kind = IesKind.fromKey(name)
                    val isNT = IesHelper.isNTColumn(name)
                    val stringKey = IesHelper.columnNameToStringKey(name, kind)
                    val index = when (type) {
                        IesType.Number -> tracker.numbers++
                        IesType.LocalizedString, IesType.CalculatedString -> tracker.strings++
                    }.toUShort()
                    IesColumn(
                        stringKey = stringKey,
                        name = name,
                        type = type,
                        kind = kind,
                        index = index,
                        isNT = isNT,
                    )
                }
            }

            if ("ClassName" !in columns) {
                columns["ClassName"] = IesColumn(
                    stringKey = "ClassName",
                    name = "ClassName",
                    type = IesType.LocalizedString,
                    kind = IesKind.NORMAL,
                    index = tracker.strings++,
                    isNT = false,
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getIesClasses(
        root: Element,
        columns: Map<String, IesColumn<*>>,
        tracker: DataTracker,
    ): List<IesClass> = buildList {
        for (child in root.children) {
            if (child.name != "Class") {
                addAll(getIesClasses(child, columns, tracker))
                continue
            }
            val classID = child.getAttributeValue("ClassID")?.let {
                tracker.useClassID = true
                it.toUIntOrNull() ?: error("Invalid class ID '$it' @ ${getAbsolutePath(child)}")
            } ?: 0u
            val hasClassName = child.getAttribute("ClassName") != null
            val className = child
                .getAttributeValue("ClassName")
                ?.ifEmpty { null }
                ?.let { if (it == DEFAULT_STRING) null else it }
            val fields = buildList(child.attributes.size) {
                if (!hasClassName) {
                    add(IesLocalizedString(columns["ClassName"] as IesColumn<IesType.LocalizedString>, null, false))
                }
                for (attribute in child.attributes) {
                    val name = attribute.name
                    val value = attribute.value
                    val column = columns[name]
                        ?: error("Unknown column '$name' @ ${getAbsolutePath(attribute)}")
                    val field: IesField<*, *> = when (column.type) {
                        IesType.Number -> {
                            val number = value
                                .toFloatOrNull() ?: error("Invalid number '$value' @ ${getAbsolutePath(attribute)}")
                            IesNumber(
                                column = column as IesColumn<IesType.Number>,
                                value = number,
                            )
                        }
                        IesType.LocalizedString -> iesString(column, value, ::IesLocalizedString)
                        IesType.CalculatedString -> iesString(column, value, ::IesCalculatedString)
                    }
                    add(field)
                }
            }
            add(IesClass(classID = classID, className = className, fields = fields))
        }
    }

    override fun writeTo(file: Path, ies: Ies) {
        val formatter = BasicIesFieldFormatter.using(ies.classes)
        val document = newDocument(ies, formatter)
        document.writeTo(file)
    }

    private fun newDocument(ies: Ies, formatter: IesFieldFormatter) = createDocument(rootName = "idspace") { _ ->
        attr("id", ies.id)
        attr("keyid", ies.keyID ?: "")
        element("Category") {
            val serializeClassName = !(ies.classes.all { it.className == null })
            for (clz in ies.classes) {
                element("Class") {
                    if (ies.useClassID) {
                        attr("ClassID", clz.classID)
                    }
                    if (serializeClassName) {
                        attr("ClassName", clz.className ?: DEFAULT_STRING)
                    }

                    for (field in clz.fields) {
                        if (field.name == "ClassID" || field.name == "ClassName") continue
                        attr(field.name, formatter.formatValue(field))
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <T : IesType<*>, V : IesStringField<T>> iesString(
        column: IesColumn<*>,
        value: String,
        constructor: (IesColumn<T>, String?, Boolean) -> V,
    ): V = constructor(
        column as IesColumn<T>,
        if (value == DEFAULT_STRING) null else value,
        IesHelper.isScriptField(value),
    )

    private fun Element.attr(name: String): String =
        getAttributeValue(name) ?: error("Attribute $name not found at ${getAbsolutePath(this)}")

    private fun Element.child(name: String): Element =
        getChild(name) ?: error("Child $name not found at ${getAbsolutePath(this)}")
}