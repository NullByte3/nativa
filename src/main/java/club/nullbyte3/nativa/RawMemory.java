package club.nullbyte3.nativa;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/**
 * Raw bytes addressed by offset, for packing mixed fields by hand. Native byte order, any
 * alignment, except the atomics: those need an offset that is a multiple of the value size. Every
 * type has atomics; the numeric ones also have {@code getAndAdd}.
 */
public final class RawMemory extends Memory {

  private static final ValueLayout.OfShort SHORT = ValueLayout.JAVA_SHORT_UNALIGNED;
  private static final ValueLayout.OfChar CHAR = ValueLayout.JAVA_CHAR_UNALIGNED;
  private static final ValueLayout.OfInt INT = ValueLayout.JAVA_INT_UNALIGNED;
  private static final ValueLayout.OfLong LONG = ValueLayout.JAVA_LONG_UNALIGNED;
  private static final ValueLayout.OfFloat FLOAT = ValueLayout.JAVA_FLOAT_UNALIGNED;
  private static final ValueLayout.OfDouble DOUBLE = ValueLayout.JAVA_DOUBLE_UNALIGNED;

  // Atomic access modes only exist on aligned layouts.
  private static final VarHandle ATOMIC_BYTE = ValueLayout.JAVA_BYTE.varHandle();
  private static final VarHandle ATOMIC_SHORT = ValueLayout.JAVA_SHORT.varHandle();
  private static final VarHandle ATOMIC_CHAR = ValueLayout.JAVA_CHAR.varHandle();
  private static final VarHandle ATOMIC_INT = ValueLayout.JAVA_INT.varHandle();
  private static final VarHandle ATOMIC_LONG = ValueLayout.JAVA_LONG.varHandle();
  private static final VarHandle ATOMIC_FLOAT = ValueLayout.JAVA_FLOAT.varHandle();
  private static final VarHandle ATOMIC_DOUBLE = ValueLayout.JAVA_DOUBLE.varHandle();

  private RawMemory(Arena arena, long bytes) {
    super(arena, bytes, Long.BYTES);
  }

  public static RawMemory allocate(long bytes) {
    return new RawMemory(null, bytes);
  }

