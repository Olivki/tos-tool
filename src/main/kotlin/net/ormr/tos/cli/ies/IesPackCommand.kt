package net.ormr.tos.cli.ies

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import net.ormr.tos.ies.element.IesTable
import net.ormr.tos.ies.format.IesSerializer
import net.ormr.tos.ies.format.IesXmlSerializer
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
        .defaultLazy { Path("./packed_ies/") }
    private val format: IesSerializer by option("-f", "--format")
        .choice("xml" to IesXmlSerializer)
        .required()

    private val isDirectory by lazy { input.isDirectory() }

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        output.createDirectories()
        if (isDirectory) {
            input.walk().forEach { if (it.isRegularFile()) writeFile(it) }
        } else {
            writeFile(input)
        }
    }

    private fun writeFile(input: Path) {
        echo("Reading ${input.fileName}...")
        val table = format.decodeFromFile(input)
        writeIesTable(table, output / "${table.header.name}.ies")
    }

    private fun writeIesTable(table: IesTable, output: Path) {
        FileChannel.open(output, WRITE, CREATE, TRUNCATE_EXISTING).use { channel ->
            val buffer = ByteBuffer.allocateDirect(table.elementSize + 1).order(ByteOrder.LITTLE_ENDIAN)
            table.write(buffer)
            buffer.flip()
            channel.write(buffer)
        }
    }
}