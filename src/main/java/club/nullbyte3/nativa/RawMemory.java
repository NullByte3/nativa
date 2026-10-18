package club.nullbyte3.nativa;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

/**
 * Raw bytes addressed by offset, for packing mixed fields by hand. Native byte order, any
 * alignment, except the atomics: those need an offset that is a multiple of the value size.
 */
public final class RawMemory extends Memory {

  private static final ValueLayout.OfShort SHORT = ValueLayout.JAVA_SHORT_UNALIGNED;
  private static final ValueLayout.OfChar CHAR = ValueLayout.JAVA_CHAR_UNALIGNED;
  private static final ValueLayout.OfInt INT = ValueLayout.JAVA_INT_UNALIGNED;
  private static final ValueLayout.OfLong LONG = ValueLayout.JAVA_LONG_UNALIGNED;
  private static final ValueLayout.OfFloat FLOAT = ValueLayout.JAVA_FLOAT_UNALIGNED;
  private static final ValueLayout.OfDouble DOUBLE = ValueLayout.JAVA_DOUBLE_UNALIGNED;

  // Atomic access modes only exist on aligned layouts.
  private static final VarHandle ATOMIC_INT = ValueLayout.JAVA_INT.varHandle();
  private static final VarHandle ATOMIC_LONG = ValueLayout.JAVA_LONG.varHandle();

  private RawMemory(long bytes) {
    super(bytes, Long.BYTES);
  }

  public static RawMemory allocate(long bytes) {
    return new RawMemory(bytes);
  }

  public byte getByte(long offset) {
    return segment.get(ValueLayout.JAVA_BYTE, offset);
  }

  public void setByte(long offset, byte value) {
    segment.set(ValueLayout.JAVA_BYTE, offset, value);
  }

  /** One byte, any non-zero value being {@code true}. */
  public boolean getBoolean(long offset) {
    return segment.get(ValueLayout.JAVA_BYTE, offset) != 0;
  }

  public void setBoolean(long offset, boolean value) {
    segment.set(ValueLayout.JAVA_BYTE, offset, value ? (byte) 1 : (byte) 0);
  }

  public short getShort(long offset) {
    return segment.get(SHORT, offset);
  }

  public void setShort(long offset, short value) {
    segment.set(SHORT, offset, value);
  }

  public char getChar(long offset) {
    return segment.get(CHAR, offset);
  }

  public void setChar(long offset, char value) {
    segment.set(CHAR, offset, value);
  }

  public int getInt(long offset) {
    return segment.get(INT, offset);
  }

  public void setInt(long offset, int value) {
    segment.set(INT, offset, value);
  }

  public long getLong(long offset) {
    return segment.get(LONG, offset);
  }

  public void setLong(long offset, long value) {
    segment.set(LONG, offset, value);
  }

  public float getFloat(long offset) {
    return segment.get(FLOAT, offset);
  }

  public void setFloat(long offset, float value) {
    segment.set(FLOAT, offset, value);
  }

  public double getDouble(long offset) {
    return segment.get(DOUBLE, offset);
  }

  public void setDouble(long offset, double value) {
    segment.set(DOUBLE, offset, value);
  }

  /** Set {@code length} bytes from {@code offset} to {@code value}. */
  public void fill(long offset, long length, byte value) {
    segment.asSlice(offset, length).fill(value);
  }

  /** Copy {@code length} bytes to {@code dstOffset} in {@code dst}; the ranges may overlap. */
  public void copyTo(long offset, RawMemory dst, long dstOffset, long length) {
    MemorySegment.copy(segment, offset, dst.segment, dstOffset, length);
  }

  /** The latest write to this int from any thread. */
  public int getIntVolatile(long offset) {
    return (int) ATOMIC_INT.getVolatile(segment, offset);
  }

  /** Write an int every thread sees at once. */
  public void setIntVolatile(long offset, int value) {
    ATOMIC_INT.setVolatile(segment, offset, value);
  }

  /** Atomically set the int to {@code update} if it holds {@code expected}; false if it did not. */
  public boolean compareAndSetInt(long offset, int expected, int update) {
    return ATOMIC_INT.compareAndSet(segment, offset, expected, update);
  }

  /** Atomically add {@code delta} to the int and return what it held before. */
  public int getAndAddInt(long offset, int delta) {
    return (int) ATOMIC_INT.getAndAdd(segment, offset, delta);
  }

  /** The latest write to this long from any thread. */
  public long getLongVolatile(long offset) {
    return (long) ATOMIC_LONG.getVolatile(segment, offset);
  }

  /** Write a long every thread sees at once. */
  public void setLongVolatile(long offset, long value) {
    ATOMIC_LONG.setVolatile(segment, offset, value);
  }

  /**
   * Atomically set the long to {@code update} if it holds {@code expected}; false if it did not.
   */
  public boolean compareAndSetLong(long offset, long expected, long update) {
    return ATOMIC_LONG.compareAndSet(segment, offset, expected, update);
  }

  /** Atomically add {@code delta} to the long and return what it held before. */
  public long getAndAddLong(long offset, long delta) {
    return (long) ATOMIC_LONG.getAndAdd(segment, offset, delta);
  }
}
