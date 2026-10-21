package club.nullbyte3.nativa.map;

import club.nullbyte3.nativa.Memory;
import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;

/**
 * Open addressing with linear probing over a power-of-two table, keys first and then whatever the
 * subclass stores per slot. A key of 0 marks an empty slot, so the key 0 itself is kept on the heap
 * by the subclass.
 */
abstract sealed class LongTable extends Memory permits LongIntMap, LongSet {

  private static final ValueLayout.OfLong KEY = ValueLayout.JAVA_LONG;

  // Multiplying by 2^64 / phi spreads even keys that are all multiples of each other.
  private static final long PHI = 0x9E3779B97F4A7C15L;

  private final int capacity;
  private final int mask;
  private final int shift;
  private int size;
  private boolean hasZero;

  LongTable(Arena arena, int capacity, int valueBytes) {
    super(arena, (long) tableSize(capacity) * (Long.BYTES + valueBytes), Long.BYTES);
    this.capacity = capacity;
    this.mask = tableSize(capacity) - 1;
    this.shift = 64 - Integer.numberOfTrailingZeros(mask + 1);
  }

  /** How many keys fit; adding another throws. */
  public final int capacity() {
    return capacity;
  }

  public final int size() {
    return size;
  }

  public final boolean isEmpty() {
    return size == 0;
  }

  /** Remove everything. */
  @Override
  public void clear() {
    super.clear();
    size = 0;
    hasZero = false;
  }

  /** Slots in the table: at least {@code capacity} plus an always-empty one, so probing ends. */
  private static int tableSize(int capacity) {
    if (capacity < 0) throw new IllegalArgumentException("negative capacity: " + capacity);
    long need = (long) capacity * 4 / 3 + 2;
    if (need > 1 << 30) throw new IllegalArgumentException("capacity too large: " + capacity);
    return Math.max(4, (int) (Long.highestOneBit(need - 1) << 1));
  }

  final int slots() {
    return mask + 1;
  }

  final boolean hasZero() {
    return hasZero;
  }

  /** The key 0 became present or absent. Counts it against the capacity. */
  final void setZero(boolean present) {
    if (present == hasZero) return;
    if (present) reserve();
    else size--;
    hasZero = present;
  }

  /** Count one more key, or throw if the table is at capacity. */
  final void reserve() {
    if (size == capacity) throw new IllegalStateException("full: " + capacity + " keys");
    size++;
  }

  /** The key 0 is not in the table, so it has to check that the memory is still there. */
  final void live() {
    if (isClosed()) throw new IllegalStateException("Already closed");
  }

  final long keyAt(int slot) {
    return segment.getAtIndex(KEY, slot);
  }

  final void setKey(int slot, long key) {
    segment.setAtIndex(KEY, slot, key);
  }

  /** Where a non-zero key lives: its slot, or {@code ~slot} for the empty slot it would take. */
  final int indexOf(long key) {
    int i = slotOf(key);
    while (true) {
      long found = keyAt(i);
      if (found == key) return i;
      if (found == 0) return ~i;
      i = (i + 1) & mask;
    }
  }

  /** Move whatever the subclass keeps for the slot {@code from} into {@code to}. */
  abstract void move(int from, int to);

  /** Remove the key in {@code slot} and close the gap, so no tombstones are needed. */
  final void removeAt(int slot) {
    int i = slot;
    int j = i;
    while (true) {
      j = (j + 1) & mask;
      long key = keyAt(j);
      if (key == 0) break;
      int home = slotOf(key);
      // It may move back into the gap only if its home is not between the gap and where it is.
      boolean stays = i <= j ? i < home && home <= j : i < home || home <= j;
      if (stays) continue;
      setKey(i, key);
      move(j, i);
      i = j;
    }
    setKey(i, 0);
    size--;
  }

  private int slotOf(long key) {
    return (int) ((key * PHI) >>> shift);
  }
}
