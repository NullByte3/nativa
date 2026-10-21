package club.nullbyte3.nativa;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A block of off-heap memory, zeroed when allocated and freed by {@link #close()}. Any thread may
 * use it; an access after the close throws instead of reading freed memory. Closing twice is fine.
 */
public abstract class Memory implements AutoCloseable {

  // A shared arena per block: any thread can access it, and closing it waits out in-flight reads.
  private final Arena arena;
  private final AtomicBoolean closing = new AtomicBoolean();
  protected final MemorySegment segment;

  protected Memory(long bytes, long alignment) {
    if (bytes < 0) throw new IllegalArgumentException("negative size: " + bytes);
    this.arena = Arena.ofShared();
    this.segment = arena.allocate(bytes, alignment);
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

  /** Whether {@link #close()} has run. Every access after that throws. */
  public final boolean isClosed() {
    return !arena.scope().isAlive();
  }

  /**
   * Free the memory. Closing again does nothing, from any thread; the first close waits for reads
   * and writes in flight.
   */
  @Override
  public final void close() {
    if (closing.compareAndSet(false, true)) arena.close();
  }
}
