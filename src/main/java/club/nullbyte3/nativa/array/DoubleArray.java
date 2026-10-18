package club.nullbyte3.nativa.array;

import club.nullbyte3.nativa.Memory;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/** A fixed-length {@code double} array, zeroed when allocated. */
public final class DoubleArray extends Memory {

  private static final ValueLayout.OfDouble LAYOUT = ValueLayout.JAVA_DOUBLE;
  private static final VarHandle ATOMIC = LAYOUT.varHandle();
  private final long length;

  private DoubleArray(long length) {
    super(Math.multiplyExact(length, Double.BYTES), Double.BYTES);
    this.length = length;
  }

  public static DoubleArray allocate(long length) {
    return new DoubleArray(length);
  }

  /** A copy of {@code values}. */
  public static DoubleArray of(double... values) {
    DoubleArray array = new DoubleArray(values.length);
    array.copyFrom(values);
    return array;
  }

  public long length() {
    return length;
  }

  public double get(long index) {
    return segment.getAtIndex(LAYOUT, index);
  }

  public void set(long index, double value) {
    segment.setAtIndex(LAYOUT, index, value);
  }

  /** Add {@code delta} and return the new value. A read then a write, not atomic. */
  public double add(long index, double delta) {
    double value = get(index) + delta;
    set(index, value);
    return value;
  }

  public void fill(double value) {
    for (long i = 0; i < length; i++) segment.setAtIndex(LAYOUT, i, value);
  }

  /** Copy {@code src} to the start of this array. */
  public void copyFrom(double[] src) {
    MemorySegment.copy(src, 0, segment, LAYOUT, 0, src.length);
  }

  public double[] toArray() {
    return segment.toArray(LAYOUT);
  }

  /** The latest write to this element from any thread. */
  public double getVolatile(long index) {
    return (double) ATOMIC.getVolatile(segment, offset(index));
  }

  /** Write an element every thread sees at once. */
  public void setVolatile(long index, double value) {
    ATOMIC.setVolatile(segment, offset(index), value);
  }

  /**
   * Atomically set the element to {@code update} if it holds {@code expected}; false if it did not.
   * Compares bits, so NaN matches NaN and -0.0 does not match 0.0.
   */
  public boolean compareAndSet(long index, double expected, double update) {
    return ATOMIC.compareAndSet(segment, offset(index), expected, update);
  }

  // Checked before multiplying, so a huge index cannot wrap around into range.
  private long offset(long index) {
    return Objects.checkIndex(index, length) * Double.BYTES;
  }
}
