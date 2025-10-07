---
sidebar_position: 2
---

# JPMS (Java Platform Module System) Support

JLine 4.0+ provides comprehensive support for the Java Platform Module System (JPMS), allowing you to use JLine in modular applications with proper module boundaries and dependencies.

## Overview

JLine's JPMS support enables:
- **Strong encapsulation**: Internal APIs are properly hidden from consumers
- **Explicit dependencies**: Clear module boundaries and compile-time dependency checking
- **Service provider interfaces**: Automatic discovery of terminal providers
- **Backward compatibility**: Works with both modular and non-modular applications

## Module Status Overview

JLine modules are categorized into two groups based on their JPMS support:

### ✅ Full JPMS Modules

These modules have complete `module-info.java` descriptors with proper exports, requires, and service declarations:

| Module | Artifact ID | Module Name | JDK Requirement | Description |
|--------|-------------|-------------|-----------------|-------------|
| **Core Infrastructure** | | | | |
| Native | `jline-native` | `org.jline.nativ` | 11+ | Native library loading and OS detection |
| Terminal | `jline-terminal` | `org.jline.terminal` | 11+ | Core terminal abstraction and utilities |
| Reader | `jline-reader` | `org.jline.reader` | 11+ | Line editing, completion, and history |
| Style | `jline-style` | `org.jline.style` | 11+ | Text styling and color support |
| **Terminal Providers** | | | | |
| Terminal JNI | `jline-terminal-jni` | `org.jline.terminal.jni` | 11+ | **Recommended**: Java Native Interface provider |
| Terminal FFM | `jline-terminal-ffm` | `org.jline.terminal.ffm` | 22+ | **Recommended**: Foreign Function Memory API provider |
| **Extended Functionality** | | | | |
| Builtins | `jline-builtins` | `org.jline.builtins` | 11+ | Built-in shell commands (ls, cat, etc.) |
| Console UI | `jline-console-ui` | `org.jline.console.ui` | 11+ | Interactive UI components (deprecated) |
| Prompt | `jline-prompt` | `org.jline.prompt` | 11+ | Modern prompt API for interactive applications |
| Console | `jline-console` | `org.jline.console` | 11+ | High-level console framework |
| Jansi Core | `jline-jansi-core` | `org.jline.jansi.core` | 11+ | JLine's ANSI implementation |
| Curses | `jline-curses` | `org.jline.curses` | 11+ | Curses-like UI components |

### ❌ Automatic Modules

These modules remain as automatic modules for backward compatibility and integration purposes:

| Module | Artifact ID | Automatic Module Name | Reason for Automatic Module Status |
|--------|-------------|----------------------|-------------------------------------|
| ~~Terminal Jansi~~ | ~~`jline-terminal-jansi`~~ | ~~`jline.terminal.jansi`~~ | **Removed in JLine 4.x**: Use JNI or FFM provider instead |
| Groovy | `jline-groovy` | `jline.groovy` | Integration with Groovy ecosystem, complex dependencies |
| Remote SSH | `jline-remote-ssh` | `jline.remote.ssh` | SSH server functionality, Apache SSHD dependencies |
| Remote Telnet | `jline-remote-telnet` | `jline.remote.telnet` | Telnet server functionality |
| Demo | `jline-demo` | `jline.demo` | Example applications, not intended for production use |
| Graal | `jline-graal` | `jline.graal` | GraalVM native image support and configuration |

**Note**: Automatic modules derive their module name from the JAR filename, but JLine explicitly sets the `Automatic-Module-Name` manifest attribute for predictable module names.

## Using JLine in Modular Applications

### Basic Module Declaration

For a simple modular application using JLine's core functionality:

```java
module your.application {
    requires org.jline.terminal;
    requires org.jline.reader;
}
```

### Full-Featured Application

For applications using all JLine features:

```java
module your.application {
    // Core modules
    requires org.jline.terminal;
    requires org.jline.reader;

    // Optional modules
    requires org.jline.style;        // For text styling
    requires org.jline.builtins;     // For built-in commands
    requires org.jline.console;      // For console framework
    requires org.jline.prompt;       // For modern prompt API
    requires org.jline.console.ui;   // For legacy UI components (deprecated)

    // Terminal providers (choose one or more)
    requires org.jline.terminal.jni; // JNI-based (recommended)
    requires org.jline.terminal.ffm; // FFM-based (recommended, JDK 22+)
}
```

### Using Automatic Modules

If you need functionality from automatic modules, you can still use them:

```java
module your.application {
    requires org.jline.terminal;
    requires org.jline.reader;
    
    // JPMS module
    requires org.jline.curses;       // For curses UI

    // Automatic modules (use artifact name as module name)
    requires jline.groovy;           // For Groovy integration
    requires jline.remote.ssh;       // For SSH server
    // Note: jline.terminal.jansi is deprecated, use JNI provider instead
}
```

## Special Considerations

### FFM Terminal Provider (JDK 22+)

The FFM (Foreign Function Memory) terminal provider uses JDK 22's Foreign Function & Memory API and requires special runtime permissions.

#### Module Path Requirement

**Critical**: To use `--enable-native-access=org.jline.terminal.ffm`, JLine jars **must be on the module path**, not the classpath:

```bash
# ✅ CORRECT: JLine on module path
java --module-path /path/to/jline/jars \
     --enable-native-access=org.jline.terminal.ffm \
     --module your.app/your.Main

# ❌ INCORRECT: JLine on classpath (flag won't work)
java -cp /path/to/jline/jars:/path/to/your/app \
     --enable-native-access=org.jline.terminal.ffm \
     your.Main
```

#### Alternative for Classpath Applications

If you must use the classpath, use the less secure but functional alternative:

