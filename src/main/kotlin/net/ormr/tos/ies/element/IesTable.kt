package net.ormr.tos.ies.element

import net.ormr.tos.ies.IesDataType
import java.nio.ByteBuffer

class IesTable : IesElement {
    lateinit var header: IesHeader
    lateinit var columns: Array<IesColumn>
    val columnsSize: Int
        get() = columns.sumOf { it.elementSize }
    lateinit var rows: Array<IesRow>
    val rowsSize: Int
        get() = rows.sumOf { it.elementSize }
    override val elementSize: Int
        get() = header.elementSize + columnsSize + rowsSize

    override fun read(buffer: ByteBuffer) {
        header = IesHeader(this)
        header.read(buffer)

        buffer.position(header.columnsOffset)
        columns = Array(header.columnCount.toInt()) { IesColumn(this).also { it.read(buffer) } }

        buffer.position(header.rowsOffset)
        rows = Array(header.rowCount.toInt()) { IesRow(this).also { it.read(buffer) } }
    }

    override fun write(buffer: ByteBuffer) {
        header.write(buffer)
        for (column in columns) column.write(buffer)
        for (row in rows) row.write(buffer)
    }

    inline fun <reified T : IesDataType> getTypeDataCount(): Int = columns.count { it.type is T }
}