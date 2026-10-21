package club.nullbyte3.nativa.struct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StructTest {

  record Move(byte flags, double x, int ticks, boolean ground, float yaw) {}

  record Boxed(Integer value) {}

  static final Struct MOVE = Struct.of(Move.class);
  static final StructField FLAGS = MOVE.field("flags");
  static final StructField X = MOVE.field("x");
  static final StructField TICKS = MOVE.field("ticks");
  static final StructField GROUND = MOVE.field("ground");
  static final StructField YAW = MOVE.field("yaw");

  @Test
  void naturalAlignmentLayout() {
    assertEquals(0, FLAGS.offset());
    assertEquals(8, X.offset());
    assertEquals(16, TICKS.offset());
    assertEquals(20, GROUND.offset());
    assertEquals(24, YAW.offset());
    assertEquals(32, MOVE.byteSize());
  }

  @Test
  void fieldsReadBackPerElement() {
    try (StructArray moves = MOVE.array(3)) {
      moves.setByte(1, FLAGS, (byte) 3);
      moves.setDouble(1, X, 12.5);
      moves.setInt(1, TICKS, 40);
      moves.setBoolean(1, GROUND, true);
      moves.setFloat(1, YAW, 90f);
      moves.copy(1, 2);
      assertEquals(12.5, moves.getDouble(2, X));
      assertEquals(40, moves.getInt(2, TICKS));
      assertTrue(moves.getBoolean(2, GROUND));
      assertEquals(90f, moves.getFloat(2, YAW));
      assertEquals(0, moves.getInt(0, TICKS));
      assertEquals(96, moves.byteSize());
    }
  }

  @Test
  void misuseThrows() {
    try (StructArray moves = MOVE.array(1)) {
      assertThrows(IllegalArgumentException.class, () -> moves.getInt(0, X));
      assertThrows(IndexOutOfBoundsException.class, () -> moves.getDouble(1, X));
      Struct other = Struct.of(Move.class);
      assertThrows(IllegalArgumentException.class, () -> moves.getDouble(0, other.field("x")));
    }
    assertThrows(IllegalArgumentException.class, () -> MOVE.field("y"));
    assertThrows(IllegalArgumentException.class, () -> Struct.of(Boxed.class));
  }

  @Test
  void ringKeepsTheLastElements() {
    try (StructRing ring = MOVE.ring(3)) {
      assertTrue(ring.isEmpty());
      for (int i = 1; i <= 5; i++) {
        ring.push();
        ring.setInt(0, TICKS, i);
      }
      assertTrue(ring.isFull());
      assertEquals(3, ring.size());
      assertEquals(5, ring.getInt(0, TICKS));
      assertEquals(4, ring.getInt(1, TICKS));
      assertEquals(3, ring.getInt(2, TICKS));
      assertThrows(IndexOutOfBoundsException.class, () -> ring.getInt(3, TICKS));
      ring.push();
      assertEquals(0, ring.getInt(0, TICKS)); // pushed slot is zeroed, not the old 3
      ring.clear();
      assertEquals(0, ring.size());
      assertThrows(IndexOutOfBoundsException.class, () -> ring.getInt(0, TICKS));
    }
    assertThrows(IllegalArgumentException.class, () -> MOVE.ring(0));
  }

  @Test
  void fillSetsTheFieldOfEveryElement() {
    try (StructArray moves = MOVE.array(5);
        StructRing ring = MOVE.ring(4)) {
      moves.fillInt(TICKS, 7);
      moves.fillBoolean(GROUND, true);
      for (int i = 0; i < 5; i++) {
        assertEquals(7, moves.getInt(i, TICKS));
        assertTrue(moves.getBoolean(i, GROUND));
        assertEquals(0, moves.getDouble(i, X));
      }
      ring.fillDouble(X, 1); // empty ring: nothing to fill
      ring.push();
      ring.push();
      ring.fillDouble(X, 2.5);
      assertEquals(2.5, ring.getDouble(1, X));
      assertThrows(IllegalArgumentException.class, () -> moves.fillDouble(TICKS, 1));
      assertThrows(IllegalArgumentException.class, () -> ring.fillFloat(TICKS, 1f));
    }
  }

  @Test
  void fieldAtomics() throws InterruptedException {
    try (StructArray moves = MOVE.array(2)) {
      Thread[] threads = new Thread[4];
      for (int t = 0; t < threads.length; t++) {
        threads[t] =
            Thread.ofPlatform()
                .start(
                    () -> {
                      for (int i = 0; i < 25_000; i++) {
                        moves.getAndAddInt(1, TICKS, 1);
                        moves.getAndAddByte(1, FLAGS, (byte) 1);
                        moves.getAndAddFloat(1, YAW, 1f);
                      }
                    });
      }
      for (Thread thread : threads) thread.join();
      assertEquals(100_000, moves.getIntVolatile(1, TICKS));
      assertEquals((byte) 100_000, moves.getByteVolatile(1, FLAGS));
      assertEquals(100_000f, moves.getFloatVolatile(1, YAW));
      assertEquals(0, moves.getInt(0, TICKS));

      assertFalse(moves.getAndSetBoolean(0, GROUND, true));
      assertTrue(moves.compareAndSetBoolean(0, GROUND, true, false));
      assertTrue(moves.compareAndSetDouble(0, X, 0, 4.5));
      assertEquals(4.5, moves.getAndAddDouble(0, X, 1));
      assertEquals(5.5, moves.getDoubleVolatile(0, X));
      assertThrows(IllegalArgumentException.class, () -> moves.getAndAddLong(0, TICKS, 1));
      assertThrows(IndexOutOfBoundsException.class, () -> moves.getAndAddInt(2, TICKS, 1));
    }
  }
}
