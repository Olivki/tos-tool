package net.ormr.tos.ipf

class Crc32 {
    private var hash = -0x1
    val value: Int
        get() = hash.inv()

    fun calculate(crc: Int, b: Byte): Int = TABLE[crc xor b.toInt() and 0xFF] xor (crc ushr 8)

    fun update(b: Byte): Int {
        hash = TABLE[hash xor b.toInt() and 0xFF] xor (hash ushr 8)
        return value
    }

    @JvmOverloads
    fun update(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): Int {
        for (i in offset..<length) {
            hash = TABLE[hash xor bytes[i].toInt() and 0xFF] xor (hash ushr 8)
        }
        return value
    }

    fun reset() {
        hash = -0x1
    }

    companion object {
        /** CRC-32-IEEE 802.3  */
        private const val POLYNOM = -0x12477CE0
        private val TABLE = IntArray(256)

        init {
            for (i in TABLE.indices) {
                var value = i
                for (j in 0..7) {
                    value = (if (value and 1 == 1) value ushr 1 xor POLYNOM else value ushr 1)
                }
                TABLE[i] = value
            }
        }
    }
}
