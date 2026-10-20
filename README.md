# nativa

Store data outside the Java heap. Use primitive arrays, raw bytes, and structs. No boxing, no
dependencies. Needs Java 22 or later.

It uses the Foreign Function & Memory API (`java.lang.foreign`), not `Unsafe`. You free memory
yourself with `close()`. The GC never touches it.

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
    <version>0.1.0</version>
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
```

A struct is built from a record of primitives. The record only describes the fields. You read and
write one field at a time with a `StructField`, so no record object is ever created.

```java
record Move(double x, double y, double z, boolean ground) {}

static final Struct MOVE = Struct.of(Move.class);
static final StructField Y = MOVE.field("y");  // keep fields in static finals

StructArray moves = MOVE.array(1024);          // fixed size, index 0 to 1023
StructRing history = MOVE.ring(20);            // keeps the last 20, index 0 is the newest

history.push();                                // adds an empty slot; the oldest is dropped when full
history.setDouble(0, Y, y);
double dy = history.getDouble(0, Y) - history.getDouble(1, Y);
```

| Package | Classes |
|---|---|
| `club.nullbyte3.nativa` | `RawMemory`, `Memory` |
| `club.nullbyte3.nativa.array` | `ByteArray`, `IntArray`, `LongArray`, `FloatArray`, `DoubleArray` |
| `club.nullbyte3.nativa.struct` | `Struct`, `StructField`, `StructArray`, `StructRing` |

- Always close what you allocate. Use try-with-resources, or close it with whatever owns it.
- A bad index throws. Using something after `close()` also throws.
- Many threads can read and write. Plain `get` and `set` work like normal fields. For a slot that
  threads share, use the volatile and atomic methods. Only one thread should use a ring.
- Structs use C layout: fields in order, each aligned to its size. Using a field of the wrong type, or
  from another struct, throws.

## Building

```sh
./mvnw package          # build and test; a compiler warning fails it
./mvnw spotless:apply   # format the code
```

When you push to `main` with a new version in the pom, it publishes the jar to GitHub Packages. It
also adds a tag `vX.Y.Z` and a release. Change the version by hand.

## License

MIT, see [LICENSE](LICENSE).
