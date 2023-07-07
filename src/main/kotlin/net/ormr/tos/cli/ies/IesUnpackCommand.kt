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
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_ONLY
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import kotlin.io.path.*

class IesUnpackCommand : CliktCommand(name = "unpack") {
    private val input by argument()
        .help("Input file or directory")
        .path(mustExist = true, mustBeReadable = true, canBeDir = true, canBeFile = true, canBeSymlink = false)
    private val output by option("-o", "--output")
        .help("Output directory")
        .path()
        .defaultLazy { Path("./unpacked_ies/") }
    private val format: IesSerializer by option("-f", "--format")
        .choice("xml" to IesXmlSerializer)
        .required()

    private val isDirectory by lazy { input.isDirectory() }

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        output.createDirectories()
        input.walk().forEach(::writeFile)
    }

    private fun writeFile(input: Path) {
        val table = readIesTable(input)
        format.encodeToFile(output / "${input.nameWithoutExtension}.xml", table)
    }

    private fun readIesTable(input: Path): IesTable = FileChannel.open(input, READ).use { channel ->
        echo("Reading ${input.fileName}...")
        val buffer = channel.map(READ_ONLY, 0, input.fileSize()).order(ByteOrder.LITTLE_ENDIAN)
        val table = IesTable()
        table.read(buffer)
        table
    }
}