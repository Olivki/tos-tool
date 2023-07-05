package net.ormr.tos.ies.element

import net.ormr.tos.ies.IesDataReader
import net.ormr.tos.ies.IesDataType
import java.nio.ByteBuffer

class IesData(val column: IesColumn) : IesElement {
    lateinit var data: Any
    var flag: Byte = 0

    override val elementSize: Int
        get() = column.type.getElementSize(data)

    val isIntType: Boolean
        get() = column.type is IesDataType.Int32

    val isStringType: Boolean
        get() = column.type is IesDataType.String

    override fun read(buffer: ByteBuffer) {
        data = column.type.read(buffer)
    }

    fun read(reader: IesDataReader, buffer: ByteBuffer) {
        data = reader.read(buffer)
    }

    override fun write(buffer: ByteBuffer) {
        column.type.write(data, buffer)
    }

    override fun toString(): String = "IesData(column=$column, data=$data, flag=$flag)"
}