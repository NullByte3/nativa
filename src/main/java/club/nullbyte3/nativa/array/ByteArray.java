package club.nullbyte3.nativa.array;

import club.nullbyte3.nativa.Memory;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/** A fixed-length {@code byte} array, zeroed when allocated. */
public final class ByteArray extends Memory {

  private static final ValueLayout.OfByte LAYOUT = ValueLayout.JAVA_BYTE;
  private final long length;

  private ByteArray(long length) {
    super(Math.multiplyExact(length, Byte.BYTES), Byte.BYTES);
    this.length = length;
  }

  public static ByteArray allocate(long length) {
    return new ByteArray(length);
  }

  /** A copy of {@code values}. */
  public static ByteArray of(byte... values) {
    ByteArray array = new ByteArray(values.length);
    array.copyFrom(values);
    return array;
  }

  public long length() {
    return length;
  }

  public byte get(long index) {
    return segment.getAtIndex(LAYOUT, index);
  }

  public void set(long index, byte value) {
    segment.setAtIndex(LAYOUT, index, value);
  }

  /** Add {@code delta} and return the new value. Wraps on overflow. */
  public byte add(long index, byte delta) {
    byte value = (byte) (get(index) + delta);
    set(index, value);
    return value;
  }

  public void fill(byte value) {
    segment.fill(value);
  }

  /** Copy {@code src} to the start of this array. */
  public void copyFrom(byte[] src) {
    MemorySegment.copy(src, 0, segment, LAYOUT, 0, src.length);
  }

  public byte[] toArray() {
    return segment.toArray(LAYOUT);
  }
}
