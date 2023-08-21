### xac

The `xac` format is a proprietary format used by `EMotionFX`, modern implementations of it is provided by
the [O3DE](https://en.wikipedia.org/wiki/Open_3D_Engine) project, in the form of
the [actor component](https://docs.o3de.org/docs/user-guide/components/reference/animation/actor/).

Note that the `actor` format differs *slightly* from the `xac` format, mainly in that the magic word for `xac` is `xac `
*(space included at the end)*
and the magic word for `actor` is `actr`, and other slight differences. The O3DE `actor` format can be
found [here](https://github.com/o3de/o3de/blob/development/Gems/EMotionFX/Code/EMotionFX/Source/Importer/ActorFileFormat.h).

The `xac` format described by the `xac.hexpat` file is ***NOT*** a full mapping of the format, as it only contains
mappings for the `metadata (0x7)`, `node hierarchy (0xB)`, `material totals (0xD)`, `material definition (0x3)`
and `properties (0x5)`
node types as of writing this.

The most interesting node supported is the `properties` one, as that seems to be proprietary to ToS itself. It seems to
be multiple dictionaries that can contain the mappings `string -> s32`, `string -> f32`, `string -> boolean`
and `string -> string`, in that order. The dictionaries seem to provide properties for how the model should be rendered,
eg opacity, color, the texture to use, etc.

As of writing this, I have no plans of fully mapping the `xac` format, nor creating a tool for conversion of it, but I
leave the mappings I've created so far in case someone else wants to do so.