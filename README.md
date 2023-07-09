Large rework of the original tos tool code.

This version has a fully working `ies` packer and unpacker, the old version could not unpack values with type id `2` *(
which a decent chunk of files use)*, and it also had faulty calculations for the sizes of rows *(Using `String.length`
for values where the actual byte count was needed, which is faulty for various reasons)*.

The old `ies` reader also just completely failed to read the row data into the correct column, so they were completely
wrongly mapped.