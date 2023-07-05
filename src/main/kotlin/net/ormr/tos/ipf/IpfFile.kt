package net.ormr.tos.ipf

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import kotlin.io.path.div

/**
 * @author zcxv
 * @date 06.06.2019
 */
class IpfFile {
    var version: Int = 0
    var subversion: Int = 0
    lateinit var archiveFile: Path

    fun save(directory: Path) {
        Files.newByteChannel(directory / "version", CREATE, TRUNCATE_EXISTING, WRITE).use { channel ->
            val buffer = ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(version)
            buffer.putInt(subversion)
            channel.write(buffer)
        }
    }

    fun restore(directory: File) {
        Files.newByteChannel(File(directory, "version").toPath(), READ).use { channel ->
            val buffer = ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN)
            channel.read(buffer)
            version = buffer.getInt()
            subversion = buffer.getInt()
        }
    }

    fun restore(directory: Path) {
        Files.newByteChannel(directory / "version", READ).use { channel ->
            val buffer = ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN)
            channel.read(buffer)
            version = buffer.getInt()
            subversion = buffer.getInt()
        }
    }

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is IpfFile -> false
        version != other.version -> false
        subversion != other.subversion -> false
        else -> archiveFile == other.archiveFile
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + subversion
        result = 31 * result + archiveFile.hashCode()
        return result
    }

    override fun toString(): String = "IpfFile(version=$version, subversion=$subversion, archiveFile=$archiveFile)"

    companion object {
        // Open BETA:
        //  0x00, 0x00, 0x00, 0x00 subversion
        //  0x01, 0x09, 0x00, 0x00 version
        // Some new files:
        //  0x00, 0x00, 0x00, 0x00 subversion
        //  0x1b, 0x2b, 0x00, 0x00 version
        @JvmField
        val PASSWORD = byteArrayOf(
            0x6F, 0x66, 0x4F, 0x31, 0x61, 0x30, 0x75, 0x65, 0x58, 0x41,
            0x3F, 0x20, 0x5B, 0xFF.toByte(), 0x73, 0x20, 0x68, 0x20, 0x25, 0x3F
        )

        inline operator fun invoke(body: IpfFile.() -> Unit): IpfFile = IpfFile().apply(body)
    }
}