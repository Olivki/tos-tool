package net.ormr.tos.ies.element

import net.ormr.tos.ies.IesDataType
import net.ormr.tos.ies.IesUtil.getCString
import net.ormr.tos.ies.IesUtil.sizeOf
import java.nio.ByteBuffer
import kotlin.properties.Delegates

class IesHeader(private val table: IesTable) : IesElement {
    lateinit var name: String
    var flag: Int by Delegates.notNull()
    var columnSize: Int by Delegates.notNull()
    var rowSize: Int by Delegates.notNull()
    var fileSize: Int by Delegates.notNull()
    var flag2: Short by Delegates.notNull()
    var rowCount: Short by Delegates.notNull()
    var columnCount: Short by Delegates.notNull()
    var intColumns: Short by Delegates.notNull()
    var stringColumns: Short by Delegates.notNull()
    var unkColumns: Short by Delegates.notNull()

    override val elementSize: Int
        get() = 128 + 4 + 4 + 4 + 4 + 2 + 2 + 2 + 2 + 2 + 2

    val columnsOffset: Int
        get() = fileSize - (columnSize + rowSize)

    val rowsOffset: Int
        get() = fileSize - rowSize

    override fun read(buffer: ByteBuffer) {
        name = buffer.getCString() //name, 128 bytes (with null-bytes)
        buffer.position(buffer.position() + 128 - sizeOf(name) - 1) //skip null-bytes
        flag = buffer.getInt()
        columnSize = buffer.getInt()
        rowSize = buffer.getInt()
        fileSize = buffer.getInt()
        flag2 = buffer.getShort()
        rowCount = buffer.getShort()
        columnCount = buffer.getShort()
        intColumns = buffer.getShort()
        stringColumns = buffer.getShort()
        unkColumns = buffer.getShort()
    }

    override fun write(buffer: ByteBuffer) {
        update()

        val nameBytes = name.toByteArray()
        buffer.put(nameBytes)
        buffer.position(buffer.position() + 128 - nameBytes.size)
        buffer.putInt(flag)
        buffer.putInt(columnSize)
        buffer.putInt(rowSize)
        buffer.putInt(fileSize)
        buffer.putShort(flag2)
        buffer.putShort(rowCount)
        buffer.putShort(columnCount)
        buffer.putShort(intColumns)
        buffer.putShort(stringColumns)
        buffer.putShort(unkColumns)
    }

    fun update() {
        fileSize = table.elementSize
        columnSize = table.columnsSize
        rowSize = table.rowsSize
        columnCount = table.columns.size.toShort()
        rowCount = table.rows.size.toShort()
        intColumns = table.getTypeDataCount<IesDataType.Int32>().toShort()
        stringColumns = table.getTypeDataCount<IesDataType.String>().toShort()
    }


    override fun toString(): String =
        "IesHeader(table=$table, name='$name', flag=$flag, columnSize=$columnSize, rowSize=$rowSize, size=$fileSize, flag2=$flag2, rowCount=$rowCount, columnCount=$columnCount, intColumns=$intColumns, stringColumns=$stringColumns, unkColumns=$unkColumns)"
}