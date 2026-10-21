package club.nullbyte3.nativa.map;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/**
 * A map from {@code long} keys to {@code int} values, with room for a fixed number of keys: 12
 * bytes a slot, and about a third of the slots empty. Adding a key to a full map throws. Not thread
 * safe.
 *
 * <pre>{@code
 * try (LongIntMap hits = LongIntMap.allocate(1024)) {
 *   hits.add(playerId, 1);
 *   int n = hits.get(playerId, 0);
 * }
 * }</pre>
 */
public final class LongIntMap extends LongTable {

  private static final ValueLayout.OfInt VALUE = ValueLayout.JAVA_INT;

  /** Receives each key and value from {@link #forEach}. */
  @FunctionalInterface
  public interface Visitor {
    void visit(long key, int value);
  }

  private final long valuesOffset;

  // The key 0 marks an empty slot in the table, so its value lives here.
  private int zeroValue;

  private LongIntMap(Arena arena, int capacity) {
    super(arena, capacity, Integer.BYTES);
    this.valuesOffset = (long) slots() * Long.BYTES;
  }

  /** An empty map that holds up to {@code capacity} keys. */
  public static LongIntMap allocate(int capacity) {
    return new LongIntMap(null, capacity);
  }

  /** As {@link #allocate(int)}, in {@code arena}, which frees it when it closes. */
  public static LongIntMap allocate(Arena arena, int capacity) {
    return new LongIntMap(Objects.requireNonNull(arena), capacity);
  }

  /** Set the value of {@code key}; throws if the key is new and the map is full. */
  public void put(long key, int value) {
    if (key == 0) {
      live();
      setZero(true);
      zeroValue = value;
      return;
    }
    int i = indexOf(key);
    if (i < 0) {
      reserve();
      i = ~i;
      setKey(i, key);
    }
    setValue(i, value);
  }

  /** The value of {@code key}, or {@code missing} if there is none. */
  public int get(long key, int missing) {
    if (key == 0) {
      live();
      return hasZero() ? zeroValue : missing;
    }
    int i = indexOf(key);
    return i < 0 ? missing : valueAt(i);
  }

  public boolean containsKey(long key) {
    if (key == 0) {
      live();
      return hasZero();
    }
    return indexOf(key) >= 0;
  }

  /**
   * Add {@code delta} to the value of {@code key}, taking a missing key as 0, and return the new
   * value. Throws if the key is new and the map is full.
   */
  public int add(long key, int delta) {
    if (key == 0) {
      live();
      if (!hasZero()) {
        setZero(true);
        zeroValue = 0;
      }
      return zeroValue += delta;
    }
    int i = indexOf(key);
    if (i < 0) {
      reserve();
      i = ~i;
      setKey(i, key);
      setValue(i, delta);
      return delta;
    }
    int value = valueAt(i) + delta;
    setValue(i, value);
    return value;
  }

  /** Remove {@code key}; false if it was not there. */
  public boolean remove(long key) {
    if (key == 0) {
      live();
      boolean had = hasZero();
      setZero(false);
      return had;
    }
    int i = indexOf(key);
    if (i < 0) return false;
    removeAt(i);
    return true;
  }

  /** Call {@code visitor} for each entry, in no particular order. Do not change the map inside. */
  public void forEach(Visitor visitor) {
    if (hasZero()) visitor.visit(0, zeroValue);
    for (int i = 0, n = slots(); i < n; i++) {
      long key = keyAt(i);
      if (key != 0) visitor.visit(key, valueAt(i));
    }
  }

  @Override
  void move(int from, int to) {
    setValue(to, valueAt(from));
  }

  private int valueAt(int slot) {
    return segment.get(VALUE, valuesOffset + (long) slot * Integer.BYTES);
  }

  private void setValue(int slot, int value) {
    segment.set(VALUE, valuesOffset + (long) slot * Integer.BYTES, value);
  }
}
