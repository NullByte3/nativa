package club.nullbyte3.nativa.struct;

import club.nullbyte3.nativa.Memory;
import java.lang.foreign.ValueLayout;

/**
 * Struct elements off the heap, read and written one field at a time. {@code index} is a position
 * in a {@link StructArray} and a count back from the newest in a {@link StructRing}.
 */
public abstract sealed class StructMemory extends Memory permits StructArray, StructRing {

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

  // Both checks fold away for a static final field, so a mistake throws without slowing a correct
  // read.
  private long offset(long index, StructField field, Class<?> type) {
    if (field.struct() != struct || field.type() != type) {
      throw new IllegalArgumentException(
          field.name() + " is not a " + type + " field of " + struct);
    }
    return slot(index) * field.stride() + field.offset();
  }
}
