## tos-tools

CLI tool and libraries for working with the various file format that ToS uses, currently supports `ies` and `ipf` files,
with some mappings done for the `xac` model format.

This tool has not been tested with the live version of the game, and is not intended to be used with it, therefore no
support will be provided for it.

Note that support for this tool is *not* provided by IMC Games, and is *not* officially supported by them.

**Requires Java 17 or newer to run.**

If unsure of where to easily download Java 17, you can use the Microsoft build of OpenJDK 17, which can be
downloaded [here](https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-17).

### Usage

Download the latest version from releases and extract it to a safe location, add the `bin` directory to your `PATH` *(or
the Linux equivalent)*, and you can then invoke the CLI environment by invoking `tos` in your preferred terminal.

All commands have basic `-h` / `--help` support, which will print out the usage of the command and the available
options. For example `tos ies unpack -h` will print out all available arguments/options for unpacking an `ies` file.

The `ies` and `ipf` commands are written to be sufficiently smart, and will try to prompt the user for input if the
given file structure is somehow wrong, but they are *not* fool-proof, so errors can and will happen.

Note that all commands have *more* options than what's shown below, the arguments shown below are just what you need to
know to quickly get started. For all arguments, like explained above, use the `-h` / `--help` option on the command.

#### ies

Handles the `ies` file format, which is a proprietary format used by ToS for storing game data.

IES files are essentially a binary representation of a dictionary, with the mapping `string -> f32`
and `string -> string`, these files are then used by the Lua scripts that run the client/server as pseudo class
structures.

Only 3 data types are supported by the `ies` format:

- Number `0x0`
-
    - A 32-bit floating point number.
- Localized String `0x1`
-
    - A string that can be localized by the client/server, the localized variant is stored in separate files.
- Calculated String `0x2`
-
    - A string whose content is "calculated"/static, and is not privy to the localization mechanics.

If a string values key starts with `CP_` then its type is `Calculated String` otherwise it's a `Localized String`.

##### unpack

`tos ies unpack <input> [output]`

Unpacks an `ies` file(s) into a directory determined by the `output` argument. If `output` is not provided, the `output`
directory will be created in the same directory as the `input` file, with the name `unpacked_ies`.

If `input` is a single file, only a single `ies` file will be unpacked, if `input` is a directory, then *all* files in
the directory will be traversed, and all `ies` files found will be unpacked.

##### pack

`tos ies pack <input> [output]`

Packs an `ies` file(s) into a directory determined by the `output` argument. If `output` is not provided, the `output`
directory will be created in the same directory as the `input` file, with the name `packed_ies`.

If `input` is a single file, only a single `ies` file will be packed, if `input` is a directory, then *all* files in the
directory will be traversed, and all `xml` *(this actually depends on the `format` used, but currently only the `xml`
format
is supported)* files found will be packed.

##### xml format

The `xml` format used by `unpack` and `pack` tries to mimic that of the official one, which means that a lot of the data
is *inferred*.

Note that "optional" columns are *not* supported, that means that if *one* entry has, say column, `CoolLevel`, but no
other entry
in the same xml file has that column, the conversion *will fail*. To fix this, either remove `CoolLevel` from the entry
that has it, or add `CoolLevel` to *every* entry in the xml file.

First the `key` and `value` is checked to determine an initial type, where it's resolved as follows:

- Does the `key` start with `CP_`?
    - If yes, then the type is `Calculated String`
    - If no, then we go to the next step
- Does `value` start with `-` or only contain the characters `[' ', '.', '0'..'9']`?
    - If yes, then the type is `Number`
    - If no, then the type is `Localized String`

If a `value` that was previously inferred to be a `Number` is then inferred to be a `Localized String` then the type is
changed to `Localized String`. This is because the intial type is just a guess, and can be wrong.

The `kind` of a `column` is determined by the prefix of its `key`:

- `EP_` -> `EP`
- `CP_` -> `CP`
- `VP_` -> `VP`
- `CT_` -> `CT`
- Anything else -> `NORMAL`

Whether a `column` is an `NT` column is determined by if its `key` contains `_NT`.

Whether a `field` is a script field is determined by if its `value` contains `SCR_` or `SCP`.

#### ipf

Handles the `ipf` file format, which is a proprietary format used by ToS for storing game data and assets.

IPF files are essentially just a proprietary archive format, with a custom footer, and encrypted file contents.

Both `unpack` and `pack` commands will attempt to use multiple cores by default, the amount of cores used can be changed
with the `-t`/`--threads` option. By default, it's half of the available cores.

To create custom `ipf` files, use the `pack` command on the directory containing the files you want to pack. To
understand the structure of the `ipf` file, it's recommended to first `unpack` an ipf file and mimic that.

##### unpack

`tos ipf unpack <input> [output]`

Unpacks an `ipf` file(s) into a directory determined by the `output` argument. If `output` is not provided, the `output`
directory will be created in the same directory as the `input` file, with the name `unpacked_ipf`.

If `input` is a single file, only a single `ipf` file will be unpacked, if `input` is a directory, then *all* files in
the directory will be traversed, and all `ipf` files found will be unpacked.

Unpacked `ipf` files will contain a `.ipf_data` file, which contains data required by the `pack` command to repack
the `ipf` file properly.

##### pack

`tos ipf pack <input> [output]`

Packs the `input` file/directory into an `ipf` file(s), the `output` argument determines the name of the `ipf` file,
if `input` contains multiple `ipf` directories, then `output` must be a directory, otherwise it must be a file.

If no `.ipf_data` file is located in the `input` then the user will be prompted to enter the required data, and a
new `.ipf_data` file will be created in `input` containing the provided data.

If the structure of the `input` directory is ambiguous then the user will be prompted for further information. Or in the
case that the structure is not resolvable *at all*, an error will be printed and the program will exit.