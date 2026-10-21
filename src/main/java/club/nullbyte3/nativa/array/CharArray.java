package club.nullbyte3.nativa.array;

import club.nullbyte3.nativa.Memory;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/** A fixed-length {@code char} array, zeroed when allocated. */
public final class CharArray extends Memory {

  private static final ValueLayout.OfChar LAYOUT = ValueLayout.JAVA_CHAR;
  private static final VarHandle ATOMIC = LAYOUT.varHandle();
  private final long length;

  private CharArray(long length) {
    super(Math.multiplyExact(length, Character.BYTES), Character.BYTES);
    this.length = length;
  }

  public static CharArray allocate(long length) {
    return new CharArray(length);
  }

  /** A copy of {@code values}. */
  public static CharArray of(char... values) {
    CharArray array = new CharArray(values.length);
    array.copyFrom(values);
    return array;
  }

  public long length() {
    return length;
  }

  public char get(long index) {
    return segment.getAtIndex(LAYOUT, index);
  }

  public void set(long index, char value) {
    segment.setAtIndex(LAYOUT, index, value);
  }

  public void fill(char value) {
    for (long i = 0; i < length; i++) segment.setAtIndex(LAYOUT, i, value);
  }

  /** Copy {@code src} to the start of this array. */
  public void copyFrom(char[] src) {
    MemorySegment.copy(src, 0, segment, LAYOUT, 0, src.length);
  }

  public char[] toArray() {
    return segment.toArray(LAYOUT);
  }

  /** The latest write to this element from any thread. */
  public char getVolatile(long index) {
    return (char) ATOMIC.getVolatile(segment, offset(index));
  }

  /** Write an element every thread sees at once. */
  public void setVolatile(long index, char value) {
    ATOMIC.setVolatile(segment, offset(index), value);
  }

  /**
   * Atomically set the element to {@code update} if it holds {@code expected}; false if it did not.
   */
  public boolean compareAndSet(long index, char expected, char update) {
    return compareAndSetSmall(offset(index), Character.BYTES, expected, update);
  }

  /** Atomically set the element and return what it held before. */
  public char getAndSet(long index, char value) {
    return (char) getAndSetSmall(offset(index), Character.BYTES, value);
  }

  /** Set the elements from {@code from} up to, not including, {@code to}. */
  public void fill(long from, long to, char value) {
    Objects.checkFromToIndex(from, to, length);
    for (long i = from; i < to; i++) segment.setAtIndex(LAYOUT, i, value);
  }

  /** Copy {@code length} elements of {@code src}, from {@code srcPos}, to {@code index}. */
  public void copyFrom(char[] src, int srcPos, long index, int length) {
    Objects.checkFromIndexSize(srcPos, length, src.length);
    Objects.checkFromIndexSize(index, length, this.length);
    MemorySegment.copy(src, srcPos, segment, LAYOUT, index * Character.BYTES, length);
  }

  /** Copy {@code length} elements from {@code index} to {@code dst}, from {@code dstPos}. */
  public void copyTo(long index, char[] dst, int dstPos, int length) {
    Objects.checkFromIndexSize(index, length, this.length);
    Objects.checkFromIndexSize(dstPos, length, dst.length);
    MemorySegment.copy(segment, LAYOUT, index * Character.BYTES, dst, dstPos, length);
  }

  /**
   * Copy {@code length} elements from {@code index} to {@code dstIndex} in {@code dst}, which may
   * be this array; the ranges may overlap.
   */
  public void copyTo(long index, CharArray dst, long dstIndex, long length) {
    Objects.checkFromIndexSize(index, length, this.length);
    Objects.checkFromIndexSize(dstIndex, length, dst.length);
    MemorySegment.copy(
        segment,
        index * Character.BYTES,
        dst.segment,
        dstIndex * Character.BYTES,
        length * Character.BYTES);
  }

  /** A copy of {@code length} elements from {@code from}. */
  public char[] toArray(long from, int length) {
    char[] out = new char[length];
    copyTo(from, out, 0, length);
    return out;
  }

  // Checked before multiplying, so a huge index cannot wrap around into range.
  private long offset(long index) {
    return Objects.checkIndex(index, length) * Character.BYTES;
  }
}
