---
sidebar_position: 7
---

# Remote Terminals

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides support for remote terminal connections through its `remote-telnet` and `remote-ssh` modules, allowing you to create networked terminal applications accessible via Telnet or SSH protocols.

## Overview

Remote terminals enable your JLine-based applications to be accessed over a network, providing:

- Multi-user access to your application
- Remote administration capabilities
- Network service interfaces
- Terminal access to embedded or headless systems

JLine's remote terminal support is built on the same core Terminal and LineReader abstractions used for local terminals, ensuring consistent behavior and capabilities.

## Telnet Support

The `remote-telnet` module provides a simple Telnet server implementation that can be used to expose JLine-based applications over the network.

### Maven Dependency

To use the Telnet support, add the following dependency to your project:

import VersionDisplay from '@site/src/components/VersionDisplay';

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-remote-telnet</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>
```

### Basic Telnet Server

Here's how to create a basic Telnet server:

<CodeSnippet name="TelnetServerExample" />

### Telnet Server Options

The Telnet server supports several configuration options:

```java
// Start on a specific port and interface
telnet.telnetd(System.out, System.err, new String[]{
    "--port=2023",  // Default is 2019
    "--ip=0.0.0.0", // Default is 127.0.0.1 (localhost only)
    "start"
});

// Check server status
telnet.telnetd(System.out, System.err, new String[]{"status"});

// Stop the server
telnet.telnetd(System.out, System.err, new String[]{"stop"});
```

### Connection Management

The Telnet server includes a `ConnectionManager` that handles client connections:

```java
// Create a custom connection manager
ConnectionManager manager = new ConnectionManager(
    10,              // Maximum number of connections
    5 * 60 * 1000,   // Warning timeout (5 minutes)
    10 * 60 * 1000,  // Disconnect timeout (10 minutes)
    60 * 1000,       // Housekeeping interval (1 minute)
    null,            // Connection filter
    null,            // Login shell
    false            // Line mode
);
```

## SSH Support

The `remote-ssh` module provides SSH server and client capabilities, offering a more secure alternative to Telnet.

### Maven Dependency

To use the SSH support, add the following dependency to your project:

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-remote-ssh</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>
```

### Basic SSH Server

Here's how to create a basic SSH server:

<CodeSnippet name="SSHServerExample" />

### SSH Server Options

The SSH server supports several configuration options:

```java
// Start on a specific port and interface
ssh.sshd(System.out, System.err, new String[]{
    "--port=2222",  // Default is 8022
    "--ip=0.0.0.0", // Default is 127.0.0.1 (localhost only)
    "start"
});

// Check server status
ssh.sshd(System.out, System.err, new String[]{"status"});

// Stop the server
ssh.sshd(System.out, System.err, new String[]{"stop"});
```

### SSH Authentication

SSH supports various authentication methods:

```java
// Password authentication
server.setPasswordAuthenticator((username, password, session) ->
    "admin".equals(username) && "password".equals(password));

// Public key authentication
server.setPublickeyAuthenticator((username, key, session) -> {
    // Verify the key against authorized keys
    return true; // if valid
});
```

## Creating Remote Terminals

When a client connects to your Telnet or SSH server, JLine creates a new Terminal instance for that connection. This terminal is created using the `TerminalBuilder`:

```java
Terminal terminal = TerminalBuilder.builder()
        .type(negotiatedTerminalType.toLowerCase())
        .streams(inputStream, outputStream)
        .system(false)
        .name("remote")
        .build();
```

The terminal type is negotiated with the client during connection setup, and the terminal's input and output streams are connected to the client's network connection.

## Handling Terminal Events

Remote terminals need to handle various events, such as window resizing:

```java
// Set initial terminal size
terminal.setSize(new Size(columns, rows));

// Handle window resize events
terminal.handle(Signal.WINCH, signal -> {
    // Terminal size has changed
    Size size = terminal.getSize();
    // Update your UI accordingly
});
```

## Best Practices

When working with remote terminals, consider these best practices:

1. **Security**: Use SSH instead of Telnet for production environments
2. **Authentication**: Implement proper authentication for SSH servers
3. **Timeouts**: Configure appropriate connection timeouts
4. **Error Handling**: Handle network errors and disconnections gracefully
5. **Resource Management**: Limit the number of concurrent connections
6. **Terminal Capabilities**: Check terminal capabilities before using advanced features

## Example: Interactive Shell

Here's a more complete example of an interactive shell for remote terminals:

<CodeSnippet name="RemoteShellExample" />

## Conclusion

JLine's remote terminal support enables you to create networked terminal applications with the same rich features available to local terminals. Whether you need to provide remote administration capabilities or build multi-user terminal applications, the `remote-telnet` and `remote-ssh` modules offer the tools you need.

For more information, refer to the API documentation for the `remote-telnet` and `remote-ssh` modules.
