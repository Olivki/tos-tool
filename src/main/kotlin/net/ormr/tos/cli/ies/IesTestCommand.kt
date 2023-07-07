package net.ormr.tos.cli.ies

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.rendering.TextColors.blue
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
                if (path.isRegularFile()) useFile(path)
            }
        } else {
            useFile(input)
        }

        echo("Unique flag values: ${flagValues.joinToString(", ")}")
    }

    private val flagValues = hashSetOf<Any>()

    private fun useFile(input: Path) {
        //dumpUniqueValuesForColumns(input)
        //checkValueFlag(input)
        //dumpUniqueValuesForColumns(input)
        for (row in readIesTable(input).rows) {
            for (data in row.data) {
                if (data == null) continue
                flagValues += data.flag
            }
        }
    }

    private fun checkValueFlag(input: Path) {
        val table = readIesTable(input)
        val rowsWithFlags = hashMapOf<String, List<String>>()
        for (row in table.rows) {
            val flags = mutableListOf<String>()
            for (data in row.data) {
                if (data == null) continue
                if (data.flag != 0.toByte()) {
                    flags += "${data.column.key}: ${data.flag}"
                }
            }
            if (flags.isNotEmpty()) {
                rowsWithFlags[row.key] = flags
            }
        }
        if (rowsWithFlags.isNotEmpty()) {
            echo("${input.name} (${table.header.name}):")
            for ((key, flags) in rowsWithFlags) {
                echo("  ${blue(key)} { ${flags.joinToString(", ")} }")
            }
            echo()
        }
    }

    private fun dumpUniqueValuesForColumns(input: Path) {
        val table = readIesTable(input)
        checkForDecimals(table)
        //checkValues(table, "RSPTIME")
        //checkValues(table, "LootingChance")
        // checkValues(table, "CT_DropStyle")
        //checkValues(table, "CT_repairPrice_NT")
    }

    private fun checkForDecimals(table: IesTable) {
        for (row in table.rows) {
            for (data in row.data) {
                if (data == null) continue
                if (data.data is Float) {
                    val float = data.data as Float
                    if (float % 1 != 0f) {
                        echo("Found decimal: ${data.data} in ${data.column.key} at ${row.key}")
                    }
                }
            }
        }
    }

    private fun checkValues(table: IesTable, key: String) {
        echo("Checking $key")
        val foundValues = hashSetOf<Any>()
        for (row in table.rows) {
            for (data in row.data) {
                if (data == null) continue
                if (data.column.key != key) continue
                foundValues += data.data
            }
        }
        echo("Found values: $foundValues")
        //echo("Found values: ${foundValues.map { Float.fromBits(it as Int) }}")
    }

    private fun checkDuplicates(file: Path) {
        val table = readIesTable(file)
        val names = hashMapOf<String, Int>()
        for ((i, column) in table.columns.withIndex()) {
            val location = names[column.key]
            if (location != null) {
                echo("Duplicate column name: ${column.key} in ${file.name} at $i (original at $location) ${table.header.name}")
            } else {
                names[column.key] = i
            }
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
        //echo("Reading ${input.fileName}...")
        val buffer = channel.map(READ_ONLY, 0, input.fileSize()).order(ByteOrder.LITTLE_ENDIAN)
        val table = IesTable()
        table.read(buffer)
        table
    }
}