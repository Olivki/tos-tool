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

package net.ormr.tos.cli

import org.jdom2.*
import org.jdom2.input.SAXBuilder
import org.jdom2.input.sax.XMLReaders
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

internal val defaultXmlFormat: Format by lazy { Format.getPrettyFormat().setIndent("  ") }

internal val defaultXmlOutputter: XMLOutputter by lazy { XMLOutputter(defaultXmlFormat) }

internal val compactXmlOutputter: XMLOutputter by lazy { XMLOutputter(Format.getCompactFormat()) }

internal fun Document.encodeToString(outputter: XMLOutputter = defaultXmlOutputter): String =
    outputter.outputString(this)

internal fun Document.encodeToStream(stream: OutputStream, outputter: XMLOutputter = defaultXmlOutputter) {
    outputter.output(this, stream)
}

internal fun Element.encodeToString(outputter: XMLOutputter = defaultXmlOutputter): String =
    outputter.outputString(this)

internal fun toCompactString(element: Element): String = element.encodeToString(compactXmlOutputter)

internal fun loadDocument(input: String): Document =
    input.reader().use { createSax().build(it) }

internal fun loadDocument(input: Path): Document =
    input.inputStream().use { createSax().build(it) }

private fun createSax(): SAXBuilder = SAXBuilder(XMLReaders.NONVALIDATING).apply {
    jdomFactory = SlimJDOMFactory()
    reuseParser = false
}

internal fun Document.writeTo(file: Path, outputter: XMLOutputter = defaultXmlOutputter): Path {
    file.outputStream().use { outputter.output(this, it) }
    return file
}

internal inline fun <T> Document.use(scope: (doc: Document, root: Element) -> T): T = run { scope(this, rootElement) }

internal inline fun createDocument(
    rootName: String,
    rootNamespace: Namespace = Namespace.NO_NAMESPACE,
    builder: Element.() -> Unit = {},
): Document = Document(Element(rootName, rootNamespace)).apply { rootElement.builder() }

// TODO: context instead of receiver element
inline fun Element.element(
    name: String,
    namespace: Namespace? = getNamespace(),
    builder: Element.() -> Unit,
): Element = (namespace?.let { Element(name, it) } ?: Element(name)).also {
    it.builder()
    addContent(it)
}

fun Element.attr(
    name: String,
    value: String,
    namespace: Namespace? = getNamespace(),
): Attribute {
    val attribute = Attribute(name, value, namespace)
    attributes.add(attribute)
    return attribute
}

fun Element.attr(
    name: String,
    value: Any,
    namespace: Namespace? = getNamespace(),
): Attribute = attr(name, value.toString(), namespace)

internal inline fun createElement(
    name: String,
    namespace: Namespace? = null,
    builder: Element.() -> Unit,
): Element = (namespace?.let { Element(name, it) } ?: Element(name)).apply { builder(this) }

operator fun Element.get(name: String): String =
    getAttributeValue(name) ?: throw IllegalArgumentException(
        "Missing required attribute '$name' on: ${encodeToString(compactXmlOutputter)}"
    )