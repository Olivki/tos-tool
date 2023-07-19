#pragma endian little

#include <std/mem.pat>

struct IpfData {
    u16 fileCount;
    u32 fileTableOffset;
    u16 unk1;
    u32 unk2;
    u32 compression;
    u32 subversion;
    u32 version;
} [[static]];

struct IpfFile {
    u16 pathLength;
    u32 crc;
    u32 compressedSize;
    u32 uncompressedSize;
    u32 fileOffset;
    u16 archiveNameLength;
    char archiveName[archiveNameLength];
    char path[pathLength];
};

struct Ipf {
    IpfData data;
    IpfFile files[data.fileCount] @ data.fileTableOffset;
} [[inline]];

Ipf ipf @ (std::mem::size() - 0x18);