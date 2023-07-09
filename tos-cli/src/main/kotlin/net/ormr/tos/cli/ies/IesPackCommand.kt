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
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.animation.textAnimation
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextColors.blue
import net.ormr.tos.cli.ies.serializer.IesSerializer
import net.ormr.tos.cli.ies.serializer.IesXmlSerializer
import net.ormr.tos.cli.t
import net.ormr.tos.ies.element.IesTable
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import kotlin.io.path.*

class IesPackCommand : CliktCommand(name = "pack") {
    private val input by argument()
        .help("Input file or directory")
        .path(mustExist = true, mustBeReadable = true, canBeDir = true, canBeFile = true, canBeSymlink = false)
    private val output by option("-o", "--output")
        .help("Output directory")
        .path()
        .defaultLazy { Path("./unpacked_ies/") }

    // TODO: implement automatic format finding and shit
    private val format: IesSerializer by option("-f", "--format")
        .help("Output format")
        .choice("xml" to IesXmlSerializer)
        .default(IesXmlSerializer)

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        output.createDirectories()
        val files = input.walk().toList()
        if (files.isEmpty()) {
            echo("No files found")
            return
        }
        val progress = t.textAnimation<Progress> {
            "Packing ${blue(it.currentFile)}.. ${TextColors.gray("(${it.current}/${it.total})")}"
        }
        t.cursor.hide(showOnExit = true)
        for ((i, file) in files.withIndex()) {
            packFile(file)
            progress.update(Progress(file.name, files.size, i + 1))
        }
        progress.stop()
    }

    private data class Progress(val currentFile: String, val total: Int, val current: Int)

    private fun packFile(file: Path) {
        val table = format.decodeFromFile(file)
        writeIesTable(table, output / "${file.nameWithoutExtension}.ies")
    }

    private fun writeIesTable(table: IesTable, output: Path) {
        FileChannel.open(output, WRITE, CREATE, TRUNCATE_EXISTING).use { channel ->
            channel.write(table.toByteBuffer())
        }
    }
}