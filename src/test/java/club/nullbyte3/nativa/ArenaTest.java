package club.nullbyte3.nativa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import club.nullbyte3.nativa.array.BooleanArray;
import club.nullbyte3.nativa.array.CharArray;
import club.nullbyte3.nativa.array.IntArray;
import club.nullbyte3.nativa.struct.Struct;
import club.nullbyte3.nativa.struct.StructArray;
import club.nullbyte3.nativa.struct.StructRing;
import java.lang.foreign.Arena;
import org.junit.jupiter.api.Test;

class ArenaTest {

  record Pair(int a, long b) {}

  @Test
  void blocksShareOneArenaAndFreeTogether() {
    Struct pair = Struct.of(Pair.class);
    IntArray ints;
    RawMemory raw;
    BooleanArray bits;
    StructArray structs;
    StructRing ring;
    try (Arena arena = Arena.ofShared()) {
      ints = IntArray.allocate(arena, 4);
      raw = RawMemory.allocate(arena, 16);
      bits = BooleanArray.allocate(arena, 70);
      structs = pair.array(arena, 3);
      ring = pair.ring(arena, 3);
      ints.set(3, 9);
      raw.setLong(8, 5);
      bits.set(69, true);
      structs.setLong(2, pair.field("b"), 6);
      assertEquals(9, ints.get(3));

      ints.close(); // frees nothing: the arena owns it
      assertFalse(ints.isClosed());
      assertEquals(9, ints.get(3));
    }
    for (Memory block : new Memory[] {ints, raw, bits, structs, ring}) {
      assertTrue(block.isClosed());
    }
    assertThrows(IllegalStateException.class, () -> ints.get(0));
  }

  @Test
  void confinedArenaStaysOnItsThread() throws InterruptedException {
    try (Arena arena = Arena.ofConfined()) {
      CharArray chars = CharArray.allocate(arena, 2);
      chars.set(1, 'k');
      Throwable[] failure = {null};
      Thread thread =
          Thread.ofPlatform()
              .start(
                  () -> {
                    try {
                      chars.get(1);
                    } catch (Throwable t) {
                      failure[0] = t;
                    }
                  });
      thread.join();
      assertTrue(failure[0] instanceof WrongThreadException);
      assertEquals('k', chars.get(1));
    }
  }

  @Test
  void nullArenaIsRejected() {
    assertThrows(NullPointerException.class, () -> IntArray.allocate((Arena) null, 1));
  }
}
