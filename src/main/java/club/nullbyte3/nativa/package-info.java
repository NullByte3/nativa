/**
 * Off-heap memory without boxing: {@link club.nullbyte3.nativa.RawMemory} for raw bytes, {@code
 * nativa.array} for one primitive per element, {@code nativa.struct} for named fields. Every block
 * is freed by {@code close()}, never by the GC, which also never scans it.
 */
package club.nullbyte3.nativa;
