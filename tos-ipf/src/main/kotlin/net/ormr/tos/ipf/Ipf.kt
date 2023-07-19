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

package net.ormr.tos.ipf

import java.nio.ByteBuffer

data class Ipf(
    val buffer: ByteBuffer,
    val subversion: UInt = 0u,
    val version: UInt = 0u,
    val elements: List<IpfElement>,
)

data class IpfElement(
    val crc: Int,
    val compressedSize: Int,
    val uncompressedSize: Int,
    val fileOffset: Int,
    val archiveName: String,
    val path: String,
) {
    fun isCompressed(): Boolean = compressedSize != uncompressedSize
}