#pragma endian little

struct IesHeader {
    char id[64];
    char keyid[64];
    u32 version;
    u32 infoSize; // columnSize
    u32 dataSize; // rowSize
    u32 totalSize; // fileSize
    u16 useClassID [[format("format_2_byte_bool")]]; // bool
    u16 numField; // rowCount
    u16 numColumn; // columnCount
    u16 numColumnNumber; // intColumns
    u16 numColumnString; // stringColumns
    padding[2];
} [[static]];

enum IesType : u16 {
    Number,
    String,
    Computed
};

enum IesAccess : u16 {
    EP,
    CP,
    Normal,
    CT,
};

struct IesColumn {
    char name[64];
    char key[64];
    IesType type;
    IesAccess access;
    u16 isStatic [[format("format_2_byte_bool")]]; // 2 byte wide bool
    u16 declIdx;
} [[static]];

fn format_2_byte_bool(u16 value) {
    return value != 0;
};

using IesString;

struct IesField {
    u32 classID;
    u16 classNameLength;
    char className[classNameLength];
    float numbers[parent.header.numColumnNumber];
    IesString strings[parent.header.numColumnString];
    bool useScrFuncs[parent.header.numColumnString];
};

struct IesString {
    u16 length;
    char content[length];
} [[format_read("format_ies_string")]];

fn format_ies_string(IesString string) {
    return string.content;
};

struct Ies {
    IesHeader header;
    IesColumn columns[header.numColumn];
    IesField fields[header.numField];
} [[inline]];

Ies ies @ 0x00;