package club.nullbyte3.nativa;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A block of off-heap memory, zeroed when allocated and freed by {@link #close()}. Any thread may
 * use it; an access after the close throws instead of reading freed memory. Closing twice is fine.
 */
public abstract class Memory implements AutoCloseable {

  // A shared arena per block: any thread can access it, and closing it waits out in-flight reads.
  private final Arena arena;
  private final boolean owner;
  private final AtomicBoolean closing = new AtomicBoolean();
  protected final MemorySegment segment;

  // The block padded to whole ints, so a byte or short can be updated through the int holding it.
  private final MemorySegment backing;

  private static final VarHandle WORD = ValueLayout.JAVA_INT.varHandle();
  private static final boolean BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

  protected Memory(long bytes, long alignment) {
    this(null, bytes, alignment);
  }

  /** Allocate in {@code shared}, or in an arena of its own when that is null. */
  protected Memory(Arena shared, long bytes, long alignment) {
    if (bytes < 0) throw new IllegalArgumentException("negative size: " + bytes);
    this.owner = shared == null;
    this.arena = owner ? Arena.ofShared() : shared;
    try {
      this.backing = arena.allocate(Math.addExact(bytes, 3) & ~3L, Math.max(alignment, Long.BYTES));
    } catch (Throwable t) {
      if (owner) arena.close(); // nothing else holds it, so do not leak it
      throw t;
    }
    this.segment = backing.asSlice(0, bytes);
  }

  public final long byteSize() {
    return segment.byteSize();
  }

  /** Native address of the first byte, for handing to native code. */
  public final long address() {
    return segment.address();
  }

  /** The segment underneath, for anything this API does not cover. */
  public final MemorySegment segment() {
    return segment;
  }

  /** Zero every byte. */
  public void clear() {
    segment.fill((byte) 0);
  }

  /** Whether the memory is freed: {@link #close()} ran, or the arena it lives in was closed. */
  public final boolean isClosed() {
    return !arena.scope().isAlive();
  }

  /**
   * Free the memory. Closing again does nothing, from any thread; the first close waits for reads
   * and writes in flight. Memory allocated in an arena you passed in is not freed here but when
   * that arena closes, so for it this does nothing.
   */
  @Override
  public final void close() {
    if (owner && closing.compareAndSet(false, true)) arena.close();
  }

  /**
   * Atomically set the {@code size}-byte value (1 or 2) at {@code offset} to {@code update} if it
   * holds {@code expected}. Java has no such access mode, so the int holding the value is updated.
   */
  protected final boolean compareAndSetSmall(long offset, int size, int expected, int update) {
    long word = wordOffset(offset, size);
    int shift = shift(offset, size);
    int mask = mask(size, shift);
    int from = (expected << shift) & mask;
    int to = (update << shift) & mask;
    while (true) {
      int bits = (int) WORD.getVolatile(backing, word);
      if ((bits & mask) != from) return false;
      if (WORD.compareAndSet(backing, word, bits, (bits & ~mask) | to)) return true;
    }
  }

  /** Atomically set the small value and return what it held before, zero-extended. */
  protected final int getAndSetSmall(long offset, int size, int value) {
    return updateSmall(offset, size, value, false);
  }

  /** Atomically add {@code delta} to the small value and return what it held before. */
  protected final int getAndAddSmall(long offset, int size, int delta) {
    return updateSmall(offset, size, delta, true);
  }

  private int updateSmall(long offset, int size, int operand, boolean add) {
    long word = wordOffset(offset, size);
    int shift = shift(offset, size);
    int mask = mask(size, shift);
    while (true) {
      int bits = (int) WORD.getVolatile(backing, word);
      int before = (bits & mask) >>> shift;
      int after = ((add ? before + operand : operand) << shift) & mask;
      if (WORD.compareAndSet(backing, word, bits, (bits & ~mask) | after)) return before;
    }
  }

  // Atomics need an aligned offset, the same rule as for int and long.
  private long wordOffset(long offset, int size) {
    Objects.checkFromIndexSize(offset, size, segment.byteSize());
    if ((offset & (size - 1)) != 0) {
      throw new IllegalArgumentException("offset " + offset + " is not " + size + "-aligned");
    }
    return offset & ~3L;
  }

  private static int shift(long offset, int size) {
    int within = (int) (offset & 3);
    return (BIG_ENDIAN ? 4 - size - within : within) * 8;
  }

  private static int mask(int size, int shift) {
    return ((1 << (size * 8)) - 1) << shift;
  }
}
