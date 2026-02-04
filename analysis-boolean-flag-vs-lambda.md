# Analysis: Boolean Flag vs Lambda for Stream Closing

## Question 1
Would it be easier to add a boolean flag to streams/readers instead of passing a lambda to verify the state?

## Question 2
Do we need wrappers at all? Can we add the closed flag directly to NonBlockingReader/NonBlockingInputStream?

## Analysis

Looking at the current PR branch (`feat-1575-strict-close-v4`) and the related branches, I can see there are different approaches being considered for issue #1575:

1. **The `fix-1575-terminal-close` branch** (commit `cb505c78`) adds a `closed` boolean flag to `AbstractTerminal` and calls `checkClosed()` throughout the codebase
2. **The current approach** in `PosixPtyTerminal` already uses a wrapper class (`InputStreamWrapper`) with a boolean flag

## Answer to Question 1

**Yes, using a boolean flag would likely be simpler and cleaner than passing lambdas.** Here's why:

### Current Pattern (Already in Use)

Looking at `PosixPtyTerminal.java`, there's already an `InputStreamWrapper` that uses this exact pattern:

```java
private static class InputStreamWrapper extends NonBlockingInputStream {
    private final NonBlockingInputStream in;
    private volatile boolean closed;
    
    @Override
    public int read(long timeout, boolean isPeek) throws IOException {
        if (closed) {
            throw new ClosedException();
        }
        return in.read(timeout, isPeek);
    }
    
    @Override
    public void close() throws IOException {
        closed = true;
    }
}
```

Location: `terminal/src/main/java/org/jline/terminal/impl/PosixPtyTerminal.java` lines 214-235

### Advantages of Boolean Flag Approach:

1. **Simpler and more direct** - No need to pass lambdas or `BooleanSupplier` around
2. **Better performance** - Direct field access vs lambda invocation
3. **Easier to understand** - Clear ownership of the closed state
4. **Already established pattern** - The `InputStreamWrapper` class already uses this approach
5. **Thread-safe with `volatile`** - Simple visibility guarantees
6. **Less coupling** - Wrapper doesn't need a reference to the terminal or external state

### Recommended Approach:

Instead of wrapping streams with lambdas, you could:

1. Add a `volatile boolean closed` field to the wrapper classes (like `InputStreamWrapper` already has)
2. Provide a method to set the closed state from the owning terminal
3. Check the flag at the beginning of each I/O operation

This is exactly what the `fix-1575-terminal-close` branch does at the `AbstractTerminal` level with the `checkClosed()` method, and it's cleaner than passing lambdas around.

### Example Implementation Pattern:

```java
// In the wrapper class
private static class ReaderWrapper extends NonBlockingReader {
    private final NonBlockingReader reader;
    private volatile boolean closed;
    
    protected ReaderWrapper(NonBlockingReader reader) {
        this.reader = reader;
    }
    
    @Override
    public int read(long timeout, boolean isPeek) throws IOException {
        if (closed) {
            throw new ClosedException();
        }
        return reader.read(timeout, isPeek);
    }
    
    @Override
    public void close() throws IOException {
        closed = true;
    }
}

// In the terminal class
private final InputStreamWrapper inputWrapper;
private final ReaderWrapper readerWrapper;

@Override
protected void doClose() throws IOException {
    super.doClose();
    inputWrapper.close();  // Sets the closed flag
    readerWrapper.close(); // Sets the closed flag
}
```

## Answer to Question 2

**YES! You can add the closed flag directly to the base classes and eliminate wrappers entirely!**

This is actually a much better approach. Here's why:

### Key Insight: Each Terminal Creates Its Own Instances

Looking at the code:
- `PosixPtyTerminal` creates: `NonBlocking.nonBlocking(name, pty.getSlaveInput())` - creates a NEW `NonBlockingInputStreamImpl`
- `PosixSysTerminal` creates: `NonBlocking.nonBlocking(getName(), pty.getSlaveInput())` - creates a NEW `NonBlockingInputStreamImpl`
- `LineDisciplineTerminal` creates: `NonBlocking.nonBlockingPumpInputStream()` - creates a NEW `NonBlockingPumpInputStream`

**These instances are NOT shared between terminals!** Each terminal gets its own instances.

### Why Wrappers Are Unnecessary

Since each terminal creates its own NonBlockingReader/NonBlockingInputStream instances:
1. You can add a `volatile boolean closed` field directly to the base classes
2. Each instance will have its own closed state
3. No need for wrapper classes at all!

### Recommended Implementation

Add to `NonBlockingReader`:
```java
public abstract class NonBlockingReader extends Reader {
    protected volatile boolean closed = false;

    protected void checkClosed() throws IOException {
        if (closed) {
            throw new ClosedException();
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }
}
```

Add to `NonBlockingInputStream`:
```java
public abstract class NonBlockingInputStream extends InputStream {
    protected volatile boolean closed = false;

    protected void checkClosed() throws IOException {
        if (closed) {
            throw new ClosedException();
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }
}
```

Then in concrete implementations like `NonBlockingReaderImpl`, `NonBlockingInputStreamImpl`, etc., just call `checkClosed()` at the start of read operations:

```java
@Override
public int read(long timeout, boolean isPeek) throws IOException {
    checkClosed();
    // ... existing implementation
}
```

### Advantages of This Approach

1. **No wrappers needed** - Simpler architecture
2. **Less object allocation** - Better performance
3. **Cleaner code** - No wrapper boilerplate
4. **Consistent pattern** - All NonBlocking* classes work the same way
5. **Easy to implement** - Just add the field and check to base classes
6. **Thread-safe** - Using volatile ensures visibility across threads

### What About NonBlockingPumpReader/NonBlockingPumpInputStream?

These already have their own `closed` field! Look at `NonBlockingPumpInputStream`:
```java
public class NonBlockingPumpInputStream extends NonBlockingInputStream {
    private boolean closed;

    @Override
    protected int read(long timeout, boolean isPeek) throws IOException {
        // ... checks closed internally
        if (closed) {
            return EOF;
        }
    }
}
```

You just need to make sure they call `super.close()` to set the base class flag too, or use the base class field instead of their own.

## Conclusion

**The best approach is:**
1. Add `volatile boolean closed` to `NonBlockingReader` and `NonBlockingInputStream` base classes
2. Add `checkClosed()` helper method to base classes
3. Call `checkClosed()` in concrete implementations' read methods
4. **Eliminate all wrapper classes** - they're unnecessary!

This is:
- Simpler than wrappers
- Simpler than lambdas
- More performant
- Easier to maintain
- Already partially implemented (Pump classes have closed flags)

