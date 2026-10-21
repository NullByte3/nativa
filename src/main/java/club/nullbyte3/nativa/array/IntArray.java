package club.nullbyte3.nativa.array;

import club.nullbyte3.nativa.Memory;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/** A fixed-length {@code int} array, zeroed when allocated. */
public final class IntArray extends Memory {

  private static final ValueLayout.OfInt LAYOUT = ValueLayout.JAVA_INT;
  private static final VarHandle ATOMIC = LAYOUT.varHandle();
  private final long length;

  private IntArray(long length) {
    super(Math.multiplyExact(length, Integer.BYTES), Integer.BYTES);
    this.length = length;
  }

  public static IntArray allocate(long length) {
    return new IntArray(length);
  }

  /** A copy of {@code values}. */
  public static IntArray of(int... values) {
    IntArray array = new IntArray(values.length);
    array.copyFrom(values);
    return array;
  }

  public long length() {
    return length;
  }

  public int get(long index) {
    return segment.getAtIndex(LAYOUT, index);
  }

  public void set(long index, int value) {
    segment.setAtIndex(LAYOUT, index, value);
  }

  /**
   * Add {@code delta} and return the new value. A read then a write: not atomic, see {@code
   * getAndAdd}.
   */
  public int add(long index, int delta) {
    int value = get(index) + delta;
    set(index, value);
    return value;
  }

  public void fill(int value) {
    for (long i = 0; i < length; i++) segment.setAtIndex(LAYOUT, i, value);
  }

  /** Copy {@code src} to the start of this array. */
  public void copyFrom(int[] src) {
    MemorySegment.copy(src, 0, segment, LAYOUT, 0, src.length);
  }

  public int[] toArray() {
    return segment.toArray(LAYOUT);
  }

  /** The latest write to this element from any thread. */
  public int getVolatile(long index) {
    return (int) ATOMIC.getVolatile(segment, offset(index));
  }

  /** Write an element every thread sees at once. */
  public void setVolatile(long index, int value) {
    ATOMIC.setVolatile(segment, offset(index), value);
  }

  /**
   * Atomically set the element to {@code update} if it holds {@code expected}; false if it did not.
   */
  public boolean compareAndSet(long index, int expected, int update) {
    return ATOMIC.compareAndSet(segment, offset(index), expected, update);
  }

  /** Atomically add {@code delta} and return what the element held before. */
  public int getAndAdd(long index, int delta) {
    return (int) ATOMIC.getAndAdd(segment, offset(index), delta);
  }

  /** Atomically set the element and return what it held before. */
  public int getAndSet(long index, int value) {
    return (int) ATOMIC.getAndSet(segment, offset(index), value);
  }

  // Checked before multiplying, so a huge index cannot wrap around into range.
  private long offset(long index) {
    return Objects.checkIndex(index, length) * Integer.BYTES;
  }
}
