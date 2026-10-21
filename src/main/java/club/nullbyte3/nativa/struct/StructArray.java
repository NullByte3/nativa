package club.nullbyte3.nativa.struct;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

/**
 * A fixed array of struct elements, indexed {@code 0} to {@code length() - 1}. From {@link
 * Struct#array}.
 */
public final class StructArray extends StructMemory {

  private final long length;
  private final long stride;

  StructArray(Arena arena, Struct struct, long length) {
    super(arena, struct, length);
    this.length = length;
    this.stride = struct.byteSize();
  }

  public long length() {
    return length;
  }

  /** Copy element {@code from} over element {@code to}. */
  public void copy(long from, long to) {
    MemorySegment.copy(segment, slot(from) * stride, segment, slot(to) * stride, stride);
  }

  @Override
  long count() {
    return length;
  }

  @Override
  long slot(long index) {
    return Objects.checkIndex(index, length);
  }
}
