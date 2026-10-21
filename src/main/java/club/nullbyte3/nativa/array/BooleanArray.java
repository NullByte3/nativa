package club.nullbyte3.nativa.array;

import club.nullbyte3.nativa.Memory;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/**
 * A fixed-length {@code boolean} array packed one bit per entry, zeroed (all false) when allocated.
 * Entry {@code i} is bit {@code i % 64} of word {@code i / 64}, so on a little-endian machine the
 * bytes are laid out like a C bit set. The size is rounded up to whole 8-byte words. The unused
 * bits past the last entry are always zero.
 */
public final class BooleanArray extends Memory {

  private static final ValueLayout.OfLong LAYOUT = ValueLayout.JAVA_LONG;
  private static final VarHandle ATOMIC = LAYOUT.varHandle();
  private final long length;

  private BooleanArray(long length) {
    super(Math.multiplyExact(words(length), Long.BYTES), Long.BYTES);
    this.length = length;
  }

  public static BooleanArray allocate(long length) {
    return new BooleanArray(length);
  }

  /** A copy of {@code values}. */
  public static BooleanArray of(boolean... values) {
    BooleanArray array = new BooleanArray(values.length);
    array.copyFrom(values);
    return array;
  }

  public long length() {
    return length;
  }

  public boolean get(long index) {
    Objects.checkIndex(index, length);
    return (word(index >>> 6) & mask(index)) != 0;
  }

  public void set(long index, boolean value) {
    Objects.checkIndex(index, length);
    long word = index >>> 6;
    setWord(word, value ? word(word) | mask(index) : word(word) & ~mask(index));
  }

  /** Invert the entry and return its new value. A read then a write: not atomic. */
  public boolean flip(long index) {
    boolean value = !get(index);
    set(index, value);
    return value;
  }

  public void fill(boolean value) {
    long bits = value ? -1L : 0L;
    long words = words(length);
    for (long i = 0; i < words; i++) setWord(i, bits);
    clearTail();
  }

  /** Set the entries from {@code from} up to, not including, {@code to}. */
  public void fill(long from, long to, boolean value) {
    Objects.checkFromToIndex(from, to, length);
    if (from == to) return;
    long first = from >>> 6;
    long last = (to - 1) >>> 6;
    long firstMask = -1L << (from & 63);
    long lastMask = -1L >>> (-to & 63);
    if (first == last) {
      apply(first, firstMask & lastMask, value);
      return;
    }
    apply(first, firstMask, value);
    for (long i = first + 1; i < last; i++) setWord(i, value ? -1L : 0L);
    apply(last, lastMask, value);
  }

  /** Invert every entry. */
  public void not() {
    long words = words(length);
    for (long i = 0; i < words; i++) setWord(i, ~word(i));
    clearTail();
  }

  /** Keep an entry true only if it is also true in {@code other}, which must be as long. */
  public void and(BooleanArray other) {
    combine(other, 0);
  }

  /** Make an entry true if it is true in either; {@code other} must be as long. */
  public void or(BooleanArray other) {
    combine(other, 1);
  }

  /** Make an entry true if it differs from the one in {@code other}, which must be as long. */
  public void xor(BooleanArray other) {
    combine(other, 2);
  }

  /** Make an entry false if it is true in {@code other}, which must be as long. */
  public void andNot(BooleanArray other) {
    combine(other, 3);
  }

  /** How many entries are true. */
  public long cardinality() {
    long count = 0;
    long words = words(length);
    for (long i = 0; i < words; i++) count += Long.bitCount(word(i));
    return count;
  }

  /** Whether no entry is true. */
  public boolean isEmpty() {
    long words = words(length);
    for (long i = 0; i < words; i++) {
      if (word(i) != 0) return false;
    }
    return true;
  }

  /** The first true entry at or after {@code from}, or -1 if there is none. */
  public long nextSetBit(long from) {
    if (from < 0) throw new IndexOutOfBoundsException("negative index: " + from);
    if (from >= length) return -1;
    long words = words(length);
    long w = from >>> 6;
    long bits = word(w) & (-1L << (from & 63));
    while (bits == 0) {
      if (++w == words) return -1;
      bits = word(w);
    }
    return (w << 6) + Long.numberOfTrailingZeros(bits);
  }

