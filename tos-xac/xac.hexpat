#pragma endian little
#pragma array_limit 66666665536
#pragma pattern_limit 1331072

#include <std/mem.pat>
#include <std/sys.pat>

using XacChunk;

struct Xac {
    char magicWord[4];
    std::assert(magicWord == "XAC ", "Unknown magic word: " + magicWord);
    u8 unknown[4];
    XacChunk chunks[while(!std::mem::eof())];
} [[inline]];

using VertexBlock;

struct XacObjectChunk {
    u32 unknown1;
    u32 unknown5Count;
    u32 vertexCount;
    u32 faceIndexCount;
    u32 unknown2;
    u32 vertexBlockCount;
    u32 unknown3;
    //padding[vertexCount * 4];
    VertexBlock blocks[vertexBlockCount];
    /*u32 faceIndexCount2;
    u32 vertexCount2;
    u32 unknown4;
    u32 unknown5;
    u32 faceVertexIndices[faceIndexCount2];*/
};

struct Point {
    float x;
    float y;
    float z;
} [[static, inline]];

struct UV {
    float u;
    float v; // maybe has to be inverted
} [[static, inline]];

struct VertexBlock {
    u32 identifier;
    u32 entrySize;
    u16 unknown1;
    u16 unknown2;
    //u8 data[parent.vertexCount];

    match (identifier) {
        // XYZ
        (0): Point coordinates[parent.vertexCount];
        // Normals
        (1): Point normals[parent.vertexCount];
        // Texture Coordinates
        (3): UV coordinates[parent.vertexCount];
        (5): u32 unknown5Index[parent.vertexCount];
    }
};

struct XacChunk {
    u32 identifier;
    u32 length;
    u32 version;

    match (identifier) {
        (1): XacObjectChunk objectChunks[1];
        (_): u8 data[length];
    }
};

//Xac xac @ 0x00;

XacChunk blockChunk @ 0x0000835E;
