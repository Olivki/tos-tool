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

package net.ormr.tos.cli.ies.format

import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPack
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.ormr.krautils.compress.Compression
import net.ormr.krautils.compress.compress
import net.ormr.krautils.compress.decompress
import net.ormr.tos.cli.ies.IesOutputParent
import net.ormr.tos.cli.ies.serializer.IesCsvSerializer
import net.ormr.tos.cli.ies.serializer.IesSerializer
import net.ormr.tos.ies.element.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.QuoteMode
import java.nio.file.Path
import kotlin.io.path.*

class IesCsvFormat(private val parent: IesOutputParent) : IesFormat("csv") {
    private val formatFile by option()
        .path(mustExist = true, mustBeReadable = true, canBeDir = false, canBeFile = true, canBeSymlink = false)
        .defaultLazy {
            parent.output.resolve(".csv_format")
        }
    private val columnName by option("--column-name")
        .enum<ColumnName>()
        .default(ColumnName.KEY)

    private val formatModel by lazy { getOrCreateFormatModel() }
    private val json = Json {
        encodeDefaults = true
    }

    // TODO: remove this
    override val serializer: IesSerializer
        get() = IesCsvSerializer

    @Suppress("UNCHECKED_CAST")
    override fun load(file: Path): IesTable {
        val extraData = loadExtraData(file)
        val format = createCSVFormat(formatModel, extraData.headers)
            .builder()
            .setSkipHeaderRecord(true)
            .build()
        val table = IesTable(
            header = IesHeader(
                name = extraData.name,
                flag1 = extraData.flag1,
                flag2 = extraData.flag2,
                unknown = extraData.unknown,
            ),
            columns = extraData.columns.mapTo(mutableListOf()) {
                IesColumn(
                    name = it.name,
                    key = it.key,
                    type = getTypeFromId(it.type),
                    position = it.pos,
                    unk1 = it.unk1,
                    unk2 = it.unk2,
                )
            },
        )
        val sortedColumns = table.columns.sortedWith(IesColumn.BINARY_COMPARATOR)
        CSVParser(file.bufferedReader(), format).use { parser ->
            for (record in parser) {
                val id = record["Row ID"].toInt()
                val key = record["Row Key"]
                val values: MutableList<IesValue<*, *>> = record
                    .drop(2)
                    .mapIndexedTo(mutableListOf()) { i, value ->
                        val column = sortedColumns[i]
                        when (sortedColumns[i].type) {
                            IesType.Float32 -> IesFloat32Value(value.toFloat(), column as IesColumn<Float>)
                            IesType.String1 -> IesString1Value(value, column as IesColumn<String>)
                            IesType.String2 -> IesString2Value(value, column as IesColumn<String>)
                        }
                    }
                table.rows.add(IesRow(id = id, key = key, values = values))
            }
        }
        return table
    }

    override fun save(table: IesTable, file: Path) {
        val names = listOf("Row ID", "Row Key") + table
            .columns
            .sortedWith(IesColumn.BINARY_COMPARATOR)
            .map(::getColumName)
        val format = createCSVFormat(formatModel, names)
        CSVPrinter(file.bufferedWriter(), format).use { printer ->
            for (rows in table.rows) {
                val values = listOf(rows.id.toString(), rows.key) + rows.values.map {
                    when (it) {
                        is IesFloat32Value -> if (it.data % 1 != 0F) it.data.toString() else it.data.toInt().toString()
                        is IesStringValue -> it.data
                    }
                }
                printer.printRecord(values)
            }
        }
        saveExtraData(file, table)
    }

    private fun getTypeFromId(id: Short): IesType<*> = when (id) {
        0.toShort() -> IesType.Float32
        1.toShort() -> IesType.String1
        2.toShort() -> IesType.String2
        else -> error("Unknown type id: $id")
    }

    private fun getColumName(column: IesColumn<*>): String = when (columnName) {
        ColumnName.NAME -> column.name
        ColumnName.KEY -> column.key
    }

    private fun saveExtraData(csvFile: Path, table: IesTable) {
        val dataModel = ExtraData(
            name = table.name,
            flag1 = table.header.flag1,
            flag2 = table.header.flag2,
            unknown = table.header.unknown,
            headers = listOf("Row ID", "Row Key") + table.columns.map(::getColumName),
            columns = table.columns.map {
                ExtraData.ColumnData(
                    name = it.name,
                    key = it.key,
                    type = it.type.id,
                    unk1 = it.unk1,
                    unk2 = it.unk2,
                    pos = it.position,
                )
            },
        )
        val file = csvFile.resolveSibling("${csvFile.fileName}.data")
        val bytes = MsgPack.encodeToByteArray(ExtraData.serializer(), dataModel)
        file.writeBytes(bytes.compress(Compression.Gzip))
    }

    private fun loadExtraData(csvFile: Path): ExtraData {
        val file = csvFile.resolveSibling("${csvFile.fileName}.data")
        if (file.notExists()) {
            error("Could not find extra data file for CSV file: $csvFile")
        }
        val bytes = file.readBytes().decompress(Compression.Gzip)
        return MsgPack.decodeFromByteArray(ExtraData.serializer(), bytes)
    }

    private fun getOrCreateFormatModel(): CsvFormatModel = if (formatFile.exists()) {
        json.decodeFromString(CsvFormatModel.serializer(), formatFile.readText())
    } else {
        val model = CsvFormatModel()
        formatFile.writeText(json.encodeToString(CsvFormatModel.serializer(), model))
        model
    }

    private fun createCSVFormat(model: CsvFormatModel, names: List<String>?): CSVFormat = CSVFormat
        .Builder
        .create()
        .apply {
            if (names != null) setHeader(*names.toTypedArray())
            setSkipHeaderRecord(false)
            setDelimiter(model.delimiter)
            setQuote(model.quote)
            setRecordSeparator(model.recordSeparator)
            setEscape(model.escape)
            setQuoteMode(model.quoteMode)
            setTrailingDelimiter(model.trailingDelimiter)
            setTrim(model.trim)
        }
        .build()

    @Serializable
    private data class CsvFormatModel(
        val delimiter: String = ",",
        val quote: Char = '"',
        val recordSeparator: String = "\r\n",
        val escape: Char? = '\\',
        val quoteMode: QuoteMode? = null,
        val trailingDelimiter: Boolean = false,
        val trim: Boolean = false,
    )

    @Serializable
    private data class ExtraData(
        val name: String,
        val flag1: Int,
        val flag2: Short,
        val unknown: Short,
        val headers: List<String>,
        val columns: List<ColumnData>,
    ) {
        @Serializable
        data class ColumnData(
            val name: String,
            val key: String,
            val type: Short,
            val unk1: Short,
            val unk2: Short,
            val pos: Short,
        )
    }

    private enum class ColumnName {
        NAME,
        KEY,
    }
}