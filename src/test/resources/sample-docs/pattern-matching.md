# Pattern Matching for switch

Pattern matching for `switch` allows testing a selector against multiple patterns.
Primitive types in patterns for `instanceof` and `switch` further extend this feature.

Example:

```java
static String describe(Object obj) {
    return switch (obj) {
        case Integer i -> "int " + i;
        case String s -> "string " + s;
        case null -> "null";
        default -> "other";
    };
}
```