  /** Allocate in {@code arena}, which frees it when it closes; see {@link Memory#close()}. */
  public static RawMemory allocate(Arena arena, long bytes) {
    return new RawMemory(Objects.requireNonNull(arena), bytes);
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

  /** The latest write to this byte from any thread. */
  public byte getByteVolatile(long offset) {
    return (byte) ATOMIC_BYTE.getVolatile(segment, offset);
  }

  /** Write a byte every thread sees at once. */
  public void setByteVolatile(long offset, byte value) {
    ATOMIC_BYTE.setVolatile(segment, offset, value);
  }

  /**
   * Atomically set the byte to {@code update} if it holds {@code expected}; false if it did not.
   */
  public boolean compareAndSetByte(long offset, byte expected, byte update) {
    return compareAndSetSmall(offset, Byte.BYTES, expected, update);
  }

  /** Atomically set the byte and return what it held before. */
  public byte getAndSetByte(long offset, byte value) {
    return (byte) getAndSetSmall(offset, Byte.BYTES, value);
  }

  /** Atomically add {@code delta} to the byte and return what it held before. Wraps on overflow. */
  public byte getAndAddByte(long offset, byte delta) {
    return (byte) getAndAddSmall(offset, Byte.BYTES, delta);
  }

  /** The latest write to this boolean from any thread. */
  public boolean getBooleanVolatile(long offset) {
    return getByteVolatile(offset) != 0;
  }

  /** Write a boolean every thread sees at once. */
  public void setBooleanVolatile(long offset, boolean value) {
    setByteVolatile(offset, value ? (byte) 1 : (byte) 0);
  }

  /**
   * Atomically set the boolean to {@code update} if it holds {@code expected}; false if it did not.
   * A stored byte other than 0 or 1 never matches.
   */
  public boolean compareAndSetBoolean(long offset, boolean expected, boolean update) {
    return compareAndSetByte(offset, expected ? (byte) 1 : (byte) 0, update ? (byte) 1 : (byte) 0);
  }

  /** Atomically set the boolean and return what it held before. */
  public boolean getAndSetBoolean(long offset, boolean value) {
    return getAndSetByte(offset, value ? (byte) 1 : (byte) 0) != 0;
  }

  /** The latest write to this short from any thread. */
  public short getShortVolatile(long offset) {
    return (short) ATOMIC_SHORT.getVolatile(segment, offset);
  }

  /** Write a short every thread sees at once. */
  public void setShortVolatile(long offset, short value) {
    ATOMIC_SHORT.setVolatile(segment, offset, value);
  }

  /**
   * Atomically set the short to {@code update} if it holds {@code expected}; false if it did not.
   */
  public boolean compareAndSetShort(long offset, short expected, short update) {
    return compareAndSetSmall(offset, Short.BYTES, expected, update);
  }

  /** Atomically set the short and return what it held before. */
  public short getAndSetShort(long offset, short value) {
    return (short) getAndSetSmall(offset, Short.BYTES, value);
  }

  /**
   * Atomically add {@code delta} to the short and return what it held before. Wraps on overflow.
   */
  public short getAndAddShort(long offset, short delta) {
    return (short) getAndAddSmall(offset, Short.BYTES, delta);
  }

  /** The latest write to this char from any thread. */
  public char getCharVolatile(long offset) {
    return (char) ATOMIC_CHAR.getVolatile(segment, offset);
  }

  /** Write a char every thread sees at once. */
  public void setCharVolatile(long offset, char value) {
    ATOMIC_CHAR.setVolatile(segment, offset, value);
  }

  /**
   * Atomically set the char to {@code update} if it holds {@code expected}; false if it did not.
   */
  public boolean compareAndSetChar(long offset, char expected, char update) {
    return compareAndSetSmall(offset, Character.BYTES, expected, update);
  }

  /** Atomically set the char and return what it held before. */
  public char getAndSetChar(long offset, char value) {
    return (char) getAndSetSmall(offset, Character.BYTES, value);
  }

  /** Atomically set the int and return what it held before. */
  public int getAndSetInt(long offset, int value) {
    return (int) ATOMIC_INT.getAndSet(segment, offset, value);
  }

  /** Atomically set the long and return what it held before. */
  public long getAndSetLong(long offset, long value) {
    return (long) ATOMIC_LONG.getAndSet(segment, offset, value);
  }

  /** The latest write to this float from any thread. */
  public float getFloatVolatile(long offset) {
    return (float) ATOMIC_FLOAT.getVolatile(segment, offset);
  }

  /** Write a float every thread sees at once. */
  public void setFloatVolatile(long offset, float value) {
    ATOMIC_FLOAT.setVolatile(segment, offset, value);
  }

  /**
   * Atomically set the float to {@code update} if its bits equal those of {@code expected}; false
   * if they did not.
   */
  public boolean compareAndSetFloat(long offset, float expected, float update) {
    return ATOMIC_FLOAT.compareAndSet(segment, offset, expected, update);
  }

  /** Atomically set the float and return what it held before. */
  public float getAndSetFloat(long offset, float value) {
    return (float) ATOMIC_FLOAT.getAndSet(segment, offset, value);
  }

  /** Atomically add {@code delta} to the float and return what it held before. */
  public float getAndAddFloat(long offset, float delta) {
    while (true) {
      float before = getFloatVolatile(offset);
      if (compareAndSetFloat(offset, before, before + delta)) return before;
    }
  }

  /** The latest write to this double from any thread. */
  public double getDoubleVolatile(long offset) {
    return (double) ATOMIC_DOUBLE.getVolatile(segment, offset);
  }

  /** Write a double every thread sees at once. */
  public void setDoubleVolatile(long offset, double value) {
    ATOMIC_DOUBLE.setVolatile(segment, offset, value);
  }

  /**
   * Atomically set the double to {@code update} if its bits equal those of {@code expected}; false
   * if they did not.
   */
  public boolean compareAndSetDouble(long offset, double expected, double update) {
    return ATOMIC_DOUBLE.compareAndSet(segment, offset, expected, update);
  }

  /** Atomically set the double and return what it held before. */
  public double getAndSetDouble(long offset, double value) {
    return (double) ATOMIC_DOUBLE.getAndSet(segment, offset, value);
  }

  /** Atomically add {@code delta} to the double and return what it held before. */
  public double getAndAddDouble(long offset, double delta) {
    while (true) {
      double before = getDoubleVolatile(offset);
      if (compareAndSetDouble(offset, before, before + delta)) return before;
    }
  }
}
