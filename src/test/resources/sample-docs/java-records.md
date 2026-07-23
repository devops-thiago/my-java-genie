# Java Records

Records are a special kind of class in Java that act as transparent carriers for immutable data.
They were finalized in Java 16.

A record declaration automatically provides:
- a canonical constructor
- accessors for each component
- `equals`, `hashCode`, and `toString`

Example:

```java
public record Point(int x, int y) {}
```
