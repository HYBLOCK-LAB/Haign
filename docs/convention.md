# Coding Conventions

##  Java Coding Convention

This document outlines coding standards and best practices for Java development.

---

### 1. File & Class Naming

- Use `CamelCase` for class names: `WalletApplet`, `KeyManager`
- File name must match the public class name
- Avoid deeply nested packages (use 1~2 levels max)

### 2. Method Naming

- Use `camelCase` for methods: `processCommand`, `generateKey`
- Prefix boolean methods with `is`, `has`, or `can`: `isInitialized()`

### 3. Constant Naming

- Use `UPPER_CASE_WITH_UNDERSCORES`: `MAX_KEYS`, `SW_FILE_FULL`

### 4. Variable Naming

- Short but descriptive: `keyIndex`, `bufferOffset`
- Avoid single-letter names except for loop counters (`i`, `j`)

### 5. Code Structure & Indentation

- 4-space indentation
- Group related methods together
- Keep methods short (< 40 lines if possible)

## 6. Comments

- Use JavaDoc-style comments for public methods
- Inline comments for tricky logic
```java
// Copy UUID to EEPROM
Util.arrayCopyNonAtomic(src, offset, dest, 0, UUID_LENGTH);
```
- Use `TODO`: to mark incomplete or to-be-improved code.
  - Format: `// TODO: [short explanation of what needs to be done]`
```java
// TODO(js, 2025-07-13): Optimize memory usage for key storage
```

##  Kotlin  Coding Convention

TODO
##  Swift Coding Convention

TODO