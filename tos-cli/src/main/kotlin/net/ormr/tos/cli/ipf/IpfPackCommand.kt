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

package net.ormr.tos.cli.ipf

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.rendering.TextColors.blue
import com.github.ajalt.mordant.terminal.ConversionResult
import net.ormr.tos.cli.Sys
import net.ormr.tos.ipf.IpfFileBuilder
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.io.path.*

class IpfPackCommand : CliktCommand(name = "pack") {
    private val input by argument()
        .help("Input directory")
        .path(mustExist = true, mustBeReadable = true, canBeDir = true, canBeFile = false, canBeSymlink = false)
    private val output by argument()
        .help("Output file")
        .path()
    private val threadsCount by option("-t", "--threads")
        .help("Number of threads to use")
        .int()
        .defaultLazy { (Sys.availableProcessors / 2).coerceAtLeast(1) }
        .validate {
            require(it > 0) { "Number of threads must be greater than 0" }
            require(it <= Sys.availableProcessors) { "Number of threads can be max ${Sys.availableProcessors}" }
        }
    private val level by option("-l", "--level")
        .help("Compression level to use")
        .int()
        .default(8)
        .validate {
            require(it in 0..9) { "Compression level must be between 0 and 9" }
        }
    private val pool by lazy { Executors.newFixedThreadPool(threadsCount) }
    private val ipfDataFile by lazy { input / ".ipf_data" }

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        output.createParentDirectories()

        val ipfData = when {
            ipfDataFile.notExists() -> {
                echo("No '.ipf_data' file found in '${input.pathString}'")
                echo("Please enter the data manually:")
                val name = archiveNamePrompt() ?: error("Archive name is required")
                val subversion = uIntPrompt("Subversion") ?: error("Subversion is required")
                val version = uIntPrompt("Version") ?: error("Version is required")
                val data = IpfData(name, subversion, version)
                saveIpfDataTo(input, data)
                data
            }
            else -> loadIpfDataFrom(input)
        }

        val files = input.walk().filter { it.name != ".ipf_data" }.toList()
        if (files.isEmpty()) {
            echo("No files found", err = true)
            return
        }
        val fileWord = if (files.size == 1) "file" else "files"
        echo("Packing ${blue(files.size.toString())} $fileWord using ${blue(threadsCount.toString())} threads...")
        packFiles(files, ipfData)
        pool.shutdown()
    }

    private fun packFiles(files: List<Path>, data: IpfData) {
        val progress = currentContext.terminal.progressAnimation {
            text(blue(data.name))
            percentage()
            progressBar()
            completed()
            timeRemaining()
        }
        progress.start()
        progress.updateTotal(files.sumOf { it.fileSize() })
        val builder = IpfFileBuilder(
            root = input,
            compressionLevel = level,
            archiveName = data.name,
            subversion = data.subversion,
            version = data.version,
        )
        val actions = files.map { file ->
            Callable {
                builder.importFile(file)
                progress.advance(file.fileSize())
            }
        }
        pool.invokeAll(actions)
        /*files.forEach { file ->
            builder.importFile(file)
            progress.advance(file.fileSize())
        }*/
        builder.writeTo(output)
        Thread.sleep(300)
        progress.stop()
    }

    private fun archiveNamePrompt(): String? = prompt(
        text = "Archive name",
        convert = { input ->
            when {
                input.isBlank() -> ConversionResult.Invalid("Input cannot be blank")
                !input.endsWith(".ipf") -> ConversionResult.Invalid("Input must end with '.ipf'")
                else -> ConversionResult.Valid(input)
            }
        },
        default = if (input.name.endsWith(".ipf")) input.name else null,
    )

    private fun uIntPrompt(
        text: String,
        default: UInt? = null,
    ): UInt? = prompt(
        text = text,
        convert = { input ->
            try {
                ConversionResult.Valid(input.toUInt())
            } catch (e: NumberFormatException) {
                ConversionResult.Invalid("Invalid unsigned integer: $input")
            }
        },
        default = default,
    )
}