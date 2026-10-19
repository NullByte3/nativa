package club.nullbyte3.nativa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import club.nullbyte3.nativa.array.ByteArray;
import club.nullbyte3.nativa.array.DoubleArray;
import club.nullbyte3.nativa.array.FloatArray;
import club.nullbyte3.nativa.array.IntArray;
import club.nullbyte3.nativa.array.LongArray;
import org.junit.jupiter.api.Test;

class NativaTest {

  @Test
  void memoryReadsBackMixedFields() {
    try (RawMemory mem = RawMemory.allocate(64)) {
      mem.setInt(0, 42);
      mem.setLong(4, Long.MAX_VALUE); // unaligned on purpose
      mem.setDouble(12, 1.5);
      mem.setBoolean(20, true);
      mem.setChar(21, 'z');
      assertEquals(42, mem.getInt(0));
      assertEquals(Long.MAX_VALUE, mem.getLong(4));
      assertEquals(1.5, mem.getDouble(12));
      assertTrue(mem.getBoolean(20));
      assertEquals('z', mem.getChar(21));
      assertEquals(0, mem.getLong(56));
    }
  }

  @Test
  void memoryFillAndCopy() {
    try (RawMemory a = RawMemory.allocate(16);
        RawMemory b = RawMemory.allocate(16)) {
      a.fill(0, 8, (byte) 7);
      a.copyTo(0, b, 8, 8);
      assertEquals(0, b.getByte(7));
      assertEquals(7, b.getByte(8));
      assertEquals(7, b.getByte(15));
    }
  }

  @Test
  void intArrayOperations() {
    try (IntArray array = IntArray.allocate(4)) {
      assertEquals(4, array.length());
      assertEquals(16, array.byteSize());
      array.set(1, 10);
      assertEquals(15, array.add(1, 5));
      array.fill(3);
      assertArrayEquals(new int[] {3, 3, 3, 3}, array.toArray());
      array.clear();
      assertArrayEquals(new int[4], array.toArray());
    }
  }

  @Test
  void typedArraysRoundTrip() {
    try (LongArray l = LongArray.of(1, 2, 3);
        FloatArray f = FloatArray.of(0.5f, 1.5f);
        DoubleArray d = DoubleArray.of(2.5);
        ByteArray b = ByteArray.of((byte) 127)) {
      assertArrayEquals(new long[] {1, 2, 3}, l.toArray());
      assertEquals(2f, f.add(1, 0.5f));
      assertEquals(2.5, d.get(0));
      assertEquals(-128, b.add(0, (byte) 1));
    }
  }

  @Test
  void outOfBoundsAndUseAfterCloseThrow() {
    IntArray array = IntArray.allocate(2);
    assertThrows(IndexOutOfBoundsException.class, () -> array.get(2));
    assertThrows(IndexOutOfBoundsException.class, () -> array.set(-1, 0));
    array.close();
    assertThrows(IllegalStateException.class, () -> array.get(0));
    assertThrows(IllegalArgumentException.class, () -> RawMemory.allocate(-1));
  }
}
