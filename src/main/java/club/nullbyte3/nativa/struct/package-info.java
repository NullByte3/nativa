/**
 * Structs off the heap: a {@link club.nullbyte3.nativa.struct.Struct} lays out a record of
 * primitives, and {@link club.nullbyte3.nativa.struct.StructArray} or {@link
 * club.nullbyte3.nativa.struct.StructRing} stores elements of it. Fields are read and written one
 * at a time, so no record is ever created.
 */
package club.nullbyte3.nativa.struct;