```bash
# Works with classpath, but less secure
java -cp /path/to/jline/jars:/path/to/your/app \
     --enable-native-access=ALL-UNNAMED \
     your.Main
```

**Important**: The FFM provider is only available on JDK 22+ and provides the best performance and compatibility when properly configured.

### JNI vs FFM Providers

#### JNI Provider (No Special Setup)
- **Traditional JNI**: Uses pre-compiled native libraries (`.so`, `.dll`, `.dylib`)
- **No permissions required**: Works with standard Java security model
- **JDK 11+ compatible**: Available on all supported JDK versions
- **Recommended for most users**: Easiest to set up and deploy

#### FFM Provider (Requires Native Access)
- **Modern Panama API**: Uses Foreign Function & Memory API (JDK 22+)
- **Requires `--enable-native-access`**: Must grant explicit native access permissions
- **Module path required**: Only works when JLine is on the module path
- **Best performance**: Optimized for modern JDK versions

### Terminal Provider Selection

JLine uses a service provider interface (SPI) to automatically discover and select terminal providers. The selection priority is:

1. **JNI** (`org.jline.terminal.jni`) - **Recommended**: Native performance, no external dependencies, no special permissions
2. **FFM** (`org.jline.terminal.ffm`) - **Recommended**: Best performance, requires `--enable-native-access`, JDK 22+ only
3. ~~**JNA** (`org.jline.terminal.jna`)~~ - **Removed in JLine 4.x**: Use JNI or FFM instead
4. ~~**Jansi** (`org.jline.terminal.jansi`)~~ - **Removed in JLine 4.x**: Use JNI or FFM instead

**Recommended providers**: Use **JNI** for maximum compatibility (no special setup) or **FFM** for best performance on JDK 22+.

You can force a specific provider by including only that module in your dependencies.

### Dependency Management

When using Maven or Gradle, you can depend on individual modules:

#### Maven
```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-terminal</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-reader</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>
```

#### Gradle
```groovy
implementation 'org.jline:jline-terminal:%%JLINE_VERSION%%'
implementation 'org.jline:jline-reader:%%JLINE_VERSION%%'
```

## Migration from JLine 3.x

When migrating from JLine 3.x to 4.x in a modular application:

1. **Add module-info.java** to your application if not already present
2. **Update requires statements** to use the new module names
3. **Choose appropriate terminal provider** based on your JDK version and requirements
4. **Update Maven/Gradle dependencies** to use individual modules instead of the uber-jar
5. **Test thoroughly** - JPMS provides stronger compile-time checking that may reveal issues

### Example Migration

**Before (JLine 3.x):**
```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>3.30.0</version>
</dependency>
```

**After (JLine 4.x):**
```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-terminal</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-reader</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-terminal-jni</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>
```

```java
module your.application {
    requires org.jline.terminal;
    requires org.jline.reader;
    requires org.jline.terminal.jni; // Recommended provider
}
```

## Best Practices

### Module Selection Strategy

- **Minimal applications**: Use only `org.jline.terminal` and `org.jline.reader`
- **Shell applications**: Add `org.jline.builtins` for common commands (ls, cat, etc.)
- **UI applications**: Add `org.jline.curses` for advanced UI components

### Terminal Provider Selection

- **Recommended**: Use `org.jline.terminal.jni` (no external dependencies, works on JDK 11+)
- **Best performance**: Use `org.jline.terminal.ffm` on JDK 22+

### Dependency Scope

```xml
<!-- Core modules: compile scope -->
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-terminal</artifactId>
    <scope>compile</scope>
</dependency>

<!-- Terminal providers: runtime scope (SPI) -->
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-terminal-jni</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Gradual Migration

For existing applications, consider a phased approach:

1. **Phase 1**: Keep using `jline` uber-jar (automatic module)
2. **Phase 2**: Switch to individual modules on classpath
3. **Phase 3**: Convert your application to JPMS module
4. **Phase 4**: Optimize by removing unused modules

## Troubleshooting

### FFM Provider Not Working

**Problem**: FFM provider not being used despite being on classpath.

**Solutions**:
1. **Use JNI provider instead** (recommended for most users):
   ```bash
   # No special setup required
   java --module-path jline-jars/ --module your.app/your.Main
   ```

2. **Fix FFM setup** (for JDK 22+ users wanting best performance):
   ```bash
   # Ensure module path and native access
   java --module-path jline-jars/ --enable-native-access=org.jline.terminal.ffm ...
   ```

### "Module not found" Errors

**Problem**: `java.lang.module.FindException: Module X not found`

**Solutions**:
- Verify all required modules are on the module path
- Check module names match exactly (e.g., `org.jline.terminal`, not `jline-terminal`)
- Ensure you're using JPMS modules, not automatic modules for requires statements

### Native Access Denied

**Problem**: `java.lang.IllegalCallerException: access denied`

**Solutions**:
- Add `--enable-native-access=org.jline.terminal.ffm` (module path)
- Or use `--enable-native-access=ALL-UNNAMED` (classpath)
- Ensure you're running on JDK 22+ for FFM provider

### Service Provider Not Found

**Problem**: No terminal provider found, falling back to dumb terminal.

**Solutions**:
- Include at least one terminal provider module (jni, or ffm)
- Verify provider JARs are on module path or classpath
- Check that provider modules are not excluded by your build tool

## Benefits of JPMS Support

- **Explicit dependencies**: Clear module boundaries and dependencies
- **Encapsulation**: Internal APIs are properly hidden
- **Reliability**: Compile-time dependency checking
- **Performance**: Faster startup and reduced memory footprint
- **Security**: Better isolation between modules
- **Maintainability**: Clearer architecture and dependencies
