package net.ormr.tos.ies.element

import net.ormr.tos.ies.IesDataReader
import net.ormr.tos.ies.IesDataReader.FLOAT
import net.ormr.tos.ies.IesDataReader.STRING
import net.ormr.tos.ies.IesDataType
import net.ormr.tos.ies.IesUtil.getStringWithSize
import net.ormr.tos.ies.IesUtil.putStringWithSize
import net.ormr.tos.ies.IesUtil.shiftBits
import net.ormr.tos.ies.IesUtil.sizeOf
import java.nio.ByteBuffer
import kotlin.properties.Delegates

class IesRow(private val table: IesTable) : IesElement {
    var id: Int by Delegates.notNull()
    lateinit var key: String
    lateinit var data: Array<IesData?>

    override val elementSize: Int
        get() {
            val dataSize = data.sumOf { it!!.elementSize }
            return 4 + 2 + sizeOf(key) + dataSize + table.getTypeDataCount<IesDataType.String>()
        }

    override fun read(buffer: ByteBuffer) {
        id = buffer.getInt()
        key = buffer.getStringWithSize().shiftBits()
        data = arrayOfNulls(table.header.columnCount.toInt())
        readData<IesDataType.Float32>(FLOAT, table.header.intColumns.toInt(), buffer)
        readData<IesDataType.String>(STRING, table.header.stringColumns.toInt(), buffer)
        for (iesData in data) {
            // the code is a bit ambiguous about whether 'data' should actually contain nulls or not
            // because here it just doesn't care, but in the 'write' section it *does* check for nulls?
            requireNotNull(iesData)
            if (!iesData.isStringType) continue
            iesData.flag = buffer.get()
        }
    }

    private inline fun <reified T : IesDataType> readData(reader: IesDataReader, max: Int, buffer: ByteBuffer) {
        var i = 0
        var column = 0
        while (i < max) {
            for (j in data.indices) {
                if (data[j] == null && table.columns[j].type is T) {
                    column = j
                    break
                }
            }
            val iesData = IesData(table.columns[column])
            data[column] = iesData
            iesData.read(reader, buffer)
            i++
        }
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putInt(id)
        buffer.putStringWithSize(key.shiftBits())
        writeData<IesDataType.Float32>(buffer)
        writeData<IesDataType.String>(buffer)
        for (iesData in data) {
            requireNotNull(iesData)
            if (!iesData.isStringType) continue
            buffer.put(iesData.flag)
        }
    }

    private inline fun <reified T : IesDataType> writeData(buffer: ByteBuffer) {
        val data = data.copyOf(data.size)
        for (i in data.indices) {
            for (j in data.indices) {
                val iesData = data[j]
                if (iesData != null && iesData.column.type is T) {
                    iesData.write(buffer)
                    data[j] = null
                    break
                }
            }
        }
    }

    override fun toString(): String = "IesRow(table=$table, id=$id, key='$key', data=${data.contentDeepToString()})"
}