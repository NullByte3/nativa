package club.nullbyte3.nativa;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
