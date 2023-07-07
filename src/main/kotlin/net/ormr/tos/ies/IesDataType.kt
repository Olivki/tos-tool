package net.ormr.tos.ies

import net.ormr.tos.ies.IesUtil.getString
import net.ormr.tos.ies.IesUtil.getUShort
import net.ormr.tos.ies.IesUtil.putUString
import net.ormr.tos.ies.IesUtil.shiftBits
import java.nio.ByteBuffer
import kotlin.Int as KInt
import kotlin.String as KString

sealed interface IesDataType {
    val id: Short

    val name: KString

    fun read(buffer: ByteBuffer): Any

    fun write(value: Any, buffer: ByteBuffer)

    fun getElementSize(value: Any): KInt

    // TODO: unsure if this is actually ints or floats, because they're packed the same way
    data object Float32 : IesDataType {
        override val id: Short
            get() = 0

        override val name: KString
            get() = "float32"

        override fun read(buffer: ByteBuffer): Any = buffer.getFloat()

        override fun write(value: Any, buffer: ByteBuffer) {
            buffer.putFloat(value as Float)
        }

        override fun getElementSize(value: Any): KInt = Float.SIZE_BYTES
    }

    sealed class String : IesDataType {
        override fun read(buffer: ByteBuffer): Any = buffer.getString(size = buffer.getUShort().toInt()).shiftBits()

        override fun write(value: Any, buffer: ByteBuffer) {
            buffer.putUString((value as KString).shiftBits())
        }

        //override fun getElementSize(value: Any): KInt = 2 /* length part */ + (value as KString).toByteArray().size
        override fun getElementSize(value: Any): KInt = 2 + (value as KString).toByteArray(Charsets.UTF_8).size
    }

    data object String1 : String() {
        override val id: Short
            get() = 1

        override val name: kotlin.String
            get() = "string1"
    }

    // TODO: this might be representing like booleans? It seems to be 'FALSE', 'TRUE', 'NO' or 'YES'
    data object String2 : String() {
        override val id: Short
            get() = 2

        override val name: kotlin.String
            get() = "string2"
    }
}

private val koreanCharset = charset("x-windows-949")