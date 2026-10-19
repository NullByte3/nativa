package club.nullbyte3.nativa.struct;

/**
 * One field of a {@link Struct}, from {@link Struct#field}. Kept in a {@code static final}, the JIT
 * folds {@code offset} and {@code stride} into constants, which is what makes struct access as fast
 * as a hand-written offset; it is a record because the JIT trusts record fields to be final.
 *
 * @param stride bytes per element, the struct's {@link Struct#byteSize()}
 */
public record StructField(Struct struct, String name, Class<?> type, long offset, long stride) {}
