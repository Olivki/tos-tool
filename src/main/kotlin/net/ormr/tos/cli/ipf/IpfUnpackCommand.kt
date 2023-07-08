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
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextStyles.italic
import net.ormr.tos.ies.IesUtil.getUShort
import net.ormr.tos.ipf.IpfElement
import net.ormr.tos.ipf.IpfFile
import net.ormr.tos.ipf.PkwareInputStream
import net.ormr.tos.ipf.getBytes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_ONLY
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import kotlin.io.path.*

class IpfUnpackCommand : CliktCommand(name = "unpack") {
    private val input by argument()
        .help("Input file or directory")
        .path(mustExist = true, mustBeReadable = true, canBeDir = true, canBeFile = true, canBeSymlink = false)
    private val output by option("-o", "--output")
        .help("Output directory")
        .path()
        .defaultLazy { Path("./unpacked_ies/") }
    private val threadsCount by option("-t", "--threads")
        .help("Number of threads to use")
        .int()
        .defaultLazy { (availableProcessors / 2).coerceAtLeast(1) }
        .validate {
            require(it > 0) { "Number of threads must be greater than 0" }
            require(it <= availableProcessors) { "Number of threads can be max $availableProcessors" }
        }

    private val availableProcessors: Int by lazy { Runtime.getRuntime().availableProcessors() }
    private val pool by lazy { Executors.newFixedThreadPool(threadsCount) }

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        output.createDirectories()
        val files = input.walk().toList()
        val maxNameLength = files.maxOf { it.relativeTo(input).pathString.length }
        val fileWord = if (files.size == 1) "file" else "files"
        echo("Extracting ${blue(files.size.toString())} $fileWord using ${blue(threadsCount.toString())} threads...")
        for (file in files) {
            unpack(file, maxNameLength)
        }
        pool.shutdown()
    }

    // TODO: there's probably more values that are being read as signed values that should be read as unsigned
    private fun unpack(file: Path, maxNameLength: Int) {
        val progress = currentContext.terminal.progressAnimation {
            text(blue(file.relativeTo(input).pathString).padEnd(maxNameLength))
            percentage()
            progressBar()
            completed()
            timeRemaining()
        }
        progress.start()
        var archiveDirectory: Path? = null
        var fileCount: UShort
        val tasks = mutableListOf<Callable<Unit>>()
        FileChannel.open(file, READ).use { channel ->
            val buffer = channel.map(READ_ONLY, 0, channel.size()).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(buffer.limit() - 0x18)
            /*val header = buffer.limit() - IpfElement.getTail().size - 4
            buffer.position(header)

            val jmp = buffer.getInt()
            buffer.position(jmp)*/

            fileCount = buffer.getUShort()

            // this is most likely a UInt, but because 'buffer.position' is an Int,
            // we can't do any meaningful conversions from UInt
            val fileTableOffset = buffer.getInt()
            buffer.getShort()
            buffer.getInt()
            buffer.getInt() // compression
            val ipf = IpfFile {
                subversion = buffer.getInt() // TODO: UInt
                version = buffer.getInt() // TODO: UInt
                archiveFile = file
            }
            buffer.position(fileTableOffset)

            var totalSize: Long = 0

            for (i in 0u.toUShort()..<fileCount) {
                val nameSize = buffer.getUShort().toInt()
                val crc = buffer.getInt()
                val compressedSize = buffer.getInt()
                val originalSize = buffer.getInt()
                totalSize += originalSize.toLong()
                val fileOffset = buffer.getInt()
                val archiveSize = buffer.getShort()

                if (archiveDirectory == null) {
                    val archive = buffer.getBytes(archiveSize.toInt()).decodeToString()
                    archiveDirectory = output / archive
                    archiveDirectory!!.createDirectories()
                } else {
                    buffer.position(buffer.position() + archiveSize)
                }

                val fileName = buffer.getBytes(nameSize).decodeToString()
                val targetFile = archiveDirectory!! / fileName

                val element = IpfElement {
                    this.file = targetFile
                    this.name = fileName
                    this.crc = crc
                    this.compressedSize = compressedSize
                    this.originalSize = originalSize
                    this.fileOffset = fileOffset
                    this.archive = file.name
                    this.ipfFile = ipf
                }
                tasks += Callable { extractElement(element, progress) }
            }
            progress.updateTotal(totalSize)
        }
        //pool.awaitTermination(4, TimeUnit.MINUTES)
        pool.invokeAll(tasks)
        // sleep to allow the state to catch up
        Thread.sleep(300)
        progress.stop()
    }

    private fun debug(message: String) {
        echo(italic(gray(message)))
    }

    private fun extractElement(element: IpfElement, progress: ProgressAnimation) {
        try {
            FileChannel.open(element.ipfFile.archiveFile, READ).use { channel ->
                val buffer = channel.map(READ_ONLY, 0, channel.size())
                buffer.position(element.fileOffset)
                element.data = buffer.getBytes(element.compressedSize)
            }
        } catch (e: IOException) {
            System.out.printf("Failed read %s from archive %s\r\n", element.file.toString(), element.archive)
            e.printStackTrace()
            return
        }

        if (element.compressedSize != element.originalSize) {
            element.data = decompressElement(element, element.data)
        }

        try {
            element.file.createParentDirectories()
            element.file.writeBytes(element.data, CREATE, TRUNCATE_EXISTING)
            progress.advance(element.data.size.toLong())
        } catch (e: IOException) {
            System.out.printf("Failed write %s\r\n", element.file.toString())
            e.printStackTrace()
        }
    }

    private fun decompressElement(element: IpfElement, data: ByteArray): ByteArray {
        val inflater = Inflater(true)
        val version = element.ipfFile.version
        val input = when {
            version >= 11_035 || version == 0 -> PkwareInputStream(IpfFile.PASSWORD, ByteArrayInputStream(data))
            else -> ByteArrayInputStream(data)
        }
        return InflaterInputStream(input, inflater).use { stream ->
            val output = ByteArrayOutputStream()
            do {
                output.write(stream.read())
            } while (stream.available() > 0)
            output.toByteArray()
        }
    }
}