  /** The first false entry at or after {@code from}, or -1 if there is none. */
  public long nextClearBit(long from) {
    if (from < 0) throw new IndexOutOfBoundsException("negative index: " + from);
    if (from >= length) return -1;
    long words = words(length);
    long w = from >>> 6;
    long bits = ~word(w) & (-1L << (from & 63));
    while (bits == 0) {
      if (++w == words) return -1;
      bits = ~word(w);
    }
    long index = (w << 6) + Long.numberOfTrailingZeros(bits);
    return index < length ? index : -1; // the zero bits past the end are not entries
  }

  /** Copy {@code src} to the start of this array. */
  public void copyFrom(boolean[] src) {
    Objects.checkFromIndexSize(0, src.length, length);
    for (int i = 0; i < src.length; i++) set(i, src[i]);
  }

  /** Copy {@code length} entries from {@code index} to {@code dstIndex} in {@code dst}. */
  public void copyTo(long index, BooleanArray dst, long dstIndex, long length) {
    Objects.checkFromIndexSize(index, length, this.length);
    Objects.checkFromIndexSize(dstIndex, length, dst.length);
    // Walk backwards when the target overlaps the source from above, so nothing is read after it
    // was overwritten.
    if (dst == this && dstIndex > index) {
      for (long i = length - 1; i >= 0; i--) dst.set(dstIndex + i, get(index + i));
    } else {
      for (long i = 0; i < length; i++) dst.set(dstIndex + i, get(index + i));
    }
  }

  public boolean[] toArray() {
    return toArray(0, Math.toIntExact(length));
  }

  /** A copy of {@code length} entries from {@code from}. */
  public boolean[] toArray(long from, int length) {
    Objects.checkFromIndexSize(from, length, this.length);
    boolean[] out = new boolean[length];
    for (int i = 0; i < length; i++) out[i] = get(from + i);
    return out;
  }

  /** The latest write to this entry from any thread. */
  public boolean getVolatile(long index) {
    return ((long) ATOMIC.getVolatile(segment, offset(index)) & mask(index)) != 0;
  }

  /** Write an entry every thread sees at once; other entries in the same word are untouched. */
  public void setVolatile(long index, boolean value) {
    getAndSet(index, value);
  }

  /** Atomically set the entry and return what it held before. */
  public boolean getAndSet(long index, boolean value) {
    long mask = mask(index);
    long offset = offset(index);
    long before =
        value
            ? (long) ATOMIC.getAndBitwiseOr(segment, offset, mask)
            : (long) ATOMIC.getAndBitwiseAnd(segment, offset, ~mask);
    return (before & mask) != 0;
  }

  /** Atomically invert the entry and return what it held before. */
  public boolean getAndFlip(long index) {
    long mask = mask(index);
    return ((long) ATOMIC.getAndBitwiseXor(segment, offset(index), mask) & mask) != 0;
  }

  /**
   * Atomically set the entry to {@code update} if it holds {@code expected}; false if it did not.
   */
  public boolean compareAndSet(long index, boolean expected, boolean update) {
    long mask = mask(index);
    long offset = offset(index);
    while (true) {
      long bits = (long) ATOMIC.getVolatile(segment, offset);
      if (((bits & mask) != 0) != expected) return false;
      long next = update ? bits | mask : bits & ~mask;
      if (next == bits || ATOMIC.compareAndSet(segment, offset, bits, next)) return true;
    }
  }

  private void combine(BooleanArray other, int op) {
    if (other.length != length) {
      throw new IllegalArgumentException("length " + other.length + " is not " + length);
    }
    long words = words(length);
    for (long i = 0; i < words; i++) {
      long a = word(i);
      long b = other.word(i);
      setWord(i, op == 0 ? a & b : op == 1 ? a | b : op == 2 ? a ^ b : a & ~b);
    }
  }

  private void apply(long word, long mask, boolean value) {
    setWord(word, value ? word(word) | mask : word(word) & ~mask);
  }

  // After filling or inverting whole words, the bits past the last entry must be zero again.
  private void clearTail() {
    if ((length & 63) != 0) {
      long last = words(length) - 1;
      setWord(last, word(last) & ((1L << (length & 63)) - 1));
    }
  }

  private long word(long index) {
    return segment.getAtIndex(LAYOUT, index);
  }

  private void setWord(long index, long value) {
    segment.setAtIndex(LAYOUT, index, value);
  }

  // Checked before shifting, so a huge index cannot wrap around into range.
  private long offset(long index) {
    return (Objects.checkIndex(index, length) >>> 6) * Long.BYTES;
  }

  private static long mask(long index) {
    return 1L << (index & 63);
  }

  private static long words(long length) {
    if (length < 0) throw new IllegalArgumentException("negative size: " + length);
    return (length + 63) >>> 6;
  }
}
