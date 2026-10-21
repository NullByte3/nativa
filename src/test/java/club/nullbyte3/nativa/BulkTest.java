package club.nullbyte3.nativa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import club.nullbyte3.nativa.array.BooleanArray;
import club.nullbyte3.nativa.array.IntArray;
import org.junit.jupiter.api.Test;

class BulkTest {

  @Test
  void closeIsIdempotent() {
    IntArray array = IntArray.allocate(4);
    assertFalse(array.isClosed());
    array.close();
    array.close();
    assertTrue(array.isClosed());
    assertThrows(IllegalStateException.class, () -> array.get(0));
  }

  @Test
  void closeFromManyThreadsFreesOnce() throws InterruptedException {
    RawMemory mem = RawMemory.allocate(64);
    Thread[] threads = new Thread[8];
    for (int t = 0; t < threads.length; t++) threads[t] = Thread.ofPlatform().start(mem::close);
    for (Thread thread : threads) thread.join();
    assertTrue(mem.isClosed());
  }

  @Test
  void booleanRangeAndSearch() {
    try (BooleanArray bits = BooleanArray.allocate(200)) {
      assertTrue(bits.isEmpty());
      assertEquals(-1, bits.nextSetBit(0));
      bits.fill(60, 130, true);
      assertEquals(70, bits.cardinality());
      assertEquals(60, bits.nextSetBit(0));
      assertEquals(100, bits.nextSetBit(100));
      assertEquals(-1, bits.nextSetBit(130));
      assertEquals(130, bits.nextClearBit(60));
      assertEquals(0, bits.nextClearBit(0));
      bits.fill(64, 128, false);
      assertEquals(6, bits.cardinality());
      bits.fill(70, 70, true); // empty range
      assertEquals(6, bits.cardinality());
      bits.fill(true);
      assertEquals(-1, bits.nextClearBit(0));
      assertEquals(-1, bits.nextClearBit(199));
      assertEquals(-1, bits.nextSetBit(200));
      bits.not();
      assertTrue(bits.isEmpty());
      bits.not();
      assertEquals(200, bits.cardinality());
      assertThrows(IndexOutOfBoundsException.class, () -> bits.fill(190, 201, true));
      assertThrows(IndexOutOfBoundsException.class, () -> bits.nextSetBit(-1));
    }
  }

  @Test
  void booleanSetOperations() {
    try (BooleanArray a = BooleanArray.of(true, true, false, false);
        BooleanArray b = BooleanArray.of(true, false, true, false);
        BooleanArray other = BooleanArray.allocate(5)) {
      a.and(b);
      assertArrayEquals(new boolean[] {true, false, false, false}, a.toArray());
      a.or(b);
      assertArrayEquals(new boolean[] {true, false, true, false}, a.toArray());
      a.xor(BooleanArray.of(true, true, true, true));
      assertArrayEquals(new boolean[] {false, true, false, true}, a.toArray());
      a.andNot(BooleanArray.of(false, true, false, false));
      assertArrayEquals(new boolean[] {false, false, false, true}, a.toArray());
      a.not();
      assertEquals(3, a.cardinality());
      assertThrows(IllegalArgumentException.class, () -> a.and(other));
    }
  }

  @Test
  void booleanCopiesAndFlip() {
    try (BooleanArray a = BooleanArray.of(true, false, true, true, false);
        BooleanArray b = BooleanArray.allocate(5)) {
      a.copyTo(2, b, 0, 3);
      assertArrayEquals(new boolean[] {true, true, false, false, false}, b.toArray());
      a.copyTo(0, a, 1, 4); // overlapping, moves forward
      assertArrayEquals(new boolean[] {true, true, false, true, true}, a.toArray());
      assertArrayEquals(new boolean[] {false, true}, a.toArray(2, 2));
      assertFalse(a.getAndFlip(2));
      assertTrue(a.get(2));
      assertTrue(a.getAndFlip(2));
      assertFalse(a.getVolatile(2));
      assertThrows(IndexOutOfBoundsException.class, () -> a.copyTo(3, b, 0, 3));
      assertThrows(IndexOutOfBoundsException.class, () -> a.getAndFlip(5));
    }
  }
}
