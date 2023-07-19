#pragma endian little
#include <std/string.pat>

fn xor_string(str content) {
    str newContent;
    for (u32 i = 0, i < std::string::length(content), i = i + 1) {
        newContent = newContent + char(std::string::at(content, i) ^ 1);
    }
    return newContent;
};

// TODO: just get rid of the null termination in the struct instead of doing it here
fn format_null_terminated_string(str value) {
    if (std::string::length(value) == 0) {
        return "";
    }
    str newValue;
    u32 count = 0;
    char currentChar;
    while (true) {
        currentChar = std::string::at(value, count);
        if (currentChar == 0) break;
        newValue = newValue + currentChar;
        count = count + 1;
    }
    return newValue;
};

fn format_null_terminated_xor_string(str value) {
    return xor_string(format_null_terminated_string(value));
};

fn format_2_byte_bool(u16 value) {
    return value != 0;
};

using IesString;

fn format_ies_string(IesString string) {
    return xor_string(string.content);
};

struct IesHeader {
    char id[64] [[format("format_null_terminated_string")]];
    char keyID[64] [[format("format_null_terminated_string")]];
    u32 version;
    u32 columnSize;
    u32 classSize;
    u32 totalSize;
    u16 useClassID [[format("format_2_byte_bool")]]; // bool
    u16 classCount;
    u16 columnCount;
    u16 numberColumnsCount;
    u16 stringColumnsCount;
    padding[2];
} [[static]];

enum IesType : u16 {
    Number,
    String,
    Computed
};

enum IesKind : u16 {
    EP,
    CP,
    VP,
    Normal,
    CT,
};

struct IesString {
    u16 length;
    char content[length];
} [[format("format_ies_string")]];

struct IesColumn {
    char stringKey[64] [[format("format_null_terminated_xor_string")]];
    char name[64] [[format("format_null_terminated_xor_string")]];
    IesType type;
    IesKind kind;
    u16 isNT [[format("format_2_byte_bool")]]; // 2 byte wide bool
    u16 index;
} [[static]];

struct IesClass {
    u32 classID;
    IesString className;
    float numbers[parent.header.numberColumnsCount];
    IesString strings[parent.header.stringColumnsCount];
    bool usesScriptFunctions[parent.header.stringColumnsCount];
};

struct Ies {
    IesHeader header;
    IesColumn columns[header.columnCount];
    IesClass classes[header.classCount];
} [[inline]];

Ies ies @ 0x00;