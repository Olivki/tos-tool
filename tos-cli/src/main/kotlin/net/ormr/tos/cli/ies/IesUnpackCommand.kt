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
import com.github.ajalt.clikt.parameters.groups.defaultByName
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.animation.textAnimation
import com.github.ajalt.mordant.rendering.TextColors.blue
import com.github.ajalt.mordant.rendering.TextColors.gray
import net.ormr.tos.cli.ies.format.IesCsvFormat
import net.ormr.tos.cli.ies.format.IesXmlFormat
import net.ormr.tos.cli.t
import net.ormr.tos.ies.element.IesTable
import java.nio.ByteOrder.LITTLE_ENDIAN
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_ONLY
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import kotlin.io.path.*

class IesUnpackCommand : CliktCommand(name = "unpack"), IesOutputParent {
    private val input by argument()
        .help("Input file or directory")
        .path(mustExist = true, mustBeReadable = true, canBeDir = true, canBeFile = true, canBeSymlink = false)
    override val output by option("-o", "--output")
        .help("Output directory")
        .path()
        .defaultLazy { Path("./unpacked_ies/") }
    private val format by option("-f", "--format")
        .help("Output format")
        .groupChoice(
            "xml" to IesXmlFormat(this),
            "csv" to IesCsvFormat(this),
        )
        .defaultByName("xml") // TODO: set it as required instead of default?

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        output.createDirectories()
        val files = input.walk().toList()
        if (files.isEmpty()) {
            echo("No files found")
            return
        }
        val progress = t.textAnimation<Progress> {
            "Unpacking ${blue(it.currentFile)}.. ${gray("(${it.current}/${it.total})")}"
        }
        t.cursor.hide(showOnExit = true)
        for ((i, file) in files.withIndex()) {
            unpackFile(file)
            progress.update(Progress(file.name, files.size, i + 1))
        }
        progress.stop()
    }

    private data class Progress(val currentFile: String, val total: Int, val current: Int)

    private fun unpackFile(file: Path) {
        val table = readIesTable(file)
        val extension = when (format) {
            is IesXmlFormat -> "xml"
            is IesCsvFormat -> "csv"
        }
        val target = output / "${file.nameWithoutExtension}.${extension}"
        format.save(table, target)
    }

    private fun readIesTable(input: Path): IesTable = FileChannel.open(input, READ).use { channel ->
        val buffer = channel.map(READ_ONLY, 0, input.fileSize()).order(LITTLE_ENDIAN)
        IesTable.readFromByteBuffer(buffer)
    }
}