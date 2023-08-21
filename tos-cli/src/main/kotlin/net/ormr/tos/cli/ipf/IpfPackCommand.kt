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
import net.ormr.tos.cli.setupFormatter
import net.ormr.tos.cli.t
import net.ormr.tos.ipf.IpfFileBuilder
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.io.path.*
import kotlin.system.exitProcess

class IpfPackCommand : CliktCommand(name = "pack") {
    private val input by argument()
        .help("Input directory")
        .path(mustExist = true, mustBeReadable = true, canBeDir = true, canBeFile = false, canBeSymlink = false)
    private val output by option("-o", "--output")
        .help("Output file or directory")
        .path()
        .defaultLazy { Path("./packed_ipf/") }
    private val threadsCount by option("-t", "--threads")
        .help("Number of threads to use, will default to half of the available processors")
        .int()
        .defaultLazy { (Sys.availableProcessors / 2).coerceAtLeast(1) }
        .validate {
            require(it > 0) { "Number of threads must be greater than 0" }
            require(it <= Sys.availableProcessors) { "Number of threads can be max ${Sys.availableProcessors}" }
        }
    private val level by option("-l", "--level")
        .help("Compression level to use, must be between 0 and 9")
        .int()
        .default(8)
        .validate {
            require(it in 0..9) { "Compression level must be between 0 and 9" }
        }
    private val pool by lazy { Executors.newFixedThreadPool(threadsCount) }

    init {
        setupFormatter()
    }

    override fun run() {
        val inputData = findIpfDataFile(input)

        // TODO: do similar thing for IpfUnpackCommand
        val directories = if (inputData.exists()) {
            // then input is a single ipf directory
            listOf(IpfDirectory(input, loadIpfDataFrom(input)))
        } else {
            val ipfDirectories = getAllIpfDirectories(input)
            if (ipfDirectories.isEmpty()) {
                // input isn't a direct ipf directory, nor does it contain any ipf directories
                // at this point we give up and just ask the user for feedback
                t.danger("Could not find any ipf directories to pack in '${input.pathString}'")
                val shouldContinue = confirm(
                    text = "Do you want to turn '${input.name}' into an ipf directory?",
                    default = true,
                ) ?: false
                if (!shouldContinue) exitProcess(1)
                echo("Please enter the data manually:")
                val data = promptForIpfData()
                saveIpfDataTo(input, data)
                listOf(IpfDirectory(input, data))
            } else {
                // then input is a directory containing multiple ipf directories
                ipfDirectories.map { IpfDirectory(it, loadIpfDataFrom(it)) }
            }
        }
        val isFileTarget: Boolean
        val target = if (output.name.endsWith(".ipf")) {
            if (directories.size > 1) {
                t.danger("Output file name ends with '.ipf', but input contains multiple ipf directories.")
                t.danger("This is not supported, please specify a directory as output instead.")
                exitProcess(1)
            }
            output.createParentDirectories()
            isFileTarget = true
            output
        } else {
            // then it's a directory target
            output.createDirectories()
            isFileTarget = false
            output
        }

        val fileWord = if (directories.size == 1) "file" else "files"
        echo("Packing ${blue(directories.size.toString())} $fileWord using ${blue(threadsCount.toString())} threads...")
        directories.forEach { packIpfDirectory(it, target, isFileTarget) }
        pool.shutdown()
    }

    private fun packIpfDirectory(ipfDirectory: IpfDirectory, target: Path, isFileTarget: Boolean) {
        val (directory, data) = ipfDirectory
        val entries = getIpfDirectoryEntries(ipfDirectory)
        if (entries.isEmpty()) {
            t.danger("No files found in '${directory.pathString}'")
            return
        }
        val ipfFile = if (isFileTarget) target else target / data.name
        packEntries(entries, data, ipfFile)
    }

    private fun packEntries(entries: List<IpfDirectoryEntry>, data: IpfData, ipfFile: Path) {
        val progress = currentContext.terminal.progressAnimation {
            text(blue(data.name))
            percentage()
            progressBar()
            completed()
            timeRemaining()
        }
        progress.start()
        progress.updateTotal(entries.sumOf { it.file.fileSize() })
        val builder = IpfFileBuilder(
            target = ipfFile,
            compressionLevel = level,
            subversion = data.subversion,
            version = data.version,
        )
        val actions = entries.map { entry ->
            Callable {
                builder.importFile(entry.file, entry.archive.directory, entry.archive.name)
                progress.advance(entry.file.fileSize())
            }
        }
        pool.invokeAll(actions)
        builder.writeEnd()
        Thread.sleep(300)
        progress.stop()
    }

    @OptIn(ExperimentalPathApi::class)
    private fun getIpfDirectoryEntries(ipfDirectory: IpfDirectory): List<IpfDirectoryEntry> = buildList {
        val (rootDirectory, data) = ipfDirectory
        val archiveDirectories = ArrayDeque<NamedDirectory>()
        rootDirectory.visitFileTree {
            onPreVisitDirectory { directory, _ ->
                if (directory == rootDirectory) {
                    archiveDirectories.addFirst(NamedDirectory(data.name, directory))
                } else if (directory.name.endsWith(".ipf")) {
                    archiveDirectories.addFirst(NamedDirectory(directory.name, directory))
                }
                FileVisitResult.CONTINUE
            }

            onPostVisitDirectory { directory, _ ->
                if (directory == rootDirectory || directory.name.endsWith(".ipf")) {
                    archiveDirectories.removeFirst()
                }
                FileVisitResult.CONTINUE
            }

            onVisitFile { file, _ ->
                if (file.name != ".ipf_data") {
                    add(IpfDirectoryEntry(file, archiveDirectories.first()))
                }
                FileVisitResult.CONTINUE
            }
        }
    }

    private data class IpfDirectoryEntry(val file: Path, val archive: NamedDirectory)

    private data class NamedDirectory(val name: String, val directory: Path)

    private fun promptForIpfData(): IpfData {
        val name = archiveNamePrompt() ?: error("Archive name is required")
        val subversion = uIntPrompt("Subversion") ?: error("Subversion is required")
        val version = uIntPrompt("Version") ?: error("Version is required")
        return IpfData(name, subversion, version)
    }

    private fun getAllIpfDirectories(root: Path): List<Path> = root.useDirectoryEntries { files ->
        files
            .filter { it.isDirectory() }
            .filter { findIpfDataFile(it).exists() }
            .toList()
    }

    private fun findIpfDataFile(directory: Path): Path = directory / ".ipf_data"

    private fun archiveNamePrompt(): String? = prompt(
        text = "Archive name",
        convert = { input ->
            when {
                input.isBlank() -> ConversionResult.Invalid("Input cannot be blank")
                !input.endsWith(".ipf") -> ConversionResult.Invalid("Input must end with '.ipf'")
                else -> ConversionResult.Valid(input)
            }
        },
        default = if (input.name.endsWith(".ipf")) input.name else "${input.name}.ipf",
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

    private data class IpfDirectory(val directory: Path, val data: IpfData)
}