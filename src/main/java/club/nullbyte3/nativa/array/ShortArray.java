package club.nullbyte3.nativa.array;

import club.nullbyte3.nativa.Memory;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/** A fixed-length {@code short} array, zeroed when allocated. */
public final class ShortArray extends Memory {

  private static final ValueLayout.OfShort LAYOUT = ValueLayout.JAVA_SHORT;
  private static final VarHandle ATOMIC = LAYOUT.varHandle();
  private final long length;

  private ShortArray(Arena arena, long length) {
    super(arena, Math.multiplyExact(length, Short.BYTES), Short.BYTES);
    this.length = length;
  }

  public static ShortArray allocate(long length) {
    return new ShortArray(null, length);
  }

  /** Allocate in {@code arena}, which frees it when it closes; see {@link Memory#close()}. */
  public static ShortArray allocate(Arena arena, long length) {
    return new ShortArray(Objects.requireNonNull(arena), length);
  }

  /** A copy of {@code values}. */
  public static ShortArray of(short... values) {
    ShortArray array = new ShortArray(null, values.length);
    array.copyFrom(values);
    return array;
  }

  public long length() {
    return length;
  }

  public short get(long index) {
    return segment.getAtIndex(LAYOUT, index);
  }

  public void set(long index, short value) {
    segment.setAtIndex(LAYOUT, index, value);
  }

  /** Add {@code delta} and return the new value, wrapping on overflow. Not atomic. */
  public short add(long index, short delta) {
    short value = (short) (get(index) + delta);
    set(index, value);
    return value;
  }

  public void fill(short value) {
    for (long i = 0; i < length; i++) segment.setAtIndex(LAYOUT, i, value);
  }

  /** Copy {@code src} to the start of this array. */
  public void copyFrom(short[] src) {
    MemorySegment.copy(src, 0, segment, LAYOUT, 0, src.length);
  }

  public short[] toArray() {
    return segment.toArray(LAYOUT);
  }

  /** The latest write to this element from any thread. */
  public short getVolatile(long index) {
    return (short) ATOMIC.getVolatile(segment, offset(index));
  }

  /** Write an element every thread sees at once. */
  public void setVolatile(long index, short value) {
    ATOMIC.setVolatile(segment, offset(index), value);
  }

  /**
   * Atomically set the element to {@code update} if it holds {@code expected}; false if it did not.
   */
  public boolean compareAndSet(long index, short expected, short update) {
    return compareAndSetSmall(offset(index), Short.BYTES, expected, update);
  }

  /** Atomically set the element and return what it held before. */
  public short getAndSet(long index, short value) {
    return (short) getAndSetSmall(offset(index), Short.BYTES, value);
  }

  /** Atomically add {@code delta} and return what the element held before. Wraps on overflow. */
  public short getAndAdd(long index, short delta) {
    return (short) getAndAddSmall(offset(index), Short.BYTES, delta);
  }

  /** Set the elements from {@code from} up to, not including, {@code to}. */
  public void fill(long from, long to, short value) {
    Objects.checkFromToIndex(from, to, length);
    for (long i = from; i < to; i++) segment.setAtIndex(LAYOUT, i, value);
  }

  /** Copy {@code length} elements of {@code src}, from {@code srcPos}, to {@code index}. */
  public void copyFrom(short[] src, int srcPos, long index, int length) {
    Objects.checkFromIndexSize(srcPos, length, src.length);
    Objects.checkFromIndexSize(index, length, this.length);
    MemorySegment.copy(src, srcPos, segment, LAYOUT, index * Short.BYTES, length);
  }

  /** Copy {@code length} elements from {@code index} to {@code dst}, from {@code dstPos}. */
  public void copyTo(long index, short[] dst, int dstPos, int length) {
    Objects.checkFromIndexSize(index, length, this.length);
    Objects.checkFromIndexSize(dstPos, length, dst.length);
    MemorySegment.copy(segment, LAYOUT, index * Short.BYTES, dst, dstPos, length);
  }

  /**
   * Copy {@code length} elements from {@code index} to {@code dstIndex} in {@code dst}, which may
   * be this array; the ranges may overlap.
   */
  public void copyTo(long index, ShortArray dst, long dstIndex, long length) {
    Objects.checkFromIndexSize(index, length, this.length);
    Objects.checkFromIndexSize(dstIndex, length, dst.length);
    MemorySegment.copy(
        segment, index * Short.BYTES, dst.segment, dstIndex * Short.BYTES, length * Short.BYTES);
  }

  /** A copy of {@code length} elements from {@code from}. */
  public short[] toArray(long from, int length) {
    short[] out = new short[length];
    copyTo(from, out, 0, length);
    return out;
  }

  // Checked before multiplying, so a huge index cannot wrap around into range.
  private long offset(long index) {
    return Objects.checkIndex(index, length) * Short.BYTES;
  }
}
