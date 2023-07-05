package net.ormr.tos.ies.element

import java.nio.ByteBuffer

/**
 * @author PointerRage
 */
sealed interface IesElement {
    val elementSize: Int

    fun read(buffer: ByteBuffer)

    fun write(buffer: ByteBuffer)
}
