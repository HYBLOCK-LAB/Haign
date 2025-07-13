# Java Card Coding Considerations

This document outlines key differences and caveats developers should be aware of when transitioning from standard Java to Java Card.

## Abstraction
| Topic                        | Java                         | Java Card                                | Notes |
|-----------------------------|------------------------------|------------------------------------------|-------|
| Memory Management           | Automatic (Garbage Collected)| Manual; no GC; EEPROM/RAM distinction     | Use `JCSystem.makeTransient...` for RAM |
| Data Types                  | `int`, `long`, `float` etc.  | Only `byte`, `short`                     | Use fixed-size arrays for structures |
| Object Allocation           | Allowed at any time          | Only at install-time (for persistent objects) | No dynamic allocation during runtime |
| Exception Handling          | Extensive use acceptable     | Try to minimize; expensive on smartcards | Avoid try-catch inside loops |
| Standard Libraries          | Full Java SE                 | Only Java Card API (subset)              | No `java.util`, `java.io`, etc. |
| Class Size Limit            | No limit                     | CAP file class size limit (e.g. 64KB)    | Split logic if needed |
| String Handling             | `String` supported           | No native `String` class                 | Use byte arrays instead |
| Logging / Debugging         | System.out/Logging available | No console output or logging             | Use status words or APDU responses for debugging |
| File I/O                    | Available                    | Not supported                            | No filesystem on card |
| Static Initialization       | Commonly used                | Must be carefully handled to avoid EEPROM wear | Avoid frequent writes |
| Threads / Concurrency       | Supported                    | Not supported                            | Single-threaded execution only |
| API Usage                   | Flexible                     | Use `Util`, `ISO7816`, `JCSystem`, etc.  | Rely on provided helper classes |