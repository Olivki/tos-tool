/*
 * Copyright 2023 Oliver Berg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ormr.tos.cli.ipf

import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPack
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

@Serializable
data class IpfData(val name: String, val subversion: UInt, val version: UInt)

fun loadIpfDataFrom(directory: Path): IpfData = MsgPack.decodeFromByteArray(
    IpfData.serializer(),
    (directory / ".ipf_data").readBytes(),
)

fun saveIpfDataTo(directory: Path, data: IpfData) {
    (directory / ".ipf_data").writeBytes(MsgPack.encodeToByteArray(IpfData.serializer(), data))
}