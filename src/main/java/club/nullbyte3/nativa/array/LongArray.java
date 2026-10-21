package club.nullbyte3.nativa.array;

import club.nullbyte3.nativa.Memory;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/** A fixed-length {@code long} array, zeroed when allocated. */
public final class LongArray extends Memory {

  private static final ValueLayout.OfLong LAYOUT = ValueLayout.JAVA_LONG;
  private static final VarHandle ATOMIC = LAYOUT.varHandle();
  private final long length;

  private LongArray(Arena arena, long length) {
    super(arena, Math.multiplyExact(length, Long.BYTES), Long.BYTES);
    this.length = length;
  }

  public static LongArray allocate(long length) {
    return new LongArray(null, length);
  }

  /** Allocate in {@code arena}, which frees it when it closes; see {@link Memory#close()}. */
  public static LongArray allocate(Arena arena, long length) {
    return new LongArray(Objects.requireNonNull(arena), length);
  }

  /** A copy of {@code values}. */
  public static LongArray of(long... values) {
    LongArray array = new LongArray(null, values.length);
    array.copyFrom(values);
    return array;
  }

  public long length() {
    return length;
  }

  public long get(long index) {
    return segment.getAtIndex(LAYOUT, index);
  }

  public void set(long index, long value) {
    segment.setAtIndex(LAYOUT, index, value);
  }

  /**
   * Add {@code delta} and return the new value. A read then a write: not atomic, see {@code
   * getAndAdd}.
   */
  public long add(long index, long delta) {
    long value = get(index) + delta;
    set(index, value);
    return value;
  }

  public void fill(long value) {
    for (long i = 0; i < length; i++) segment.setAtIndex(LAYOUT, i, value);
  }

  /** Copy {@code src} to the start of this array. */
  public void copyFrom(long[] src) {
    MemorySegment.copy(src, 0, segment, LAYOUT, 0, src.length);
  }

  public long[] toArray() {
    return segment.toArray(LAYOUT);
  }

  /** The latest write to this element from any thread. */
  public long getVolatile(long index) {
    return (long) ATOMIC.getVolatile(segment, offset(index));
  }

  /** Write an element every thread sees at once. */
  public void setVolatile(long index, long value) {
    ATOMIC.setVolatile(segment, offset(index), value);
  }

  /**
   * Atomically set the element to {@code update} if it holds {@code expected}; false if it did not.
   */
  public boolean compareAndSet(long index, long expected, long update) {
    return ATOMIC.compareAndSet(segment, offset(index), expected, update);
  }

  /** Atomically add {@code delta} and return what the element held before. */
  public long getAndAdd(long index, long delta) {
    return (long) ATOMIC.getAndAdd(segment, offset(index), delta);
  }

  /** Atomically set the element and return what it held before. */
  public long getAndSet(long index, long value) {
    return (long) ATOMIC.getAndSet(segment, offset(index), value);
  }

  /** Set the elements from {@code from} up to, not including, {@code to}. */
  public void fill(long from, long to, long value) {
    Objects.checkFromToIndex(from, to, length);
    for (long i = from; i < to; i++) segment.setAtIndex(LAYOUT, i, value);
  }

  /** Copy {@code length} elements of {@code src}, from {@code srcPos}, to {@code index}. */
  public void copyFrom(long[] src, int srcPos, long index, int length) {
    Objects.checkFromIndexSize(srcPos, length, src.length);
    Objects.checkFromIndexSize(index, length, this.length);
    MemorySegment.copy(src, srcPos, segment, LAYOUT, index * Long.BYTES, length);
  }

  /** Copy {@code length} elements from {@code index} to {@code dst}, from {@code dstPos}. */
  public void copyTo(long index, long[] dst, int dstPos, int length) {
    Objects.checkFromIndexSize(index, length, this.length);
    Objects.checkFromIndexSize(dstPos, length, dst.length);
    MemorySegment.copy(segment, LAYOUT, index * Long.BYTES, dst, dstPos, length);
  }

  /**
   * Copy {@code length} elements from {@code index} to {@code dstIndex} in {@code dst}, which may
   * be this array; the ranges may overlap.
   */
  public void copyTo(long index, LongArray dst, long dstIndex, long length) {
    Objects.checkFromIndexSize(index, length, this.length);
    Objects.checkFromIndexSize(dstIndex, length, dst.length);
    MemorySegment.copy(
        segment, index * Long.BYTES, dst.segment, dstIndex * Long.BYTES, length * Long.BYTES);
  }

  /** A copy of {@code length} elements from {@code from}. */
  public long[] toArray(long from, int length) {
    long[] out = new long[length];
    copyTo(from, out, 0, length);
    return out;
  }

  // Checked before multiplying, so a huge index cannot wrap around into range.
  private long offset(long index) {
    return Objects.checkIndex(index, length) * Long.BYTES;
  }
}
