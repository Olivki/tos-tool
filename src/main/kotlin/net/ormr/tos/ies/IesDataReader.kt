package net.ormr.tos.ies

import net.ormr.tos.ies.IesUtil.getStringWithSize
import net.ormr.tos.ies.IesUtil.shiftBits
import java.nio.ByteBuffer

sealed interface IesDataReader {
    fun read(buffer: ByteBuffer): Any

    data object INT : IesDataReader {
        override fun read(buffer: ByteBuffer): Any = buffer.getInt()
    }

    data object STRING : IesDataReader {
        override fun read(buffer: ByteBuffer): Any = buffer.getStringWithSize().shiftBits()
    }
}