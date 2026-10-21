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

  /** Atomically set the element and return what it held before. */
  public double getAndSet(long index, double value) {
    return (double) ATOMIC.getAndSet(segment, offset(index), value);
  }

  /** Atomically add {@code delta} and return what the element held before. */
  public double getAndAdd(long index, double delta) {
    while (true) {
      double before = getVolatile(index);
      if (compareAndSet(index, before, before + delta)) return before;
    }
  }

  /** Set the elements from {@code from} up to, not including, {@code to}. */
  public void fill(long from, long to, double value) {
    Objects.checkFromToIndex(from, to, length);
    for (long i = from; i < to; i++) segment.setAtIndex(LAYOUT, i, value);
  }

  /** Copy {@code length} elements of {@code src}, from {@code srcPos}, to {@code index}. */
  public void copyFrom(double[] src, int srcPos, long index, int length) {
    Objects.checkFromIndexSize(srcPos, length, src.length);
    Objects.checkFromIndexSize(index, length, this.length);
    MemorySegment.copy(src, srcPos, segment, LAYOUT, index * Double.BYTES, length);
  }

  /** Copy {@code length} elements from {@code index} to {@code dst}, from {@code dstPos}. */
  public void copyTo(long index, double[] dst, int dstPos, int length) {
    Objects.checkFromIndexSize(index, length, this.length);
    Objects.checkFromIndexSize(dstPos, length, dst.length);
    MemorySegment.copy(segment, LAYOUT, index * Double.BYTES, dst, dstPos, length);
  }

  /**
   * Copy {@code length} elements from {@code index} to {@code dstIndex} in {@code dst}, which may
   * be this array; the ranges may overlap.
   */
  public void copyTo(long index, DoubleArray dst, long dstIndex, long length) {
    Objects.checkFromIndexSize(index, length, this.length);
    Objects.checkFromIndexSize(dstIndex, length, dst.length);
    MemorySegment.copy(
        segment, index * Double.BYTES, dst.segment, dstIndex * Double.BYTES, length * Double.BYTES);
  }

  /** A copy of {@code length} elements from {@code from}. */
  public double[] toArray(long from, int length) {
    double[] out = new double[length];
    copyTo(from, out, 0, length);
    return out;
  }

  // Checked before multiplying, so a huge index cannot wrap around into range.
  private long offset(long index) {
    return Objects.checkIndex(index, length) * Double.BYTES;
  }
}
