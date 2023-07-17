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

package net.ormr.tos.cli.ies

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.animation.textAnimation
import com.github.ajalt.mordant.rendering.TextColors
import net.ormr.tos.cli.ies.serializer.IesSerializer
import net.ormr.tos.cli.ies.serializer.IesXmlSerializer
import net.ormr.tos.cli.t
import net.ormr.tos.ies.element.IesTable
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import kotlin.io.path.*

class IesTestCommand : CliktCommand(name = "test") {
    private val input by argument()
        .help("Input file or directory")
        .path(mustExist = true, mustBeReadable = true, canBeDir = true, canBeFile = true, canBeSymlink = false)
    private val format: IesSerializer by option("-f", "--format")
        .help("Output format")
        .choice("xml" to IesXmlSerializer)
        .default(IesXmlSerializer)

    private val output by lazy { createTempDirectory(prefix = "tos-tool") }

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        //val files = input.walk().toList()
        val boys = listOf("ies", "ies2", "iesOLD", "ies_client", "ies_ability", "ies_drop", "ies_mongen")
        val files = boys.flatMap { input.resolve(it).walk().filter { f -> f.name.endsWith(".ies") }.toList() }
        if (files.isEmpty()) {
            echo("No files found")
            return
        }
        val progress = t.textAnimation<Progress> {
            "Packing ${TextColors.blue(it.currentFile)}.. ${TextColors.gray("(${it.current}/${it.total})")}"
        }
        t.cursor.hide(showOnExit = true)
        for ((i, file) in files.withIndex()) {
            progress.update(Progress(file.relativeTo(input).toString(), files.size, i + 1))
            testFile(file)
        }
        progress.stop()
        output.deleteRecursively()
        println(things)
        println(things2)
    }

    private data class Progress(val currentFile: String, val total: Int, val current: Int)

    private val things = hashSetOf<Short>()
    private val things2 = hashMapOf<Short, String>()

    private fun testFile(file: Path) {
        val table = readIesTable(file)
        if (table.header.unknown != 0.toShort()) {
            echo("Unknown header value for file ${file.name}")
        }
        if (table.header.flag1 != 1) {
            echo("Flag1 header value for file ${file.name}")
        }
        /*if (table.header.flag2 == 0.toShort() && table.header.flag2 != 1.toShort()) {
            echo("Flag2 header value for file ${file.name}: ${table.header.flag2}")
        }*/
        for ((i, column) in table.columns.withIndex()) {
            things.add(column.unk1)
            val key = column.key
            if (key.length >= 3 && column.unk1 != 3.toShort()) {
                if (key[2] == '_') {
                    val prefix = key.take(2)
                    val knownPrefix = things2[column.unk1]
                    if (knownPrefix != null && knownPrefix != prefix) {
                        echo("Unknown prefix for file ${file.relativeTo(input)}/${column.name}:$i: $prefix")
                    } else {
                        things2[column.unk1] = prefix
                    }
                }
            }
            if (column.unk2 != 0.toShort() && column.unk2 != 1.toShort()) {
                echo("Unknown column value for file ${file.relativeTo(input)}/${column.name}:$i: ${column.unk2}")
            }
        }
        /*val binaryTable = file.readBytes()
        val xmlTable = getBytes(file)
        if (!binaryTable.contentEquals(xmlTable)) {
            error("Binary and XML table do not match for file ${file.name}")
        }*/
    }

    private fun writeIesTable(table: IesTable, output: Path) {
        FileChannel.open(output, WRITE, CREATE, TRUNCATE_EXISTING).use { channel ->
            channel.write(table.toByteBuffer())
        }
    }

    private fun getBytes(file: Path): ByteArray {
        val table = readIesTable(file)
        val target = output / "${file.nameWithoutExtension}.xml"
        format.encodeToFile(table, target)
        val xmlTable = format.decodeFromFile(target)
        val iesTarget = output / "${file.nameWithoutExtension}.ies"
        writeIesTable(xmlTable, iesTarget)
        readIesTable(iesTarget)
        return iesTarget.readBytes()
    }

    private fun readIesTable(input: Path): IesTable = FileChannel.open(input, READ).use { channel ->
        val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, input.fileSize()).order(ByteOrder.LITTLE_ENDIAN)
        IesTable.readFromByteBuffer(buffer)
    }
}