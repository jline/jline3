---
sidebar_position: 2
---

# Diagnostic

When troubleshooting issues with JLine, especially when dealing with system terminals, it's helpful to run diagnostic tools to gather information about your environment. This page explains how to use JLine's diagnostic tools and interpret their output.

## JLine Diagnostic Tool

JLine provides a built-in diagnostic tool that can help identify issues with terminal detection and configuration. To run the JLine diagnostic tool, use the following command:

```bash
java -cp jline-3.26.1.jar org.jline.terminal.impl.Diag
```

This will output a full diagnosis for JLine. You may want to add some dependencies if you want specific providers to be loaded.

## Jansi Diagnostic Tool

If you're using Jansi with JLine, you can run the Jansi diagnostic tool to get information about both JLine and Jansi:

```bash
java -jar jansi-3.26.1.jar
```

This will output a full diagnosis for both JLine and Jansi.

## Output Example

Here's an example of the output from the diagnostic tools:

```
System properties
=================
os.name =         Mac OS X
OSTYPE =          null
MSYSTEM =         null
PWD =             /Users/gnodet/work/git/jline3
ConEmuPID =       null
WSL_DISTRO_NAME = null
WSL_INTEROP =     null

OSUtils
=================
IS_WINDOWS = false
IS_CYGWIN =  false
IS_MSYSTEM = false
IS_WSL =     false
IS_WSL1 =    false
IS_WSL2 =    false
IS_CONEMU =  false
IS_OSX =     true

FFM Support
=================
FFM support not available: java.io.IOException: Unable to load terminal provider ffm: null

JnaSupport
=================
JNA support not available: java.io.IOException: Unable to load terminal provider jna: null

Jansi2Support
=================
Jansi 2 support not available: java.io.IOException: Unable to load terminal provider jansi: null

JniSupport
=================
StdIn stream =    true
StdOut stream =   true
StdErr stream =   true
StdIn stream name =     /dev/ttys007
StdOut stream name =    /dev/ttys007
StdErr stream name =    /dev/ttys007
Terminal size: Size[cols=184, rows=47]
The terminal seems to work: terminal org.jline.terminal.impl.PosixSysTerminal with pty org.jline.terminal.impl.jni.osx.OsXNativePty

Exec Support
=================
StdIn stream =    true
StdOut stream =   true
StdErr stream =   true
StdIn stream name =     /dev/ttys007
StdOut stream name =    /dev/ttys007
StdErr stream name =    /dev/ttys007
Terminal size: Size[cols=184, rows=47]
The terminal seems to work: terminal org.jline.terminal.impl.PosixSysTerminal with pty org.jline.terminal.impl.exec.ExecPty
```

## Understanding the Output

The diagnostic output provides several sections of information:

1. **System Properties**: Shows relevant system properties that might affect terminal behavior.

2. **OSUtils**: Indicates what type of operating system you're running on.

3. **Provider Support**: Shows which terminal providers are available and working:
   - FFM Support
   - JNA Support
   - Jansi2 Support
   - JNI Support
   - Exec Support

4. **Terminal Information**: For each working provider, it shows:
   - Whether standard streams are available
   - The names of the standard streams
   - The terminal size
   - The terminal implementation being used

## Troubleshooting with Diagnostic Output

When troubleshooting issues with JLine, pay attention to:

1. **Missing Providers**: If a provider you expect to be available shows "not available," check that you have the necessary dependencies on your classpath.

2. **Stream Information**: If standard streams are not available or have unexpected names, this could indicate issues with how your application is launched.

3. **Terminal Size**: If the terminal size is incorrect or zero, this could indicate issues with terminal detection.

4. **Terminal Implementation**: The specific implementation being used can help identify compatibility issues.

## Reporting Issues

When reporting issues with JLine, always include the full diagnostic output. This helps the JLine developers understand your environment and the specific configuration that's causing problems.

To report an issue:

1. Run the appropriate diagnostic tool
2. Copy the full output
3. Create a new issue on the [JLine GitHub repository](https://github.com/jline/jline3/issues)
4. Paste the diagnostic output into your issue description
5. Provide additional details about your problem and steps to reproduce it
