package club.nullbyte3.nativa.array;

import club.nullbyte3.nativa.Memory;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/** A fixed-length {@code byte} array, zeroed when allocated. */
public final class ByteArray extends Memory {

  private static final ValueLayout.OfByte LAYOUT = ValueLayout.JAVA_BYTE;
  private static final VarHandle ATOMIC = LAYOUT.varHandle();
  private final long length;

  private ByteArray(Arena arena, long length) {
    super(arena, Math.multiplyExact(length, Byte.BYTES), Byte.BYTES);
    this.length = length;
  }

  public static ByteArray allocate(long length) {
    return new ByteArray(null, length);
  }

  /** Allocate in {@code arena}, which frees it when it closes; see {@link Memory#close()}. */
  public static ByteArray allocate(Arena arena, long length) {
    return new ByteArray(Objects.requireNonNull(arena), length);
  }

  /** A copy of {@code values}. */
  public static ByteArray of(byte... values) {
    ByteArray array = new ByteArray(null, values.length);
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

  /** The latest write to this element from any thread. */
  public byte getVolatile(long index) {
    return (byte) ATOMIC.getVolatile(segment, Objects.checkIndex(index, length));
  }

  /** Write an element every thread sees at once. */
  public void setVolatile(long index, byte value) {
    ATOMIC.setVolatile(segment, Objects.checkIndex(index, length), value);
  }

  /**
   * Atomically set the element to {@code update} if it holds {@code expected}; false if it did not.
   */
  public boolean compareAndSet(long index, byte expected, byte update) {
    return compareAndSetSmall(Objects.checkIndex(index, length), Byte.BYTES, expected, update);
  }

  /** Atomically set the element and return what it held before. */
  public byte getAndSet(long index, byte value) {
    return (byte) getAndSetSmall(Objects.checkIndex(index, length), Byte.BYTES, value);
  }

  /** Atomically add {@code delta} and return what the element held before. Wraps on overflow. */
  public byte getAndAdd(long index, byte delta) {
    return (byte) getAndAddSmall(Objects.checkIndex(index, length), Byte.BYTES, delta);
  }

  /** Set the elements from {@code from} up to, not including, {@code to}. */
  public void fill(long from, long to, byte value) {
    Objects.checkFromToIndex(from, to, length);
    segment.asSlice(from, to - from).fill(value);
  }

  /** Copy {@code length} elements of {@code src}, from {@code srcPos}, to {@code index}. */
  public void copyFrom(byte[] src, int srcPos, long index, int length) {
    Objects.checkFromIndexSize(srcPos, length, src.length);
    Objects.checkFromIndexSize(index, length, this.length);
    MemorySegment.copy(src, srcPos, segment, LAYOUT, index * Byte.BYTES, length);
  }

  /** Copy {@code length} elements from {@code index} to {@code dst}, from {@code dstPos}. */
  public void copyTo(long index, byte[] dst, int dstPos, int length) {
    Objects.checkFromIndexSize(index, length, this.length);
    Objects.checkFromIndexSize(dstPos, length, dst.length);
    MemorySegment.copy(segment, LAYOUT, index * Byte.BYTES, dst, dstPos, length);
  }

  /**
   * Copy {@code length} elements from {@code index} to {@code dstIndex} in {@code dst}, which may
   * be this array; the ranges may overlap.
   */
  public void copyTo(long index, ByteArray dst, long dstIndex, long length) {
    Objects.checkFromIndexSize(index, length, this.length);
    Objects.checkFromIndexSize(dstIndex, length, dst.length);
    MemorySegment.copy(
        segment, index * Byte.BYTES, dst.segment, dstIndex * Byte.BYTES, length * Byte.BYTES);
  }

  /** A copy of {@code length} elements from {@code from}. */
  public byte[] toArray(long from, int length) {
    byte[] out = new byte[length];
    copyTo(from, out, 0, length);
    return out;
  }
}
