#pragma endian little

#include <std/mem.pat>
#include <std/sys.pat>

using XacHeader;
using XacInfo1;
using XacInfo2;
using XacInfo3;

struct BasicString {
    u32 length;
    char data[length];
};

enum ChunkType : u32 {
    NODE = 0,
    LIMIT = 6,
    INFO = 7,
    STDPROGMORPHTARGET = 9,
    NODEGROUPS = 10,
    NODES = 11,       // Actor_Nodes
    STDPMORPHTARGETS = 12,       // Actor_MorphTargets
    NODEMOTIONSOURCES = 14,       // Actor_NodeMotionSources
    ATTACHMENTNODES = 15,       // Actor_AttachmentNodes
    PHYSICSSETUP = 18,
    SIMULATEDOBJECTSETUP = 19,
    MESHASSET = 20,
    FORCE_32BIT = 0xFFFFFFFF,
};

enum EUnitType : u8 {
    INCHES,
    FEET,
    YARDS,
    MILES,
    MILLIMETERS,
    CENTIMETERS,
    DECIMETERS,
    METERS,
    KILOMETERS,
};

using FileChunk;

struct Xac {
    XacHeader header;
    padding[1]; // TODO: this might not be correct
    FileChunk chunks[1];
} [[inline]];

struct XacHeader {
    char magicWord[4];
    std::assert(magicWord == "XAC ", "Unknown magic word: " + magicWord);
    u8 hiVersion;
    u8 loVersion;
    u8 endianType;
} [[static]];

struct FileChunk {
    ChunkType id;
    u32 sizeInBytes;
    u32 version;
} [[static]];

struct XacInfo1 {
    u32 numLoDs;
    u32 trajectoryNodeIndex;
    u32 motionExtractionNodeIndex;
    float retargetRootOffset;
    EUnitType unitType;
    u8 exporterHighVersion;
    u8 exporerLowVersion;
};

struct XacInfo3 {
    u32 numLoDs;
    u32 motionExtractionNodeIndex;
    u32 retargetRootNodeIndex;
    EUnitType unitType;
    u8 exporterHighVersion;
    u8 exporerLowVersion;
    u8 optimizeSkeleton;
    //BasicString sourceApplication;
};

Xac xac @ 0x00;