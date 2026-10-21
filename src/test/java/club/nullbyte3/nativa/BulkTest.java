package club.nullbyte3.nativa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import club.nullbyte3.nativa.array.BooleanArray;
import club.nullbyte3.nativa.array.ByteArray;
import club.nullbyte3.nativa.array.CharArray;
import club.nullbyte3.nativa.array.DoubleArray;
import club.nullbyte3.nativa.array.FloatArray;
import club.nullbyte3.nativa.array.IntArray;
import club.nullbyte3.nativa.array.LongArray;
import club.nullbyte3.nativa.array.ShortArray;
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
  void shortAndCharArrays() {
    try (ShortArray shorts = ShortArray.of((short) 1, (short) -2, (short) 3);
        CharArray chars = CharArray.allocate(3)) {
      assertEquals(6, shorts.byteSize());
      assertEquals(-2, shorts.get(1));
      assertEquals(-2, shorts.getAndAdd(1, (short) 5));
      assertEquals(3, shorts.getVolatile(1));
      assertTrue(shorts.compareAndSet(2, (short) 3, (short) 9));
      assertEquals(9, shorts.getAndSet(2, (short) 4));
      assertEquals(1, shorts.get(0));
      assertEquals(Short.MIN_VALUE, shorts.add(0, Short.MAX_VALUE));
      chars.set(1, 'q');
      assertTrue(chars.compareAndSet(1, 'q', 'r'));
      assertEquals('r', chars.getAndSet(1, 's'));
      assertEquals('s', chars.getVolatile(1));
      assertEquals(0, chars.get(0));
      assertThrows(IndexOutOfBoundsException.class, () -> chars.getAndSet(3, 'x'));
      assertThrows(IndexOutOfBoundsException.class, () -> shorts.getVolatile(-1));
    }
  }

  @Test
  void rangeFillAndCopies() {
    try (IntArray a = IntArray.of(1, 2, 3, 4, 5, 6);
        IntArray b = IntArray.allocate(6);
        ByteArray bytes = ByteArray.allocate(6);
        CharArray chars = CharArray.of('a', 'b', 'c');
        DoubleArray doubles = DoubleArray.of(1.5, 2.5, 3.5);
        FloatArray floats = FloatArray.of(1f, 2f);
        LongArray longs = LongArray.of(7, 8, 9)) {
      a.fill(1, 3, 0);
      assertArrayEquals(new int[] {1, 0, 0, 4, 5, 6}, a.toArray());
      a.copyTo(3, b, 0, 3);
      assertArrayEquals(new int[] {4, 5, 6, 0, 0, 0}, b.toArray());
      a.copyTo(0, a, 2, 3); // overlapping, moves forward
      assertArrayEquals(new int[] {1, 0, 1, 0, 0, 6}, a.toArray());
      assertArrayEquals(new int[] {0, 1, 0}, a.toArray(1, 3));
      int[] heap = new int[4];
      b.copyTo(1, heap, 1, 2);
      assertArrayEquals(new int[] {0, 5, 6, 0}, heap);
      b.copyFrom(new int[] {9, 9, 9, 9}, 1, 4, 2);
      assertArrayEquals(new int[] {4, 5, 6, 0, 9, 9}, b.toArray());

      bytes.fill(2, 5, (byte) 7);
      assertArrayEquals(new byte[] {0, 0, 7, 7, 7, 0}, bytes.toArray());
      assertEquals('b', chars.toArray(1, 2)[0]);
      assertEquals(2.5, doubles.toArray(1, 1)[0]);
      assertEquals(2f, floats.toArray(1, 1)[0]);
      longs.copyTo(0, longs, 1, 2);
      assertArrayEquals(new long[] {7, 7, 8}, longs.toArray());

      assertThrows(IndexOutOfBoundsException.class, () -> a.fill(4, 7, 1));
      assertThrows(IndexOutOfBoundsException.class, () -> a.copyTo(5, b, 0, 2));
      assertThrows(IndexOutOfBoundsException.class, () -> a.copyTo(0, b, 5, 2));
      assertThrows(IndexOutOfBoundsException.class, () -> a.copyTo(0, heap, 3, 2));
      assertThrows(IndexOutOfBoundsException.class, () -> a.copyFrom(heap, 2, 0, 3));
      assertThrows(IndexOutOfBoundsException.class, () -> a.toArray(5, 2));
    }
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
