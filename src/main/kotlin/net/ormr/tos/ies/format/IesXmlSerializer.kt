package net.ormr.tos.ies.format

import net.ormr.tos.ies.*
import net.ormr.tos.ies.element.*
import net.ormr.tos.utils.createDocument
import net.ormr.tos.utils.element
import net.ormr.tos.utils.loadDocument
import net.ormr.tos.utils.writeTo
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.xpath.XPathHelper.getAbsolutePath
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*

// TODO: manually serialize and deserialize this instead of using XStream,
//       because XStream probably doesn't play nicely with Kotlin
data object IesXmlSerializer : IesSerializer {
    @Throws(Throwable::class)
    override fun encodeToFile(file: Path, table: IesTable) {
        val doc = createDocument(table)
        doc.writeTo(file)
    }

    private fun createDocument(table: IesTable): Document = createDocument("table") {
        setAttribute("name", table.header.name)
        setAttribute("flag", table.header.flag.toString())
        setAttribute("flag2", table.header.flag2.toString())
        setAttribute("unkColumns", table.header.unkColumns.toString())
        setAttribute("rows", table.header.rowCount.toString())
        element("columns") {
            for (column in table.columns) {
                element("column") {
                    setAttribute("name", column.name)
                    setAttribute("name2", column.name2)
                    setAttribute("type", column.type.name.lowercase())
                    setAttribute("dummy", column.dummy.toString())
                    setAttribute("pos", column.pos.toString())
                    /*setAttribute("unk1", column.unk1.toString())
                    setAttribute("unk2", column.unk2.toString())
                    setAttribute("unk3", column.unk3.toString())*/
                }
            }
        }
        element("rows") {
            for (row in table.rows) {
                element("row") {
                    setAttribute("id", row.id.toString())
                    setAttribute("key", row.key)
                    for (data in row.data) {
                        if (data == null) {
                            element("null-column") {}
                        } else {
                            element("data") {
                                val column = data.column
                                setAttribute("ref", column.name2)
                                setAttribute("value", data.data.toString())
                                //setAttribute("type", column.type.name.lowercase())
                                setAttribute("flag", data.flag.toString())
                            }
                        }
                    }
                }
            }
        }
    }

    override fun decodeFromFile(file: Path): IesTable {
        val document = loadDocument(file)
        val root = document.rootElement
        val table = IesTable()
        val header = IesHeader(table).apply {
            name = root.attr("name")
            flag = root.intAttr("flag")
            flag2 = root.shortAttr("flag2")
            unkColumns = root.shortAttr("unkColumns")
        }
        table.header = header

        val mappedColumns = hashMapOf<String, IesColumn>()
        val columns = mutableListOf<IesColumn>()
        for (columnElement in root.child("columns").getChildren("column")) {
            val column = IesColumn(table).apply {
                name = columnElement.attr("name")
                name2 = columnElement.attr("name2")
                type = when (val typeName = columnElement.attr("type")) {
                    "int32" -> IesDataType.Int32
                    "string1" -> IesDataType.String1
                    "string2" -> IesDataType.String2
                    else -> error("Unknown column type $typeName at ${getAbsolutePath(columnElement)}")
                }
                dummy = columnElement.intAttr("dummy")
                pos = columnElement.shortAttr("pos")
            }
            if (column.name in mappedColumns) {
                error("Duplicate column name ${column.name} at ${getAbsolutePath(columnElement)}")
            }
            mappedColumns[column.name2] = column
            columns += column
        }

        val rows = mutableListOf<IesRow>()
        for (rowElement in root.child("rows").getChildren("row")) {
            val row = IesRow(table).apply {
                id = rowElement.intAttr("id")
                key = rowElement.attr("key")
                val dataEntries = mutableListOf<IesData?>()
                for (dataElement in rowElement.getChildren("data")) {
                    val ref = dataElement.attr("ref")
                    val column = mappedColumns[ref] ?: error("Unknown column $ref at ${getAbsolutePath(dataElement)}")
                    val data = IesData(column).apply {
                        flag = dataElement.byteAttr("flag")
                        val rawData = dataElement.attr("value")
                        data = when (column.type) {
                            IesDataType.Int32 -> rawData.toInt()
                            IesDataType.String1, IesDataType.String2 -> rawData
                        }
                    }
                    dataEntries += data
                }
                data = dataEntries.toTypedArray()
            }
            rows += row
        }

        //println("${rows.size} : ${columns.size}")
        table.rows = rows.toTypedArray()
        table.columns = columns.toTypedArray()
        header.apply {
            columnSize = columns.sumOf { it.elementSize }
            rowSize = rows.sumOf { it.elementSize }
            fileSize = columnSize + rowSize
            rowCount = rows.size.toShort()
            columnCount = columns.size.toShort()
            intColumns = columns.count { it.type is IesDataType.Int32 }.toShort()
            stringColumns = columns.count { it.type is IesDataType.String }.toShort()
        }
        //println(header)
        header.update()
        return table
    }

    private fun Element.attr(name: String): String =
        getAttributeValue(name) ?: error("Attribute $name not found at ${getAbsolutePath(this)}")

    private fun Element.byteAttr(name: String): Byte = try {
        attr(name).toByte()
    } catch (e: NumberFormatException) {
        error("Attribute $name is not a byte at ${getAbsolutePath(this)}")
    }

    private fun Element.shortAttr(name: String): Short = try {
        attr(name).toShort()
    } catch (e: NumberFormatException) {
        error("Attribute $name is not a short at ${getAbsolutePath(this)}")
    }

    private fun Element.intAttr(name: String): Int = try {
        attr(name).toInt()
    } catch (e: NumberFormatException) {
        error("Attribute $name is not an integer at ${getAbsolutePath(this)}")
    }

    private fun Element.child(name: String): Element =
        getChild(name) ?: error("Child $name not found at ${getAbsolutePath(this)}")
}
