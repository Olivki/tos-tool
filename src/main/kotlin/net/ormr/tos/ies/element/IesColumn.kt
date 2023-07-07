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
    lateinit var key: String
    lateinit var type: IesDataType
    var unk1: Short by Delegates.notNull()
    var unk2: Short by Delegates.notNull()
    var pos: Short by Delegates.notNull()

    override fun read(buffer: ByteBuffer) {
        name = buffer.getCString().shiftBits()
        buffer.position(buffer.position() + 64 - sizeOf(name) - 1)
        key = buffer.getCString().shiftBits()
        buffer.position(buffer.position() + 64 - sizeOf(key) - 1)
        val typeId = buffer.getShort()
        type = when (typeId) {
            0.toShort() -> IesDataType.Float32
            1.toShort() -> IesDataType.String1
            2.toShort() -> IesDataType.String2
            else -> error("Unknown data type: $typeId")
        }
        unk1 = buffer.getShort()
        unk2 = buffer.getShort()
        pos = buffer.getShort()
    }

    override fun write(buffer: ByteBuffer) {
        val nameBytes = name.shiftBits().toByteArray()
        buffer.put(nameBytes)
        buffer.position(buffer.position() + 64 - nameBytes.size)
        val name2Bytes = key.shiftBits().toByteArray()
        buffer.put(name2Bytes)
        buffer.position(buffer.position() + 64 - name2Bytes.size)
        buffer.putShort(type.id)
        buffer.putShort(unk1)
        buffer.putShort(unk2)
        buffer.putShort(pos)
    }

    override fun compareTo(other: IesColumn): Int = when {
        pos > other.pos -> 1
        pos < other.pos -> -1
        type.id > other.type.id -> 1
        type.id < other.type.id -> -1
        else -> 0
    }

    override fun toString(): String =
        "IesColumn(name='$name', key='$key', type=$type, unk1=$unk1, unk2=$unk2 pos=$pos)"
}