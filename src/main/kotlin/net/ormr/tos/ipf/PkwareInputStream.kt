package net.ormr.tos.ipf

import java.io.IOException
import java.io.InputStream

/**
 * @author zcxv
 * @date 06.06.2019
 */
class PkwareInputStream(password: ByteArray, private val stream: InputStream) : InputStream() {
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
        key[1] += key[0] and 0xff
        key[1] = key[1] * 0x08088405 + 1
        key[2] = crc32.calculate(key[2], (key[1] shr 24).toByte())
    }

    @Throws(IOException::class)
    override fun read(): Int {
        return try {
            if (offset % 2 != 0) {
                return stream.read()
            }
            var uByte = stream.read() and 0xFF
            val magicByte = key[2] and 0xFFFF or 0x02
            uByte = uByte xor (magicByte * (magicByte xor 1) shr 8 and 0xFF)
            updateKey(uByte.toByte())
            uByte
        } finally {
            offset++
        }
    }

    @Throws(IOException::class)
    override fun available(): Int = stream.available()

    @Throws(IOException::class)
    override fun close() {
        stream.close()
    }
}
