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

@file:Suppress("CanSealedSubClassBeObject")

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
import net.ormr.tos.cli.ies.format.IesXmlFormat
import net.ormr.tos.cli.setupFormatter
import net.ormr.tos.cli.t
import net.ormr.tos.ies.IesBinaryWriter
import java.nio.file.Path
import kotlin.io.path.*

class IesPackCommand : CliktCommand(name = "pack"), IesFormatCommand {
    private val input by argument()
        .help("Input file or directory")
        .path(mustExist = true, mustBeReadable = true, canBeDir = true, canBeFile = true, canBeSymlink = false)
    private val output by option("-o", "--output")
        .help("Output directory")
        .path()
        .defaultLazy { Path("./packed_ies/") }
    private val format by option("-f", "--format")
        .help("Output format")
        .groupChoice(
            "xml" to IesXmlFormat(this),
        )
        .defaultByName("xml")

    init {
        setupFormatter()
    }

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        output.createDirectories()
        val files = input
            .walk()
            .filter { it.name.endsWith(".${format.fileExtension}") }
            .toList()
        if (files.isEmpty()) {
            echo("No files found")
            return
        }
        val progress = t.textAnimation<Progress> {
            "Packing ${blue(it.currentFile)}.. ${gray("(${it.current}/${it.total})")}"
        }
        t.cursor.hide(showOnExit = true)
        val isSingleFile = files.size == 1
        for ((i, file) in files.withIndex()) {
            packFile(file, isSingleFile)
            progress.update(Progress(file.name, files.size, i + 1))
        }
        progress.stop()
    }

    private fun packFile(file: Path, isSingleFile: Boolean) {
        val ies = format.loadFrom(file)
        if (ies == null) {
            if (isSingleFile) {
                t.danger("Root element of file '${file.name}' is not 'idspace' and can't be packed to an IES")
            }
            return
        }
        val newPath = when {
            input.isDirectory() -> "${file.relativeTo(input).pathString.dropFormatExtension()}.ies"
            else -> "${file.name.dropFormatExtension()}.ies"
        }
        val outputFile = output / newPath
        outputFile.createParentDirectories()
        IesBinaryWriter.writeTo(outputFile, ies)
    }

    private fun String.dropFormatExtension(): String = substringBeforeLast(".${format.fileExtension}")

    private data class Progress(val currentFile: String, val total: Int, val current: Int)

    override fun promptForContinue(text: String) {
        t.prompt(
            prompt = "$text. Continue?",
            choices = listOf("y", "n"),
        )
    }
}