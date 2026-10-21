package club.nullbyte3.nativa.struct;

import club.nullbyte3.nativa.Memory;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

/**
 * Struct elements off the heap, read and written one field at a time. {@code index} is a position
 * in a {@link StructArray} and a count back from the newest in a {@link StructRing}. Every field
 * type has volatile and atomic access; the numeric ones also have {@code getAndAdd}. Fields are
 * always aligned, so an atomic never needs an offset check.
 */
public abstract sealed class StructMemory extends Memory permits StructArray, StructRing {

  private static final VarHandle ATOMIC_BYTE = ValueLayout.JAVA_BYTE.varHandle();
  private static final VarHandle ATOMIC_SHORT = ValueLayout.JAVA_SHORT.varHandle();
  private static final VarHandle ATOMIC_CHAR = ValueLayout.JAVA_CHAR.varHandle();
  private static final VarHandle ATOMIC_INT = ValueLayout.JAVA_INT.varHandle();
  private static final VarHandle ATOMIC_LONG = ValueLayout.JAVA_LONG.varHandle();
  private static final VarHandle ATOMIC_FLOAT = ValueLayout.JAVA_FLOAT.varHandle();
  private static final VarHandle ATOMIC_DOUBLE = ValueLayout.JAVA_DOUBLE.varHandle();

  private final Struct struct;

  StructMemory(Struct struct, long elements) {
    super(Math.multiplyExact(elements, struct.byteSize()), struct.alignment());
    this.struct = struct;
  }

  public final Struct struct() {
    return struct;
  }

  /** The element slot {@code index} names; throws when it is out of range. */
  abstract long slot(long index);

  /** How many elements can be indexed: the length of an array, the size of a ring. */
  abstract long count();

  public final boolean getBoolean(long index, StructField field) {
    return segment.get(ValueLayout.JAVA_BYTE, offset(index, field, boolean.class)) != 0;
  }

  public final void setBoolean(long index, StructField field, boolean value) {
    segment.set(
        ValueLayout.JAVA_BYTE, offset(index, field, boolean.class), value ? (byte) 1 : (byte) 0);
  }

  public final byte getByte(long index, StructField field) {
    return segment.get(ValueLayout.JAVA_BYTE, offset(index, field, byte.class));
  }

  public final void setByte(long index, StructField field, byte value) {
    segment.set(ValueLayout.JAVA_BYTE, offset(index, field, byte.class), value);
  }

  public final short getShort(long index, StructField field) {
    return segment.get(ValueLayout.JAVA_SHORT, offset(index, field, short.class));
  }

  public final void setShort(long index, StructField field, short value) {
    segment.set(ValueLayout.JAVA_SHORT, offset(index, field, short.class), value);
  }

  public final char getChar(long index, StructField field) {
    return segment.get(ValueLayout.JAVA_CHAR, offset(index, field, char.class));
  }

  public final void setChar(long index, StructField field, char value) {
    segment.set(ValueLayout.JAVA_CHAR, offset(index, field, char.class), value);
  }

  public final int getInt(long index, StructField field) {
    return segment.get(ValueLayout.JAVA_INT, offset(index, field, int.class));
  }

  public final void setInt(long index, StructField field, int value) {
    segment.set(ValueLayout.JAVA_INT, offset(index, field, int.class), value);
  }

  public final long getLong(long index, StructField field) {
    return segment.get(ValueLayout.JAVA_LONG, offset(index, field, long.class));
  }

  public final void setLong(long index, StructField field, long value) {
    segment.set(ValueLayout.JAVA_LONG, offset(index, field, long.class), value);
  }

  public final float getFloat(long index, StructField field) {
    return segment.get(ValueLayout.JAVA_FLOAT, offset(index, field, float.class));
  }

  public final void setFloat(long index, StructField field, float value) {
    segment.set(ValueLayout.JAVA_FLOAT, offset(index, field, float.class), value);
  }

  public final double getDouble(long index, StructField field) {
    return segment.get(ValueLayout.JAVA_DOUBLE, offset(index, field, double.class));
  }

  public final void setDouble(long index, StructField field, double value) {
    segment.set(ValueLayout.JAVA_DOUBLE, offset(index, field, double.class), value);
  }

  /** Set this field of every element to {@code value}. */
  public final void fillBoolean(StructField field, boolean value) {
    check(field, boolean.class);
    for (long i = 0, n = count(); i < n; i++) setBoolean(i, field, value);
  }

  /** Set this field of every element to {@code value}. */
  public final void fillByte(StructField field, byte value) {
    check(field, byte.class);
    for (long i = 0, n = count(); i < n; i++) setByte(i, field, value);
  }

  /** Set this field of every element to {@code value}. */
  public final void fillShort(StructField field, short value) {
    check(field, short.class);
    for (long i = 0, n = count(); i < n; i++) setShort(i, field, value);
  }

