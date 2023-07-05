package net.ormr.tos.ies.format

import net.ormr.tos.ies.element.IesTable
import java.nio.file.Path

/**
 * @author PointerRage
 */
sealed interface IesSerializer {
    @Throws(Throwable::class)
    fun encodeToFile(file: Path, table: IesTable)

    @Throws(Throwable::class)
    fun decodeFromFile(file: Path): IesTable
}
