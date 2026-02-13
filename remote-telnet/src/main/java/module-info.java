/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Remote Telnet Module.
 * <p>
 * This module provides a simple Telnet server implementation for JLine applications,
 * enabling remote terminal access over Telnet protocol. This is primarily intended
 * for development and testing purposes.
 * <p>
 * <strong>Note:</strong> For production use, SSH (provided by the {@code org.jline.remote.ssh}
 * module) is strongly recommended over Telnet due to security considerations. The Telnet
 * protocol transmits data in plain text without encryption.
 * <p>
 * Key features include:
 * <ul>
 * <li>Simple Telnet server for hosting JLine shells remotely</li>
 * <li>Connection management and filtering</li>
 * <li>Terminal emulation over Telnet protocol</li>
 * <li>Integration with JLine's terminal infrastructure</li>
 * </ul>
 * <p>
 * <h2>Public API Packages</h2>
 * <ul>
 * <li>{@code org.jline.builtins.telnet} - Telnet server implementation including
 *     {@code Telnet}, {@code ConnectionManager}, {@code PortListener}, and related classes</li>
 * </ul>
 */
module org.jline.remote.telnet {
    // Core Java platform
    requires java.base;
    requires java.logging;

    // JLine dependencies
    requires transitive org.jline.builtins;

    // Export public API
    exports org.jline.builtins.telnet;
}
