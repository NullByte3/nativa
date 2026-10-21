# nativa

Store data outside the Java heap. Use primitive arrays, raw bytes, and structs. No boxing, no
dependencies. Needs Java 25 or later.

It uses the Foreign Function & Memory API (`java.lang.foreign`), not `Unsafe`. You free memory
yourself with `close()`. The GC never touches it.

This is just a wrapper around MemorySegment, and you should probably not use it. I would use something like [Agrona](https://github.com/aeron-io/agrona/tree/master) or [Netty](https://github.com/netty/netty) for networking.
I made this to improve a few addons aka "Plugins" in Minecraft for my own sake.

## Install

Get the jar from the [releases page](https://github.com/NullByte3/nativa/releases). It is also on
GitHub Packages. GitHub Packages needs a token with `read:packages`, even to download. Put the token
in `~/.m2/settings.xml` as server `github`.

```xml
<repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/NullByte3/nativa</url>
</repository>

<dependency>
    <groupId>club.nullbyte3</groupId>
    <artifactId>nativa</artifactId>
    <version>0.1.1</version>
</dependency>
```

## Usage

```java
try (IntArray hits = IntArray.allocate(1024)) {
    hits.set(0, 5);
    hits.add(0, 1);                            // now 6
    hits.getAndAdd(0, 1);                      // atomic, returns 6
}

try (RawMemory mem = RawMemory.allocate(32)) {  // raw bytes
    mem.setDouble(0, 1.5);
    mem.getAndAddLong(24, 1);                  // atomics need an aligned offset
}

try (BooleanArray seen = BooleanArray.allocate(1_000_000)) {  // 1 bit per entry, 125 KB
    seen.set(42, true);
    seen.getAndSet(7, true);                   // atomic, only touches that one bit
    long next = seen.nextSetBit(0);            // 7, or -1 if none
}
```

Bulk copies work on every array. The ranges are checked, and may overlap, even inside one array.

```java
try (IntArray a = IntArray.allocate(1024); IntArray b = IntArray.allocate(1024)) {
    a.fill(0, 100, -1);                        // from, to (not included)
    a.copyTo(0, b, 512, 100);                  // index, target, target index, length
    a.copyFrom(new int[] {1, 2, 3}, 0, 10, 3); // heap array, its start, index, length
    int[] head = a.toArray(0, 16);
}
```

Maps and sets of `long` keys have a fixed capacity: adding a key to a full one throws. They are not
thread safe.

```java
try (LongIntMap hits = LongIntMap.allocate(1024); LongSet seen = LongSet.allocate(1024)) {
    hits.add(playerId, 1);                     // missing counts as 0; returns the new value
    int n = hits.get(playerId, 0);             // 0 if there is none
    seen.add(playerId);                        // false if it was already there
}
```

A struct is built from a record of primitives. The record only describes the fields. You read and
write one field at a time with a `StructField`, so no record object is ever created.

```java
record Move(double x, double y, double z, boolean ground) {}

static final Struct MOVE = Struct.of(Move.class);
static final StructField Y = MOVE.field("y");  // keep fields in static finals

StructArray moves = MOVE.array(1024);          // fixed size, index 0 to 1023
moves.fillBoolean(GROUND, true);               // one field of every element
StructRing history = MOVE.ring(20);            // keeps the last 20, index 0 is the newest

history.push();                                // adds an empty slot; the oldest is dropped when full
history.setDouble(0, Y, y);
double dy = history.getDouble(0, Y) - history.getDouble(1, Y);
```

| Package | Classes |
|---|---|
| `club.nullbyte3.nativa` | `RawMemory`, `Memory` |
| `club.nullbyte3.nativa.array` | `BooleanArray` (1 bit each), `ByteArray`, `ShortArray`, `CharArray`, `IntArray`, `LongArray`, `FloatArray`, `DoubleArray` |
| `club.nullbyte3.nativa.map` | `LongIntMap`, `LongSet` |
| `club.nullbyte3.nativa.struct` | `Struct`, `StructField`, `StructArray`, `StructRing` |

- Always close what you allocate. Use try-with-resources, or close it with whatever owns it.
  `close()` is safe to call twice, from any thread; `isClosed()` tells you if it ran.
- A bad index throws. Using something after `close()` also throws.
- Structs use C layout: fields in order, each aligned to its size. Using a field of the wrong type, or
  from another struct, throws.
- The module is `club.nullbyte3.nativa`.

### Arenas

Every block gets an arena of its own, and closing it makes the JVM sync with every thread that runs
Java code before it returns. That is cheap once, but not for many short-lived blocks on a busy
server. Give them one arena instead and close that once. Every `allocate` and `Struct.array/ring`
has an overload that takes an `Arena`.

```java
try (Arena arena = Arena.ofShared()) {
    IntArray ids = IntArray.allocate(arena, 256);
    LongIntMap hits = LongIntMap.allocate(arena, 256);
    ...
}   // frees both; ids.close() and hits.close() do nothing
```

The arena decides who may touch a block: `Arena.ofConfined()` skips the sync for good but allows one
thread only, `Arena.ofShared()` allows any thread. Pass the arena you would pass to the JDK.

### Threads

Many threads can read and write. Plain `get` and `set` work like normal fields. For a slot that
threads share, use the volatile and atomic methods. Only one thread should use a ring.

| Type | Atomic methods |
|---|---|
| `int`, `long` | `getVolatile`, `setVolatile`, `compareAndSet`, `getAndSet`, `getAndAdd` |
| `float`, `double` | the same; `compareAndSet` compares bits, and `getAndAdd` retries until it wins |
| `byte`, `short` | the same |
| `char`, `boolean` | the same, without `getAndAdd` |
| `BooleanArray` | `getVolatile`, `setVolatile`, `compareAndSet`, `getAndSet`, `getAndFlip` |

- On `RawMemory` these are named by type: `getIntVolatile`, `compareAndSetByte`, `getAndAddDouble`,
  and so on. On a struct they take a field: `moves.getAndAddInt(0, TICKS, 1)`.
- An atomic needs an offset that is a multiple of the value size, or it throws. Struct fields and
  array elements are always aligned.
- Java has no atomic update for `byte`, `short` and `char`, so those go through the `int` that
  holds them. Do not mix them with plain writes to the same slot from another thread.
- `add` and `flip` are a read then a write. They are not atomic.
- Changing one entry of a `BooleanArray` with `set` is also a read then a write of its 64-bit word.
  Use `setVolatile` when threads share an array.
- `and`, `or`, `xor`, `andNot`, `not`, `fill` and the copies are not atomic either.

## Building

```sh
./mvnw package          # build and test; a compiler warning fails it
./mvnw spotless:apply   # format the code
```

When you push to `main` with a new version in the pom, it publishes the jar to GitHub Packages. It
also adds a tag `vX.Y.Z` and a release. Change the version by hand.

## License

MIT, see [LICENSE](LICENSE).
