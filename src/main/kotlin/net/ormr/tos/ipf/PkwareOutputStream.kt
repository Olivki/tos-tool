package net.ormr.tos.ipf

import java.io.IOException
import java.io.OutputStream

/**
 * @author zcxv
 * @date 06.06.2019
 */
class PkwareOutputStream(password: ByteArray, private val stream: OutputStream) : OutputStream() {
    private val key = intArrayOf(0x12345678, 0x23456789, 0x34567890)
    private val crc32 = Crc32()
    private var offset = 0

    init {
        generateKey(password)
    }

    private fun generateKey(bytes: ByteArray) {
        for (aByte in bytes) {
            updateKey(aByte)
        }
    }

    private fun updateKey(data: Byte) {
        key[0] = crc32.calculate(key[0], data)
        key[1] += key[0] and 0xFF
        key[1] = key[1] * 0x08088405 + 1
        key[2] = crc32.calculate(key[2], (key[1] shr 24).toByte())
    }

    @Throws(IOException::class)
    override fun write(uByte: Int) {
        try {
            if (offset % 2 != 0) {
                stream.write(uByte)
                return
            }
            val magicByte = key[2] and 0xFFFF or 0x02
            val encrypted = uByte xor (magicByte * (magicByte xor 1) shr 8 and 0xFF)
            updateKey(uByte.toByte())
            stream.write(encrypted)
        } finally {
            offset++
        }
    }

    @Throws(IOException::class)
    override fun flush() {
        stream.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        stream.close()
    }
}
