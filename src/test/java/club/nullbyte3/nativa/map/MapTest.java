package club.nullbyte3.nativa.map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MapTest {

  @Test
  void mapBasics() {
    try (LongIntMap map = LongIntMap.allocate(8)) {
      assertTrue(map.isEmpty());
      assertEquals(-1, map.get(5, -1));
      map.put(5, 50);
      map.put(0, 7); // the key that marks empty slots
      map.put(-3, 30);
      assertEquals(3, map.size());
      assertEquals(50, map.get(5, -1));
      assertEquals(7, map.get(0, -1));
      assertEquals(30, map.get(-3, -1));
      assertTrue(map.containsKey(0));
      assertEquals(51, map.add(5, 1));
      assertEquals(4, map.add(99, 4)); // new key counts from 0
      assertEquals(-2, map.add(0, -9));
      assertTrue(map.remove(0));
      assertFalse(map.remove(0));
      assertFalse(map.containsKey(0));
      assertEquals(3, map.size());
      long[] keys = {0, 0}; // sum of keys, number of entries
      map.forEach(
          (k, v) -> {
            keys[0] += k;
            keys[1]++;
          });
      assertEquals(3, keys[1]);
      assertEquals(5 - 3 + 99, keys[0]);
      map.clear();
      assertEquals(0, map.size());
      assertEquals(-1, map.get(5, -1));
      assertFalse(map.containsKey(99));
    }
  }

  @Test
  void fullMapRejectsNewKeysButKeepsOldOnes() {
    try (LongIntMap map = LongIntMap.allocate(3);
        LongSet set = LongSet.allocate(2)) {
      map.put(1, 1);
      map.put(2, 2);
      map.put(0, 3);
      assertThrows(IllegalStateException.class, () -> map.put(4, 4));
      assertThrows(IllegalStateException.class, () -> map.add(4, 4));
      map.put(2, 20); // existing keys still work
      map.add(0, 1);
      assertEquals(20, map.get(2, 0));
      assertEquals(4, map.get(0, 0));
      assertTrue(map.remove(1));
      map.put(4, 4);

      set.add(0);
      set.add(8);
      assertThrows(IllegalStateException.class, () -> set.add(9));
      assertFalse(set.add(8));
      assertThrows(IllegalArgumentException.class, () -> LongSet.allocate(-1));
      try (LongSet empty = LongSet.allocate(0)) {
        assertFalse(empty.contains(1));
        assertThrows(IllegalStateException.class, () -> empty.add(1));
      }
    }
  }

  @Test
  void mapAgreesWithHashMapUnderChurn() {
    Random random = new Random(7);
    try (LongIntMap map = LongIntMap.allocate(200)) {
      Map<Long, Integer> expected = new HashMap<>();
      for (int step = 0; step < 200_000; step++) {
        // Few distinct keys, many multiples of a power of two: lots of collisions and removals.
        long key = (random.nextInt(260) - 10) * 4096L;
        switch (random.nextInt(4)) {
          case 0 -> {
            int value = random.nextInt();
            if (expected.containsKey(key) || expected.size() < 200) {
              map.put(key, value);
              expected.put(key, value);
            }
          }
          case 1 -> {
            if (expected.containsKey(key) || expected.size() < 200) {
              assertEquals(expected.merge(key, 3, Integer::sum), map.add(key, 3));
            }
          }
          case 2 -> assertEquals(expected.remove(key) != null, map.remove(key));
          default -> assertEquals(expected.getOrDefault(key, -1), map.get(key, -1));
        }
        assertEquals(expected.size(), map.size());
      }
      int[] seen = {0};
      map.forEach(
          (k, v) -> {
            assertEquals(expected.get(k), v);
            seen[0]++;
          });
      assertEquals(expected.size(), seen[0]);
    }
  }

  @Test
  void setAgreesWithHashSetUnderChurn() {
    Random random = new Random(11);
    try (LongSet set = LongSet.allocate(100)) {
      Set<Long> expected = new HashSet<>();
      for (int step = 0; step < 200_000; step++) {
        long value = (random.nextInt(130) - 5) * 1024L;
        switch (random.nextInt(3)) {
          case 0 -> {
            if (expected.contains(value) || expected.size() < 100) {
              assertEquals(expected.add(value), set.add(value));
            }
          }
          case 1 -> assertEquals(expected.remove(value), set.remove(value));
          default -> assertEquals(expected.contains(value), set.contains(value));
        }
        assertEquals(expected.size(), set.size());
      }
      long[] all = set.toArray();
      java.util.Arrays.sort(all);
      assertArrayEquals(expected.stream().mapToLong(Long::longValue).sorted().toArray(), all);
    }
  }

  @Test
  void useAfterCloseThrowsEvenForZero() {
    LongIntMap map = LongIntMap.allocate(4);
    LongSet set = LongSet.allocate(4);
    map.close();
    set.close();
    assertThrows(IllegalStateException.class, () -> map.get(1, 0));
    assertThrows(IllegalStateException.class, () -> map.get(0, 0));
    assertThrows(IllegalStateException.class, () -> set.contains(0));
    assertThrows(IllegalStateException.class, () -> set.add(2));
  }
}
