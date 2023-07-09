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
import com.github.ajalt.mordant.animation.ProgressAnimation
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.rendering.TextColors.blue
import net.ormr.tos.DirectByteBuffer
import net.ormr.tos.cli.Sys.availableProcessors
import net.ormr.tos.ipf.IpfElement
import net.ormr.tos.ipf.IpfFile
import net.ormr.tos.ipf.IpfFooter
import net.ormr.tos.putUInt
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
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
        .defaultLazy { (availableProcessors / 2).coerceAtLeast(1) }
        .validate {
            require(it > 0) { "Number of threads must be greater than 0" }
            require(it <= availableProcessors) { "Number of threads can be max $availableProcessors" }
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
        val maxNameLength = files.maxOf { it.relativeTo(input).pathString.length }
        val fileWord = if (files.size == 1) "file" else "files"
        echo("Extracting ${blue(files.size.toString())} $fileWord using ${blue(threadsCount.toString())} threads...")
        files.forEach { unpackFile(it, maxNameLength) }
        pool.shutdown()
    }

    private fun unpackFile(file: Path, maxNameLength: Int) {
        val ipf = IpfFile.read(file)
        val progress = currentContext.terminal.progressAnimation {
            text(blue(file.relativeTo(input).pathString).padEnd(maxNameLength))
            percentage()
            progressBar()
            completed()
            timeRemaining()
        }
        progress.start()
        progress.updateTotal(ipf.elements.sumOf { it.uncompressedSize.toLong() })
        val directory = output / ipf.archiveName
        directory.createDirectories()
        saveFooter(ipf.footer, directory)
        pool.invokeAll(ipf.elements.map {
            Callable {
                unpackElement(it, directory, progress)
            }
        })
        Thread.sleep(300)
        progress.stop()
    }

    private fun saveFooter(footer: IpfFooter, directory: Path) {
        val file = directory / "version"
        Files.newByteChannel(file, CREATE, WRITE, TRUNCATE_EXISTING).use { channel ->
            val buffer = DirectByteBuffer(8)
            buffer.putUInt(footer.version)
            buffer.putUInt(footer.subversion)
            channel.write(buffer)
        }
    }

    private fun unpackElement(element: IpfElement, directory: Path, progress: ProgressAnimation) {
        val buffer = element.readAsByteBuffer()
        val file = directory.resolve(element.path)
        file.createParentDirectories()
        FileChannel.open(file, CREATE, WRITE, TRUNCATE_EXISTING).use { it.write(buffer) }
        progress.advance(element.uncompressedSize.toLong())
    }
}