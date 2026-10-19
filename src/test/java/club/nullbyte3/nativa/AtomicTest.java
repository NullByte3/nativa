package club.nullbyte3.nativa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import club.nullbyte3.nativa.array.DoubleArray;
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
}
