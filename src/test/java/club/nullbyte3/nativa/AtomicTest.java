package club.nullbyte3.nativa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import club.nullbyte3.nativa.array.ByteArray;
import club.nullbyte3.nativa.array.DoubleArray;
import club.nullbyte3.nativa.array.FloatArray;
import club.nullbyte3.nativa.array.IntArray;
import club.nullbyte3.nativa.array.LongArray;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AtomicTest {

  @Test
  void getAndAddFromManyThreadsLosesNothing() throws InterruptedException {
    try (LongArray counters = LongArray.allocate(2);
        RawMemory mem = RawMemory.allocate(8)) {
      List<Thread> threads = new ArrayList<>();
      for (int t = 0; t < 8; t++) {
        threads.add(
            Thread.ofPlatform()
                .start(
                    () -> {
                      for (int i = 0; i < 100_000; i++) {
                        counters.getAndAdd(1, 1);
                        mem.getAndAddInt(4, 1);
                      }
                    }));
      }
      for (Thread thread : threads) thread.join();
      assertEquals(800_000, counters.getVolatile(1));
      assertEquals(800_000, mem.getIntVolatile(4));
    }
  }

  @Test
  void compareAndSet() {
    try (IntArray ints = IntArray.of(5);
        DoubleArray doubles = DoubleArray.of(1.5)) {
      assertFalse(ints.compareAndSet(0, 4, 9));
      assertTrue(ints.compareAndSet(0, 5, 9));
      assertEquals(9, ints.get(0));
      assertTrue(doubles.compareAndSet(0, 1.5, 2.5));
      doubles.setVolatile(0, 3.5);
      assertEquals(3.5, doubles.get(0));
      assertThrows(IndexOutOfBoundsException.class, () -> ints.getVolatile(1));
    }
  }

  @Test
  void memoryAtomicsNeedAlignment() {
    try (RawMemory mem = RawMemory.allocate(16)) {
      assertTrue(mem.compareAndSetLong(8, 0, 7));
      assertEquals(7, mem.getLong(8));
      assertThrows(IllegalArgumentException.class, () -> mem.getAndAddInt(1, 1));
    }
  }

  @Test
  void smallTypesUpdateOnlyTheirOwnBytes() throws InterruptedException {
    try (RawMemory mem = RawMemory.allocate(10);
        ByteArray bytes = ByteArray.allocate(7)) {
      mem.setShort(0, (short) -2);
      mem.setByte(2, (byte) 100);
      assertEquals((byte) 100, mem.getAndAddByte(2, (byte) 100)); // wraps to -56
      assertEquals((byte) -56, mem.getByte(2));
      assertEquals(-2, mem.getShort(0));
      assertEquals(-2, mem.getAndAddShort(0, (short) 3));
      assertEquals(1, mem.getShortVolatile(0));
      assertTrue(mem.compareAndSetShort(0, (short) 1, (short) -7));
      assertFalse(mem.compareAndSetShort(0, (short) 1, (short) 9));
      assertEquals((byte) -56, mem.getByte(2));
      assertEquals(0, mem.getAndSetChar(8, 'x'));
      assertTrue(mem.compareAndSetChar(8, 'x', 'y'));
      assertFalse(mem.getAndSetBoolean(6, true));
      assertTrue(mem.compareAndSetBoolean(6, true, false));
      assertFalse(mem.getBooleanVolatile(6));
      assertThrows(IllegalArgumentException.class, () -> mem.getAndAddShort(1, (short) 1));
      assertThrows(IndexOutOfBoundsException.class, () -> mem.getAndSetByte(10, (byte) 1));
      assertThrows(IndexOutOfBoundsException.class, () -> bytes.getAndAdd(7, (byte) 1));

      Thread[] threads = new Thread[7];
      for (int t = 0; t < threads.length; t++) {
        int index = t;
        threads[t] =
            Thread.ofPlatform()
                .start(
                    () -> {
                      for (int i = 0; i < 50_000; i++) bytes.getAndAdd(index, (byte) 1);
                    });
      }
      for (Thread thread : threads) thread.join();
      for (int i = 0; i < 7; i++) assertEquals((byte) 50_000, bytes.getVolatile(i));
      assertEquals((byte) 50_000, bytes.getAndSet(3, (byte) 9));
      assertEquals(9, bytes.get(3));
    }
  }

  @Test
  void floatingPointGetAndAddFromManyThreads() throws InterruptedException {
    try (FloatArray floats = FloatArray.allocate(1);
        DoubleArray doubles = DoubleArray.allocate(1)) {
      Thread[] threads = new Thread[4];
      for (int t = 0; t < threads.length; t++) {
        threads[t] =
            Thread.ofPlatform()
                .start(
                    () -> {
                      for (int i = 0; i < 10_000; i++) {
                        floats.getAndAdd(0, 1f);
                        doubles.getAndAdd(0, 0.5);
                      }
                    });
      }
      for (Thread thread : threads) thread.join();
      assertEquals(40_000f, floats.getVolatile(0));
      assertEquals(20_000.0, doubles.getVolatile(0));
      assertEquals(40_000f, floats.getAndSet(0, 1f));
    }
  }
}
