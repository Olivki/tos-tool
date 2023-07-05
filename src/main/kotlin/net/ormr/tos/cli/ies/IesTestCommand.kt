package net.ormr.tos.cli.ies

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import net.ormr.tos.ies.element.IesTable
import net.ormr.tos.ies.format.IesXmlSerializer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_ONLY
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import kotlin.io.path.*

class IesTestCommand : CliktCommand(name = "test") {
    private val input by argument()
        .path(mustExist = true, mustBeReadable = true, canBeDir = true, canBeFile = true, canBeSymlink = false)

    private val isDirectory by lazy { input.isDirectory() }

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        if (isDirectory) {
            for (path in input.walk()) {
                if (path.isRegularFile()) doThing(path)
            }
        } else {
            doThing(input)
        }
    }

    private fun doThing(file: Path) {
        val binaryTable = readIesTable(file)
        val xmlTable = createXmlTable(binaryTable)
        val binarySizes = sizePair(binaryTable)
        val xmlSizes = sizePair(xmlTable)
        if (binarySizes != xmlSizes) {
            echo("Binary and XML table sizes do not match: $binarySizes != $xmlSizes")
            echo("${countPair(binaryTable)} vs ${countPair(xmlTable)}")
            echo(gamer(binaryTable))
            echo(gamer(xmlTable))
        }
    }

    private fun gamer(table: IesTable) = table
        .rows
        .flatMap { it.data.asIterable() }
        .filterNotNull()
        .map { it.data }

    private fun sizePair(table: IesTable) = table.header.rowSize to table.header.columnSize

    private fun countPair(table: IesTable) = table.rows.size to table.columns.size

    private fun createXmlTable(binaryTable: IesTable): IesTable {
        val tempXml = createTempFile(prefix = "ies-", suffix = ".xml")
        IesXmlSerializer.encodeToFile(tempXml, binaryTable)
        return IesXmlSerializer.decodeFromFile(tempXml)
    }

    private fun readIesTable(input: Path): IesTable = FileChannel.open(input, READ).use { channel ->
        echo("Reading ${input.fileName}...")
        val buffer = channel.map(READ_ONLY, 0, input.fileSize()).order(ByteOrder.LITTLE_ENDIAN)
        val table = IesTable()
        table.read(buffer)
        table
    }
}