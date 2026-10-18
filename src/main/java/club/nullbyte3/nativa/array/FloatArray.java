package club.nullbyte3.nativa.array;

import club.nullbyte3.nativa.Memory;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/** A fixed-length {@code float} array, zeroed when allocated. */
public final class FloatArray extends Memory {

  private static final ValueLayout.OfFloat LAYOUT = ValueLayout.JAVA_FLOAT;
  private static final VarHandle ATOMIC = LAYOUT.varHandle();
  private final long length;

  private FloatArray(long length) {
    super(Math.multiplyExact(length, Float.BYTES), Float.BYTES);
    this.length = length;
  }

  public static FloatArray allocate(long length) {
    return new FloatArray(length);
  }

  /** A copy of {@code values}. */
  public static FloatArray of(float... values) {
    FloatArray array = new FloatArray(values.length);
    array.copyFrom(values);
    return array;
  }

  public long length() {
    return length;
  }

  public float get(long index) {
    return segment.getAtIndex(LAYOUT, index);
  }

  public void set(long index, float value) {
    segment.setAtIndex(LAYOUT, index, value);
  }

  /** Add {@code delta} and return the new value. A read then a write, not atomic. */
  public float add(long index, float delta) {
    float value = get(index) + delta;
    set(index, value);
    return value;
  }

  public void fill(float value) {
    for (long i = 0; i < length; i++) segment.setAtIndex(LAYOUT, i, value);
  }

  /** Copy {@code src} to the start of this array. */
  public void copyFrom(float[] src) {
    MemorySegment.copy(src, 0, segment, LAYOUT, 0, src.length);
  }

  public float[] toArray() {
    return segment.toArray(LAYOUT);
  }

  /** The latest write to this element from any thread. */
  public float getVolatile(long index) {
    return (float) ATOMIC.getVolatile(segment, offset(index));
  }

  /** Write an element every thread sees at once. */
  public void setVolatile(long index, float value) {
    ATOMIC.setVolatile(segment, offset(index), value);
  }

  /**
   * Atomically set the element to {@code update} if it holds {@code expected}; false if it did not.
   * Compares bits, so NaN matches NaN and -0.0 does not match 0.0.
   */
  public boolean compareAndSet(long index, float expected, float update) {
    return ATOMIC.compareAndSet(segment, offset(index), expected, update);
  }

  // Checked before multiplying, so a huge index cannot wrap around into range.
  private long offset(long index) {
    return Objects.checkIndex(index, length) * Float.BYTES;
  }
}
