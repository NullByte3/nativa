package club.nullbyte3.nativa.struct;

import java.lang.foreign.Arena;
import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The C layout of a record of primitives: fields in declaration order, each aligned to its size,
 * the whole padded to the largest. The record is only the schema and is never instantiated.
 */
public final class Struct {

  private final String name;
  private final Map<String, StructField> fields = new HashMap<>();
  private final long byteSize;
  private final long alignment;

  private Struct(Class<? extends Record> type) {
    this.name = type.getSimpleName();
    RecordComponent[] components = type.getRecordComponents();
    if (components.length == 0) throw new IllegalArgumentException(name + " has no fields");
    // Offsets first: every field carries the stride, known only once the last one is placed.
    long[] offsets = new long[components.length];
    long offset = 0;
    long maxAlign = 1;
    for (int i = 0; i < components.length; i++) {
      long size = sizeOf(components[i].getType());
      if (size == 0) {
        throw new IllegalArgumentException(
            name + "." + components[i].getName() + " is not a primitive");
      }
      offsets[i] = offset = align(offset, size);
      offset += size;
      maxAlign = Math.max(maxAlign, size);
    }
    this.byteSize = align(offset, maxAlign);
    this.alignment = maxAlign;
    for (int i = 0; i < components.length; i++) {
      String field = components[i].getName();
      fields.put(
          field, new StructField(this, field, components[i].getType(), offsets[i], byteSize));
    }
  }

  /** The layout of {@code type}; throws if a component is not a primitive. */
  public static Struct of(Class<? extends Record> type) {
    return new Struct(type);
  }

  /** The field called {@code name}; throws if there is none. Look it up once, not per access. */
  public StructField field(String name) {
    StructField field = fields.get(name);
    if (field == null) throw new IllegalArgumentException(this.name + " has no field " + name);
    return field;
  }

  /** A fixed array of {@code length} zeroed elements. */
  public StructArray array(long length) {
    return new StructArray(null, this, length);
  }

  /** A fixed array of {@code length} zeroed elements in {@code arena}, which frees it. */
  public StructArray array(Arena arena, long length) {
    return new StructArray(Objects.requireNonNull(arena), this, length);
  }

  /** A ring keeping the last {@code capacity} elements pushed. */
  public StructRing ring(long capacity) {
    return new StructRing(null, this, capacity);
  }

  /** A ring keeping the last {@code capacity} elements, in {@code arena}, which frees it. */
  public StructRing ring(Arena arena, long capacity) {
    return new StructRing(Objects.requireNonNull(arena), this, capacity);
  }

  /** Bytes per element, padding included. */
  public long byteSize() {
    return byteSize;
  }

  long alignment() {
    return alignment;
  }

  @Override
  public String toString() {
    return "Struct[" + name + ", " + byteSize + " bytes]";
  }

  /** Bytes for a primitive type, 0 for anything else. A boolean takes a byte. */
  private static long sizeOf(Class<?> type) {
    if (type == byte.class || type == boolean.class) return 1;
    if (type == short.class || type == char.class) return 2;
    if (type == int.class || type == float.class) return 4;
    if (type == long.class || type == double.class) return 8;
    return 0;
  }

  /** {@code offset} rounded up to a multiple of {@code size}, a power of two. */
  private static long align(long offset, long size) {
    return (offset + size - 1) & -size;
  }
}
