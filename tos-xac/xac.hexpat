#pragma endian little

#include <std/mem.pat>
#include <std/sys.pat>
#include <std/string.pat>

using XacHeader;

// utility structs

struct BasicString {
    s32 length;
    char data[length];
} [[format("format_basic_string")]];

fn format_basic_string(BasicString string) {
    return string.data;
};

struct UV {
    float u;
    float v;
} [[static]];

struct Quaternion {
    float x;
    float y;
    float z;
    float w;
} [[static]];

struct Vec3d {
    float x;
    float y;
    float z;
} [[static]];

struct Matrix44 {
    Quaternion axes[3];
    Quaternion position;
} [[static]];

struct Color {
    float r;
    float g;
    float b;
    float a;
} [[static]];

enum ChunkType : s32 {
    MESH = 1,
    BONE_INFLUENCES = 2,
    MATERIAL_DEFINITION = 3,
    PROPERTIES = 5,
    METADATA = 7,
    NODE_HIERARCHY = 11,
    MORPH_TARGETS = 12,
    MATERIAL_TOTALS = 13,
    MAX_CHUNK,
};

// mappings

using XacChunk;

struct Xac {
    XacHeader header;
    XacChunk chunks[6]; // not '6' but rather read until EOF, 6 is just a placeholder
} [[inline]];

struct XacHeader {
    char magicWord[4];
    std::assert(magicWord == "XAC ", "Unknown magic word: " + magicWord);
    u8 hiVersion;
    u8 loVersion;
    bool isBigEndian;
    u8 multiplyOrder;
} [[static]];

struct XacChunkHeader {
    ChunkType type;
    s32 length;
    s32 version;
} [[static]];

struct XacMetadataHeader { // v2
    u32 repositionMask;
    s32 repositioningNode;
    u8 exporerMajorVersion;
    u8 exporterMinorVersion;
    padding[2];
    float retargetRootOffset;
} [[static]];

struct XacMetadata {
    XacMetadataHeader header;
    BasicString sourceApp;
    BasicString originalFileName;
    BasicString exportDate;
    BasicString actorName;
};

using XacNode;

struct XacNodeHierarchy { // v1
    s32 numNodes;
    s32 numRootNodes;
    XacNode nodes[numNodes];
};

struct XacNode { // v1
    Quaternion rotation;
    Quaternion scaleRotation;
    Vec3d position;
    Vec3d scale;
    padding[3 * 4];
    s32 unknownIndex1;
    s32 unknownIndex2;
    s32 parentNodeId;
    s32 numChildren;
    s32 includeInBoundsCalc;
    Matrix44 transform;
    float importanceFactor;
    BasicString name;
};

struct XacMaterialTotals { // v1
    s32 numTotalMaterials;
    s32 numStandardMaterials;
    s32 numFxMaterials;
} [[static]];

struct XacMaterialDefinitionHeader { // v2
    Color ambientColor;
    Color diffuseColor;
    Color specularColor;
    Color emissiveColor;
    float shine;
    float shineStrength;
    float opacity;
    float ior;
    bool isDoubleSided;
    bool isWireframe;
    padding[1];
    u8 numLayers;
} [[static]];

using XacMaterialDefinitionLayer;

struct XacMaterialDefinition { // v2
    XacMaterialDefinitionHeader header;
    BasicString name;
    XacMaterialDefinitionLayer layers[header.numLayers];
};

struct XacMaterialDefinitionLayer {
    float amount;
    UV offset;
    UV tiling;
    float rotation; // radians
    s16 materialId;
    u8 mapType;
    padding[1];
    BasicString texture;
};

struct XacProperty<T> {
    T value;
    BasicString name;
} [[format("format_property")]];

fn format_property(XacProperty<auto> property) {
    return property.name.data + " = " + std::string::to_string(property.value);
};

// because string properties have the order flipped around for some reason
struct XacStringProperty {
    BasicString name;
    BasicString value;
} [[format("format_string_property")]];

fn format_string_property(XacStringProperty property) {
    return property.name.data + " = " + std::string::to_string(property.value);
};

struct XacProperties {
    s32 intCount;
    s32 floatCount;
    s32 maybePadding1; // TODO: padding[4]
    s32 boolCount;
    s32 maybePadding2; // TODO: padding[4]
    s32 stringCount;
    BasicString name;
    BasicString fxFileName;
    BasicString shaderName;
    XacProperty<s32> intProperties[intCount];
    XacProperty<float> floatProperties[floatCount];
    XacProperty<bool> boolProperties[boolCount];
    XacStringProperty stringProperties[stringCount];
};

struct XacChunk {
    XacChunkHeader header;

    match (header.type) {
        (ChunkType::METADATA): XacMetadata data;
        (ChunkType::NODE_HIERARCHY): XacNodeHierarchy data;
        (ChunkType::MATERIAL_TOTALS): XacMaterialTotals data;
        (ChunkType::MATERIAL_DEFINITION): XacMaterialDefinition data;
        (ChunkType::PROPERTIES): XacProperties data;
    }
};

Xac xac @ 0x00;