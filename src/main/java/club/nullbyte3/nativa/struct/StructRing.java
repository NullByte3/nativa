package club.nullbyte3.nativa.struct;

import java.util.Objects;

/**
 * The last {@code capacity} struct elements pushed, the oldest overwritten first. Index 0 is the
 * newest, 1 the one before, up to {@code size() - 1}. One thread owns a ring: {@link #push()} is
 * not atomic. From {@link Struct#ring}.
 *
 * <pre>{@code
 * moves.push();
 * moves.setDouble(0, Y, y);
 * double dy = moves.getDouble(0, Y) - moves.getDouble(1, Y);
 * }</pre>
 */
public final class StructRing extends StructMemory {

  private final long capacity;
  private final long stride;

  /** Slot of the newest element. */
  private long head;

  private long size;

  StructRing(Struct struct, long capacity) {
    super(struct, positive(capacity));
    this.capacity = capacity;
    this.stride = struct.byteSize();
    this.head = capacity - 1;
  }

  /** Make a zeroed element the newest, dropping the oldest when full. */
  public void push() {
    head = head + 1 == capacity ? 0 : head + 1;
    // The slot may hold an element that just dropped out; nothing of it should show through.
    segment.asSlice(head * stride, stride).fill((byte) 0);
    if (size < capacity) size++;
  }

  /** Elements pushed and still kept, at most {@code capacity()}. */
  public long size() {
    return size;
  }

  public long capacity() {
    return capacity;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public boolean isFull() {
    return size == capacity;
  }

  /** Drop every element and zero the memory. */
  @Override
  public void clear() {
    super.clear();
    head = capacity - 1;
    size = 0;
  }

  // Checked before super allocates, so a bad capacity leaves nothing behind.
  private static long positive(long capacity) {
    if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive: " + capacity);
    return capacity;
  }

  @Override
  long count() {
    return size;
  }

  @Override
  long slot(long back) {
    long slot = head - Objects.checkIndex(back, size);
    return slot < 0 ? slot + capacity : slot;
  }
}
