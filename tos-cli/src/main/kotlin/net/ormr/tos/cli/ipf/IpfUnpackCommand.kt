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
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.rendering.TextColors.blue
import net.ormr.tos.cli.Sys
import net.ormr.tos.ipf.IpfExtractor
import net.ormr.tos.ipf.IpfReader
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.io.path.*

class IpfUnpackCommand : CliktCommand(name = "unpack") {
    private val input by argument()
        .help("Input file or directory")
        .path(mustExist = true, mustBeReadable = true, canBeDir = true, canBeFile = true, canBeSymlink = false)
    private val output by option("-o", "--output")
        .help("Output directory")
        .path()
        .defaultLazy { Path("./unpacked_ipf/") }
    private val threadsCount by option("-t", "--threads")
        .help("Number of threads to use")
        .int()
        .defaultLazy { (Sys.availableProcessors / 2).coerceAtLeast(1) }
        .validate {
            require(it > 0) { "Number of threads must be greater than 0" }
            require(it <= Sys.availableProcessors) { "Number of threads can be max ${Sys.availableProcessors}" }
        }
    private val pool by lazy { Executors.newFixedThreadPool(threadsCount) }

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        output.createDirectories()
        val files = input.walk().toList()
        if (files.isEmpty()) {
            echo("No files found")
            return
        }
        val fileWord = if (files.size == 1) "file" else "files"
        echo("Unpacking ${blue(files.size.toString())} $fileWord using ${blue(threadsCount.toString())} threads...")
        files.forEach(::unpackIpfFile)
        pool.shutdown()
    }

    private fun unpackIpfFile(file: Path) {
        val ipf = IpfReader.readFrom(file)
        val archiveName = ipf.elements.first().archiveName
        val rootDirectory = output.resolve(archiveName)
        rootDirectory.createDirectories()
        val progress = currentContext.terminal.progressAnimation {
            text(blue(file.relativeTo(input).pathString))
            percentage()
            progressBar()
            completed()
            timeRemaining()
        }
        progress.start()
        progress.updateTotal(ipf.elements.sumOf { it.uncompressedSize.toLong() })
        saveIpfDataTo(rootDirectory, IpfData(archiveName, ipf.subversion, ipf.version))
        val extractor = IpfExtractor(ipf)
        pool.invokeAll(ipf.elements.map { element ->
            Callable {
                with(extractor) {
                    element.extractTo(rootDirectory)
                    progress.advance(element.uncompressedSize.toLong())
                }
            }
        })
        Thread.sleep(300)
        progress.stop()
    }
}