  /** Set this field of every element to {@code value}. */
  public final void fillChar(StructField field, char value) {
    check(field, char.class);
    for (long i = 0, n = count(); i < n; i++) setChar(i, field, value);
  }

  /** Set this field of every element to {@code value}. */
  public final void fillInt(StructField field, int value) {
    check(field, int.class);
    for (long i = 0, n = count(); i < n; i++) setInt(i, field, value);
  }

  /** Set this field of every element to {@code value}. */
  public final void fillLong(StructField field, long value) {
    check(field, long.class);
    for (long i = 0, n = count(); i < n; i++) setLong(i, field, value);
  }

  /** Set this field of every element to {@code value}. */
  public final void fillFloat(StructField field, float value) {
    check(field, float.class);
    for (long i = 0, n = count(); i < n; i++) setFloat(i, field, value);
  }

  /** Set this field of every element to {@code value}. */
  public final void fillDouble(StructField field, double value) {
    check(field, double.class);
    for (long i = 0, n = count(); i < n; i++) setDouble(i, field, value);
  }

  public final boolean getBooleanVolatile(long index, StructField field) {
    return (byte) ATOMIC_BYTE.getVolatile(segment, offset(index, field, boolean.class)) != 0;
  }

  public final void setBooleanVolatile(long index, StructField field, boolean value) {
    ATOMIC_BYTE.setVolatile(
        segment, offset(index, field, boolean.class), value ? (byte) 1 : (byte) 0);
  }

  /** A stored byte other than 0 or 1 never matches. */
  public final boolean compareAndSetBoolean(
      long index, StructField field, boolean expected, boolean update) {
    return compareAndSetSmall(
        offset(index, field, boolean.class),
        Byte.BYTES,
        expected ? (byte) 1 : (byte) 0,
        update ? (byte) 1 : (byte) 0);
  }

  public final boolean getAndSetBoolean(long index, StructField field, boolean value) {
    return getAndSetSmall(offset(index, field, boolean.class), Byte.BYTES, value ? 1 : 0) != 0;
  }

  public final byte getByteVolatile(long index, StructField field) {
    return (byte) ATOMIC_BYTE.getVolatile(segment, offset(index, field, byte.class));
  }

  public final void setByteVolatile(long index, StructField field, byte value) {
    ATOMIC_BYTE.setVolatile(segment, offset(index, field, byte.class), value);
  }

  /** Atomically set the field to {@code update} if it holds {@code expected}. */
  public final boolean compareAndSetByte(
      long index, StructField field, byte expected, byte update) {
    return compareAndSetSmall(offset(index, field, byte.class), Byte.BYTES, expected, update);
  }

  public final byte getAndSetByte(long index, StructField field, byte value) {
    return (byte) getAndSetSmall(offset(index, field, byte.class), Byte.BYTES, value);
  }

  /** Atomically add {@code delta} and return what the field held before. Wraps on overflow. */
  public final byte getAndAddByte(long index, StructField field, byte delta) {
    return (byte) getAndAddSmall(offset(index, field, byte.class), Byte.BYTES, delta);
  }

  public final short getShortVolatile(long index, StructField field) {
    return (short) ATOMIC_SHORT.getVolatile(segment, offset(index, field, short.class));
  }

  public final void setShortVolatile(long index, StructField field, short value) {
    ATOMIC_SHORT.setVolatile(segment, offset(index, field, short.class), value);
  }

  /** Atomically set the field to {@code update} if it holds {@code expected}. */
  public final boolean compareAndSetShort(
      long index, StructField field, short expected, short update) {
    return compareAndSetSmall(offset(index, field, short.class), Short.BYTES, expected, update);
  }

  public final short getAndSetShort(long index, StructField field, short value) {
    return (short) getAndSetSmall(offset(index, field, short.class), Short.BYTES, value);
  }

  /** Atomically add {@code delta} and return what the field held before. Wraps on overflow. */
  public final short getAndAddShort(long index, StructField field, short delta) {
    return (short) getAndAddSmall(offset(index, field, short.class), Short.BYTES, delta);
  }

  public final char getCharVolatile(long index, StructField field) {
    return (char) ATOMIC_CHAR.getVolatile(segment, offset(index, field, char.class));
  }

  public final void setCharVolatile(long index, StructField field, char value) {
    ATOMIC_CHAR.setVolatile(segment, offset(index, field, char.class), value);
  }

  /** Atomically set the field to {@code update} if it holds {@code expected}. */
  public final boolean compareAndSetChar(
      long index, StructField field, char expected, char update) {
    return compareAndSetSmall(offset(index, field, char.class), Character.BYTES, expected, update);
  }

  public final char getAndSetChar(long index, StructField field, char value) {
    return (char) getAndSetSmall(offset(index, field, char.class), Character.BYTES, value);
  }

  public final int getIntVolatile(long index, StructField field) {
    return (int) ATOMIC_INT.getVolatile(segment, offset(index, field, int.class));
  }

