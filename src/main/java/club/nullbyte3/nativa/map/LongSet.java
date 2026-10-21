package club.nullbyte3.nativa.map;

import java.lang.foreign.Arena;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * A set of {@code long} values, with room for a fixed number of them: 8 bytes a slot, and about a
 * third of the slots empty. Adding to a full set throws. Not thread safe.
 */
public final class LongSet extends LongTable {

  private LongSet(Arena arena, int capacity) {
    super(arena, capacity, 0);
  }

  /** An empty set that holds up to {@code capacity} values. */
  public static LongSet allocate(int capacity) {
    return new LongSet(null, capacity);
  }

  /** As {@link #allocate(int)}, in {@code arena}, which frees it when it closes. */
  public static LongSet allocate(Arena arena, int capacity) {
    return new LongSet(Objects.requireNonNull(arena), capacity);
  }

  /** Add {@code value}; false if it was already there. Throws if it is new and the set is full. */
  public boolean add(long value) {
    if (value == 0) {
      live();
      boolean added = !hasZero();
      setZero(true);
      return added;
    }
    int i = indexOf(value);
    if (i >= 0) return false;
    reserve();
    setKey(~i, value);
    return true;
  }

  public boolean contains(long value) {
    if (value == 0) {
      live();
      return hasZero();
    }
    return indexOf(value) >= 0;
  }

  /** Remove {@code value}; false if it was not there. */
  public boolean remove(long value) {
    if (value == 0) {
      live();
      boolean had = hasZero();
      setZero(false);
      return had;
    }
    int i = indexOf(value);
    if (i < 0) return false;
    removeAt(i);
    return true;
  }

  /** Call {@code consumer} for each value, in no particular order. Do not change the set inside. */
  public void forEach(LongConsumer consumer) {
    if (hasZero()) consumer.accept(0);
    for (int i = 0, n = slots(); i < n; i++) {
      long key = keyAt(i);
      if (key != 0) consumer.accept(key);
    }
  }

  /** A copy of the values, in no particular order. */
  public long[] toArray() {
    long[] out = new long[size()];
    int[] next = {0};
    forEach(value -> out[next[0]++] = value);
    return out;
  }

  @Override
  void move(int from, int to) {}
}
