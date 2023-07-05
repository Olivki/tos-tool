package net.ormr.tos.ipf

import java.nio.file.Path

class IpfElement {
    lateinit var ipfFile: IpfFile
    lateinit var file: Path
    lateinit var name: String
    lateinit var archive: String // 'extra' in zip
    var crc: Int = 0
    var compressedSize: Int = 0
    var originalSize: Int = 0
    var fileOffset: Int = 0
    lateinit var data: ByteArray

    companion object {
        private val TAIL = byteArrayOf(
            0x50, 0x4B, 0x05, 0x06,  // zip magic
            0x00, 0x00, 0x00, 0x00,  // see IpfFile
            0x00, 0x00, 0x00, 0x00   // see IpfFile
        )

        @JvmStatic
        fun getTail(): ByteArray = TAIL

        private val zipMagic = byteArrayOf(0x50, 0x4B, 0x05, 0x06)

        @JvmStatic
        fun getZipMagic(): ByteArray = zipMagic

        inline operator fun invoke(body: IpfElement.() -> Unit): IpfElement = IpfElement().apply(body)
    }
}