  public final void setIntVolatile(long index, StructField field, int value) {
    ATOMIC_INT.setVolatile(segment, offset(index, field, int.class), value);
  }

  /** Atomically set the field to {@code update} if it holds {@code expected}. */
  public final boolean compareAndSetInt(long index, StructField field, int expected, int update) {
    return ATOMIC_INT.compareAndSet(segment, offset(index, field, int.class), expected, update);
  }

  public final int getAndSetInt(long index, StructField field, int value) {
    return (int) ATOMIC_INT.getAndSet(segment, offset(index, field, int.class), value);
  }

  /** Atomically add {@code delta} and return what the field held before. */
  public final int getAndAddInt(long index, StructField field, int delta) {
    return (int) ATOMIC_INT.getAndAdd(segment, offset(index, field, int.class), delta);
  }

  public final long getLongVolatile(long index, StructField field) {
    return (long) ATOMIC_LONG.getVolatile(segment, offset(index, field, long.class));
  }

  public final void setLongVolatile(long index, StructField field, long value) {
    ATOMIC_LONG.setVolatile(segment, offset(index, field, long.class), value);
  }

  /** Atomically set the field to {@code update} if it holds {@code expected}. */
  public final boolean compareAndSetLong(
      long index, StructField field, long expected, long update) {
    return ATOMIC_LONG.compareAndSet(segment, offset(index, field, long.class), expected, update);
  }

  public final long getAndSetLong(long index, StructField field, long value) {
    return (long) ATOMIC_LONG.getAndSet(segment, offset(index, field, long.class), value);
  }

  /** Atomically add {@code delta} and return what the field held before. */
  public final long getAndAddLong(long index, StructField field, long delta) {
    return (long) ATOMIC_LONG.getAndAdd(segment, offset(index, field, long.class), delta);
  }

  public final float getFloatVolatile(long index, StructField field) {
    return (float) ATOMIC_FLOAT.getVolatile(segment, offset(index, field, float.class));
  }

  public final void setFloatVolatile(long index, StructField field, float value) {
    ATOMIC_FLOAT.setVolatile(segment, offset(index, field, float.class), value);
  }

  /**
   * Atomically set the field to {@code update} if it holds {@code expected}. Float bits are
   * compared, so NaN matches NaN and -0.0 does not match 0.0.
   */
  public final boolean compareAndSetFloat(
      long index, StructField field, float expected, float update) {
    return ATOMIC_FLOAT.compareAndSet(segment, offset(index, field, float.class), expected, update);
  }

  public final float getAndSetFloat(long index, StructField field, float value) {
    return (float) ATOMIC_FLOAT.getAndSet(segment, offset(index, field, float.class), value);
  }

  /** Atomically add {@code delta} and return what the field held before. */
  public final float getAndAddFloat(long index, StructField field, float delta) {
    long offset = offset(index, field, float.class);
    while (true) {
      float before = (float) ATOMIC_FLOAT.getVolatile(segment, offset);
      if (ATOMIC_FLOAT.compareAndSet(segment, offset, before, before + delta)) return before;
    }
  }

  public final double getDoubleVolatile(long index, StructField field) {
    return (double) ATOMIC_DOUBLE.getVolatile(segment, offset(index, field, double.class));
  }

  public final void setDoubleVolatile(long index, StructField field, double value) {
    ATOMIC_DOUBLE.setVolatile(segment, offset(index, field, double.class), value);
  }

  /**
   * Atomically set the field to {@code update} if it holds {@code expected}. Float bits are
   * compared, so NaN matches NaN and -0.0 does not match 0.0.
   */
  public final boolean compareAndSetDouble(
      long index, StructField field, double expected, double update) {
    return ATOMIC_DOUBLE.compareAndSet(
        segment, offset(index, field, double.class), expected, update);
  }

  public final double getAndSetDouble(long index, StructField field, double value) {
    return (double) ATOMIC_DOUBLE.getAndSet(segment, offset(index, field, double.class), value);
  }

  /** Atomically add {@code delta} and return what the field held before. */
  public final double getAndAddDouble(long index, StructField field, double delta) {
    long offset = offset(index, field, double.class);
    while (true) {
      double before = (double) ATOMIC_DOUBLE.getVolatile(segment, offset);
      if (ATOMIC_DOUBLE.compareAndSet(segment, offset, before, before + delta)) return before;
    }
  }

  // Both checks fold away for a static final field, so a mistake throws without slowing a correct
  // read.
  private long offset(long index, StructField field, Class<?> type) {
    check(field, type);
    return slot(index) * field.stride() + field.offset();
  }

  private void check(StructField field, Class<?> type) {
    if (field.struct() != struct || field.type() != type) {
      throw new IllegalArgumentException(
          field.name() + " is not a " + type + " field of " + struct);
    }
  }
}
