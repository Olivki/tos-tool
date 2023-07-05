package net.ormr.tos.ies

import java.nio.ByteBuffer

object IesUtil {
    @JvmStatic
    fun ByteBuffer.getCString(): String = buildString {
        var b: Byte
        while (get().also { b = it }.toInt() != 0x00) append(Char(b.toUShort()))
    }

    @JvmStatic
    fun ByteBuffer.getString(size: Int): String {
        val bytes = ByteArray(size)
        get(bytes)
        return String(bytes)
    }

    @JvmStatic
    fun ByteBuffer.getStringWithSize(): String = getString(size = getUShort().toInt())

    @JvmStatic
    fun ByteBuffer.putString(value: String, size: Short) {
        putShort(size)
        val bytes = value.toByteArray()
        put(bytes)
    }

    @JvmStatic
    fun ByteBuffer.putUString(value: String) {
        val bytes = value.toByteArray()
        putShort(bytes.size.toUShort().toShort())
        put(bytes)
    }

    @JvmStatic
    fun ByteBuffer.putStringWithSize(value: String) {
        val bytes = value.toByteArray()
        putShort(bytes.size.toShort())
        put(bytes)
    }

    @JvmStatic
    fun sizeOf(value: String): Int = value.toByteArray().size

    @JvmStatic
    fun ByteBuffer.getUShort(): UShort = getShort().toUShort()

    @JvmStatic
    fun ByteBuffer.putUShort(value: UShort) {
        putShort(value.toShort())
    }

    @JvmStatic
    fun String.shiftBits(): String {
        val bytes = toByteArray()
        for (i in bytes.indices) bytes[i] = (bytes[i].toInt() xor 0x1).toByte()
        return String(bytes)
    }

    fun Int.shiftBits(): Int {
        /*
		_put(a + 3, int3(x));
        _put(a + 2, int2(x));
        _put(a + 1, int1(x));
        _put(a    , int0(x));
        */
        val bytes = byteArrayOf(int0(this), int1(this), int2(this), int3(this))
        for (i in bytes.indices) bytes[i] = (bytes[i].toInt() xor 0x1).toByte()
        return makeInt(bytes[3], bytes[2], bytes[1], bytes[0])
    }

    private fun int3(x: Int): Byte = (x shr 24).toByte()

    private fun int2(x: Int): Byte = (x shr 16).toByte()

    private fun int1(x: Int): Byte = (x shr 8).toByte()

    private fun int0(x: Int): Byte = x.toByte()

    private fun makeInt(b3: Byte, b2: Byte, b1: Byte, b0: Byte): Int = b3.toInt() shl 24 or
            (b2.toInt() and 0xFF shl 16) or
            (b1.toInt() and 0xFF shl 8) or
            (b0.toInt() and 0xFF)
}
