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
import com.github.ajalt.mordant.rendering.TextColors.blue
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextStyles.italic
import net.ormr.tos.ies.IesUtil.getUShort
import net.ormr.tos.ipf.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_ONLY
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import kotlin.io.path.*
import kotlin.time.measureTime

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
    private val isDirectory by lazy { input.isDirectory() }
    private val pool by lazy {
        Executors.newFixedThreadPool(threadsCount) as ThreadPoolExecutor
    }

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        echo("Using ${blue(threadsCount.toString())} threads")
        output.createDirectories()
        if (isDirectory) {
            val files = input.walk().filter { it.isRegularFile() }.toList()
            for ((i, file) in files.withIndex()) {
                echo("Extracting ${blue(file.relativeTo(input).toString())} (${i + 1}/${files.size})")
                unpack(file)
            }
        } else {
            echo("Extracting ${blue(input.relativeTo(input).toString())}")
            unpack(input)
        }
        pool.shutdown()
    }

    // TODO: there's probably more values that are being read as signed values that should be read as unsigned
    private fun unpack(file: Path) {
        val unpackDuration = measureTime {
            var archiveDirectory: Path? = null
            var ipf: IpfFile? = null
            var fileCount: UShort
            FileChannel.open(file, READ).use { channel ->
                val buffer = channel.map(READ_ONLY, 0, file.fileSize()).order(ByteOrder.LITTLE_ENDIAN)

                val header = buffer.limit() - IpfElement.getTail().size - 4
                buffer.position(header)

                val jmp = buffer.getInt()
                buffer.position(jmp)

                fileCount = buffer.getUShort()
                debug("${file.name}: $fileCount files")

                val fileTableOffset = buffer.getInt() // 6
                buffer.getShort() // 8
                buffer.position(fileTableOffset)

                for (i in 0u.toUShort()..<fileCount) {
                    val nameSize = buffer.getUShort().toInt()
                    val crc = buffer.getInt()
                    val compressedSize = buffer.getInt()
                    val originalSize = buffer.getInt()
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

                    if (ipf == null) {
                        val position = buffer.position()
                        buffer.position(buffer.limit() - 8)
                        ipf = IpfFile {
                            subversion = buffer.getInt()
                            version = buffer.getInt()
                            archiveFile = file
                        }
                        ipf!!.save(archiveDirectory!!)
                        buffer.position(position)
                    }

                    val element = IpfElement {
                        this.file = targetFile
                        this.name = fileName
                        this.crc = crc
                        this.compressedSize = compressedSize
                        this.originalSize = originalSize
                        this.fileOffset = fileOffset
                        this.archive = file.name
                        this.ipfFile = ipf!!
                    }
                    pool.execute { extractElement(element) }
                }
            }
            var csleep: Long = 0
            while (true) {
                if (pool.queue.isEmpty()) break
                if (csleep % 4000 == 0L) {
                    val extractedCount = fileCount.toInt() - pool.queue.size - pool.activeCount + 1
                    debug("${file.name}: Extracted $extractedCount/${fileCount.toInt()} files")
                }
                Thread.sleep(400)
                csleep += 400
            }
        }

        pool.purge()

        echo("Extracted ${blue(file.name)} in ${blue(unpackDuration.toPrettyString())}")
    }

    private fun debug(message: String) {
        echo(italic(gray(message)))
    }

    private fun extractElement(element: IpfElement) {
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
        } catch (e: IOException) {
            System.out.printf("Failed write %s\r\n", element.file.toString())
            e.printStackTrace()
        }
    }

    private fun decompressElement(element: IpfElement, data: ByteArray): ByteArray {
        val inflater = Inflater(true)
        val input = when {
            element.ipfFile.version >= 11_035 -> PkwareInputStream(IpfFile.PASSWORD, ByteArrayInputStream(data))
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