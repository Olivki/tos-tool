package net.ormr.tos.ies.element

import net.ormr.tos.ies.IesDataType
import net.ormr.tos.ies.IesUtil.getCString
import net.ormr.tos.ies.IesUtil.shiftBits
import net.ormr.tos.ies.IesUtil.sizeOf
import java.nio.ByteBuffer
import kotlin.properties.Delegates

// TODO: sort by type & pos ?
class IesColumn(private val table: IesTable) : IesElement, Comparable<IesColumn> {
    override val elementSize: Int
        get() = 64 + 64 + 2 + 2 + 2 + 2

    lateinit var name: String
    lateinit var name2: String
    lateinit var type: IesDataType
    var dummy: Int by Delegates.notNull()
    var pos: Short by Delegates.notNull()

    override fun read(buffer: ByteBuffer) {
        name = buffer.getCString().shiftBits()
        buffer.position(buffer.position() + 64 - sizeOf(name) - 1)
        name2 = buffer.getCString().shiftBits()
        buffer.position(buffer.position() + 64 - sizeOf(name2) - 1)
        val typeId = buffer.getShort()
        type = when (typeId) {
            0.toShort() -> IesDataType.Int32
            1.toShort() -> IesDataType.String1
            2.toShort() -> IesDataType.String2
            else -> error("Unknown data type: $typeId")
        }
        dummy = buffer.getInt()
        pos = buffer.getShort()
    }

    override fun write(buffer: ByteBuffer) {
        val nameBytes = name.shiftBits().toByteArray()
        buffer.put(nameBytes)
        buffer.position(buffer.position() + 64 - nameBytes.size)
        val name2Bytes = name2.shiftBits().toByteArray()
        buffer.put(name2Bytes)
        buffer.position(buffer.position() + 64 - name2Bytes.size)
        buffer.putShort(type.id)
        buffer.putInt(dummy)
        buffer.putShort(pos)
    }

    override fun toString(): String =
        "IesColumn(table=$table, name=$name, name2=$name2, type=$type, dummy=$dummy, pos=$pos)"

    override fun compareTo(other: IesColumn): Int = when {
        pos > other.pos -> 1
        pos < other.pos -> -1
        type.id > other.type.id -> 1
        type.id < other.type.id -> -1
        else -> 0
    }
}