/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

/**
 * A web-based terminal implementation that extends ScreenTerminal.
 * <p>
 * This class provides a web interface for terminal interaction using an embedded HTTP server.
 * It serves an HTML page with JavaScript that communicates with the terminal via HTTP requests.
 * The terminal supports ANSI escape sequences and renders them as HTML with CSS styling.
 * </p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>HTTP server using JDK's built-in HttpServer</li>
 *   <li>Real-time terminal updates via AJAX polling</li>
 *   <li>ANSI escape sequence rendering in HTML/CSS</li>
 *   <li>Keyboard input handling via JavaScript</li>
 *   <li>GZIP compression support</li>
 * </ul>
 */
public class WebTerminal extends ScreenTerminal {
    
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_HOST = "localhost";
    
    private HttpServer server;
    private final int port;
    private final String host;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<String, TerminalSession> sessions = new HashMap<>();
    
    /**
     * Creates a new WebTerminal with default settings (localhost:8080).
     */
    public WebTerminal() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }
    
    /**
     * Creates a new WebTerminal with specified host and port.
     * 
     * @param host the host to bind to
     * @param port the port to bind to
     */
    public WebTerminal(String host, int port) {
        this(host, port, 80, 24);
    }
    
    /**
     * Creates a new WebTerminal with specified host, port, and terminal size.
     * 
     * @param host the host to bind to
     * @param port the port to bind to
     * @param width terminal width in characters
     * @param height terminal height in characters
     */
    public WebTerminal(String host, int port, int width, int height) {
        super(width, height);
        this.host = host;
        this.port = port;
    }
    
    /**
     * Starts the HTTP server and begins serving the web terminal.
     * 
     * @throws IOException if the server cannot be started
     */
    public void start() throws IOException {
        if (running.get()) {
            throw new IllegalStateException("WebTerminal is already running");
        }
        
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/", new TerminalHandler());
        server.createContext("/terminal", new TerminalAjaxHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        running.set(true);
        
        System.out.println("WebTerminal started at http://" + host + ":" + port);
    }
    
    /**
     * Stops the HTTP server.
     */
    public void stop() {
        if (server != null && running.get()) {
            server.stop(0);
            running.set(false);
            System.out.println("WebTerminal stopped");
        }
    }
    
    /**
     * Returns whether the web terminal is currently running.
     * 
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Gets the URL where the web terminal is accessible.
     * 
     * @return the web terminal URL
     */
    public String getUrl() {
        return "http://" + host + ":" + port;
    }
    
    /**
     * HTTP handler for serving the main terminal page.
     */
    private class TerminalHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            
            if ("GET".equals(method)) {
                String path = exchange.getRequestURI().getPath();
                
                if ("/".equals(path) || "/index.html".equals(path)) {
                    serveTerminalPage(exchange);
                } else if (path.startsWith("/static/")) {
                    serveStaticResource(exchange, path);
                } else {
                    send404(exchange);
                }
            } else {
                send405(exchange);
            }
        }
        
        private void serveTerminalPage(HttpExchange exchange) throws IOException {
            String html = getTerminalHtml();
            byte[] response = html.getBytes(StandardCharsets.UTF_8);
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
        
        private void serveStaticResource(HttpExchange exchange, String path) throws IOException {
            String resourcePath = path.substring(8); // Remove "/static/"
            InputStream is = getClass().getResourceAsStream("/" + resourcePath);
            
            if (is == null) {
                send404(exchange);
                return;
            }
            
            String contentType = getContentType(resourcePath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            
            byte[] buffer = new byte[8192];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            byte[] response = baos.toByteArray();
            exchange.sendResponseHeaders(200, response.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
        
        private String getContentType(String path) {
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".html")) return "text/html";
            return "application/octet-stream";
        }
        
        private void send404(HttpExchange exchange) throws IOException {
            String response = "404 Not Found";
            exchange.sendResponseHeaders(404, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
        
        private void send405(HttpExchange exchange) throws IOException {
            String response = "405 Method Not Allowed";
            exchange.sendResponseHeaders(405, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    /**
     * HTTP handler for AJAX terminal communication.
     */
    private class TerminalAjaxHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            
            // Parse form data
            Map<String, String> params = parseFormData(exchange);
            String sessionId = getOrCreateSession(exchange);
            TerminalSession session = sessions.get(sessionId);
            
            String keyInput = params.get("k");
            String forceUpdate = params.get("f");
            
            // Process input
            if (keyInput != null && !keyInput.isEmpty()) {
                String processedInput = pipe(keyInput);
                // In a real implementation, this would be sent to a shell process
                write(processedInput);
            }
            
            // Get terminal output
            try {
                String output = dump(10, forceUpdate != null && !forceUpdate.isEmpty());
                if (output != null) {
                    sendResponse(exchange, output);
                } else {
                    sendResponse(exchange, "");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendResponse(exchange, "");
            }
        }
        
        private Map<String, String> parseFormData(HttpExchange exchange) throws IOException {
            Map<String, String> params = new HashMap<>();
            
            try (InputStream is = exchange.getRequestBody();
                 InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(isr)) {
                
                String formData = br.readLine();
                if (formData != null) {
                    String[] pairs = formData.split("&");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=", 2);
                        if (keyValue.length == 2) {
                            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                            String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            params.put(key, value);
                        }
                    }
                }
            }
            
            return params;
        }
        
        private String getOrCreateSession(HttpExchange exchange) {
            // Simple session management - in production, use proper session handling
            String sessionId = "default";
            if (!sessions.containsKey(sessionId)) {
                sessions.put(sessionId, new TerminalSession());
            }
            return sessionId;
        }
        
        private void sendResponse(HttpExchange exchange, String content) throws IOException {
            String encoding = exchange.getRequestHeaders().getFirst("Accept-Encoding");
            boolean supportsGzip = encoding != null && encoding.toLowerCase().contains("gzip");
            
            byte[] response;
            if (supportsGzip && content.length() > 100) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                    gzos.write(content.getBytes(StandardCharsets.UTF_8));
                }
                response = baos.toByteArray();
                exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            } else {
                response = content.getBytes(StandardCharsets.UTF_8);
            }
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }
    
    /**
     * Simple session container for terminal state.
     */
    private static class TerminalSession {
        // In a full implementation, this would contain session-specific state
        // For now, we'll use the shared ScreenTerminal state
    }
    
    /**
     * Generates the HTML page for the web terminal.
     */
    private String getTerminalHtml() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>JLine Web Terminal</title>
    <style>
        body {
            margin: 0;
            padding: 20px;
            background-color: #000;
            color: #fff;
            font-family: 'Courier New', monospace;
            overflow: hidden;
        }

        #terminal {
            width: 100%;
            height: calc(100vh - 40px);
            background-color: #000;
            border: 1px solid #333;
            padding: 10px;
            box-sizing: border-box;
            overflow: auto;
            white-space: pre;
            font-size: 14px;
            line-height: 1.2;
        }

        .terminal-line {
            margin: 0;
            padding: 0;
        }

        /* ANSI color classes */
        .ansi-black { color: #000; }
        .ansi-red { color: #cd0000; }
        .ansi-green { color: #00cd00; }
        .ansi-yellow { color: #cdcd00; }
        .ansi-blue { color: #0000ee; }
        .ansi-magenta { color: #cd00cd; }
        .ansi-cyan { color: #00cdcd; }
        .ansi-white { color: #e5e5e5; }
        .ansi-bright-black { color: #7f7f7f; }
        .ansi-bright-red { color: #ff0000; }
        .ansi-bright-green { color: #00ff00; }
        .ansi-bright-yellow { color: #ffff00; }
        .ansi-bright-blue { color: #5c5cff; }
        .ansi-bright-magenta { color: #ff00ff; }
        .ansi-bright-cyan { color: #00ffff; }
        .ansi-bright-white { color: #fff; }

        .ansi-bg-black { background-color: #000; }
        .ansi-bg-red { background-color: #cd0000; }
        .ansi-bg-green { background-color: #00cd00; }
        .ansi-bg-yellow { background-color: #cdcd00; }
        .ansi-bg-blue { background-color: #0000ee; }
        .ansi-bg-magenta { background-color: #cd00cd; }
        .ansi-bg-cyan { background-color: #00cdcd; }
        .ansi-bg-white { background-color: #e5e5e5; }

        .ansi-bold { font-weight: bold; }
        .ansi-underline { text-decoration: underline; }
        .ansi-inverse {
            background-color: #fff;
            color: #000;
        }

        .cursor {
            background-color: #fff;
            color: #000;
        }

        #input-handler {
            position: absolute;
            left: -9999px;
            opacity: 0;
        }
    </style>
</head>
<body>
    <div id="terminal"></div>
    <input type="text" id="input-handler" autocomplete="off">

    <script>
        class WebTerminal {
            constructor() {
                this.terminal = document.getElementById('terminal');
                this.inputHandler = document.getElementById('input-handler');
                this.setupEventHandlers();
                this.startPolling();
                this.inputHandler.focus();
            }

            setupEventHandlers() {
                // Keep input focused
                document.addEventListener('click', () => {
                    this.inputHandler.focus();
                });

                // Handle keyboard input
                this.inputHandler.addEventListener('keydown', (e) => {
                    this.handleKeyDown(e);
                });

                this.inputHandler.addEventListener('input', (e) => {
                    this.handleInput(e);
                });

                // Prevent losing focus
                this.inputHandler.addEventListener('blur', () => {
                    setTimeout(() => this.inputHandler.focus(), 10);
                });
            }

            handleKeyDown(e) {
                let key = '';

                // Handle special keys
                switch (e.key) {
                    case 'Enter':
                        key = '\\r';
                        break;
                    case 'Backspace':
                        key = '\\u007f';
                        break;
                    case 'Tab':
                        key = '\\t';
                        e.preventDefault();
                        break;
                    case 'ArrowUp':
                        key = '~A';
                        e.preventDefault();
                        break;
                    case 'ArrowDown':
                        key = '~B';
                        e.preventDefault();
                        break;
                    case 'ArrowRight':
                        key = '~C';
                        e.preventDefault();
                        break;
                    case 'ArrowLeft':
                        key = '~D';
                        e.preventDefault();
                        break;
                    case 'Home':
                        key = '~H';
                        e.preventDefault();
                        break;
                    case 'End':
                        key = '~F';
                        e.preventDefault();
                        break;
                    case 'PageUp':
                        key = '~1';
                        e.preventDefault();
                        break;
                    case 'PageDown':
                        key = '~2';
                        e.preventDefault();
                        break;
                    case 'Insert':
                        key = '~3';
                        e.preventDefault();
                        break;
                    case 'Delete':
                        key = '~4';
                        e.preventDefault();
                        break;
                    case 'F1':
                        key = '~a';
                        e.preventDefault();
                        break;
                    case 'F2':
                        key = '~b';
                        e.preventDefault();
                        break;
                    case 'F3':
                        key = '~c';
                        e.preventDefault();
                        break;
                    case 'F4':
                        key = '~d';
                        e.preventDefault();
                        break;
                    case 'F5':
                        key = '~e';
                        e.preventDefault();
                        break;
                    case 'F6':
                        key = '~f';
                        e.preventDefault();
                        break;
                    case 'F7':
                        key = '~g';
                        e.preventDefault();
                        break;
                    case 'F8':
                        key = '~h';
                        e.preventDefault();
                        break;
                    case 'F9':
                        key = '~i';
                        e.preventDefault();
                        break;
                    case 'F10':
                        key = '~j';
                        e.preventDefault();
                        break;
                    case 'F11':
                        key = '~k';
                        e.preventDefault();
                        break;
                    case 'F12':
                        key = '~l';
                        e.preventDefault();
                        break;
                    default:
                        if (e.ctrlKey) {
                            if (e.key.length === 1) {
                                key = String.fromCharCode(e.key.charCodeAt(0) - 64);
                                e.preventDefault();
                            }
                        }
                        break;
                }

                if (key) {
                    this.sendInput(key);
                    this.inputHandler.value = '';
                }
            }

            handleInput(e) {
                const value = e.target.value;
                if (value) {
                    this.sendInput(value);
                    e.target.value = '';
                }
            }

            sendInput(input) {
                const formData = new FormData();
                formData.append('k', input);

                fetch('/terminal', {
                    method: 'POST',
                    body: formData
                })
                .then(response => response.text())
                .then(data => {
                    if (data) {
                        this.terminal.innerHTML = data;
                        this.scrollToBottom();
                    }
                })
                .catch(error => {
                    console.error('Error sending input:', error);
                });
            }

            startPolling() {
                setInterval(() => {
                    const formData = new FormData();
                    formData.append('f', '1');

                    fetch('/terminal', {
                        method: 'POST',
                        body: formData
                    })
                    .then(response => response.text())
                    .then(data => {
                        if (data) {
                            this.terminal.innerHTML = data;
                            this.scrollToBottom();
                        }
                    })
                    .catch(error => {
                        console.error('Error polling terminal:', error);
                    });
                }, 100);
            }

            scrollToBottom() {
                this.terminal.scrollTop = this.terminal.scrollHeight;
            }
        }

        // Initialize terminal when page loads
        document.addEventListener('DOMContentLoaded', () => {
            new WebTerminal();
        });
    </script>
</body>
</html>
                """;
    }
}
