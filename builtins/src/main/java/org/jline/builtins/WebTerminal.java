/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import org.jline.terminal.impl.LineDisciplineTerminal;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * A web-based terminal implementation that extends LineDisciplineTerminal.
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
public class WebTerminal extends LineDisciplineTerminal {

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_HOST = "localhost";

    private final WebTerminalComponent component;
    private HttpServer server;
    private final int port;
    private final String host;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Creates a new WebTerminal with default settings (localhost:8080).
     */
    public WebTerminal() throws IOException {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    /**
     * Creates a new WebTerminal with specified host and port.
     *
     * @param host the host to bind to
     * @param port the port to bind to
     */
    public WebTerminal(String host, int port) throws IOException {
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
    @SuppressWarnings("this-escape")
    public WebTerminal(String host, int port, int width, int height) throws IOException {
        super("WebTerminal", "screen-256color", new WebTerminalOutputStream(), StandardCharsets.UTF_8);
        this.host = host;
        this.port = port;

        // Create the terminal component
        this.component = new WebTerminalComponent(width, height);
        this.component.setWebTerminal(this);

        // Connect the output stream to the component
        ((WebTerminalOutputStream) masterOutput).setComponent(this.component);
    }

    /**
     * Gets the web terminal component.
     *
     * @return the WebTerminalComponent instance
     */
    public WebTerminalComponent getComponent() {
        return component;
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

    // Delegation methods to the component

    /**
     * Writes text to the terminal.
     *
     * @param text the text to write
     * @return true if successful
     */
    public boolean write(String text) {
        return component.write(text);
    }

    /**
     * Reads and processes input through the terminal.
     *
     * @param input the input to process
     * @return the processed input
     */
    public String pipe(String input) {
        return component.pipe(input);
    }

    public String read() {
        return component.read();
    }

    /**
     * Dumps the terminal content as HTML.
     *
     * @param timeout timeout in seconds
     * @param forceUpdate whether to force an update
     * @return the terminal content as HTML
     * @throws InterruptedException if interrupted
     */
    public String dump(int timeout, boolean forceUpdate) throws InterruptedException {
        return component.dump(timeout, forceUpdate);
    }

    /**
     * Sets the terminal size.
     *
     * @param width the new width
     * @param height the new height
     * @return true if successful
     */
    public boolean setSize(int width, int height) {
        return component.setSize(width, height);
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
                            String key = URLDecoder.decode(keyValue[0], "UTF-8");
                            String value = URLDecoder.decode(keyValue[1], "UTF-8");
                            params.put(key, value);
                        }
                    }
                }
            }

            return params;
        }

        private String getOrCreateSession(HttpExchange exchange) {
            // Simple session management - in production, use proper session handling
            return "default";
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
     * Custom OutputStream for the WebTerminal that writes to the component.
     */
    private static class WebTerminalOutputStream extends OutputStream {

        private WebTerminalComponent component;

        @Override
        public void write(int b) throws IOException {
            if (component != null) {
                component.write(String.valueOf((char) b));
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (component != null) {
                String text = new String(b, off, len, StandardCharsets.UTF_8);
                component.write(text);
            }
        }

        @Override
        public void flush() throws IOException {
            // No buffering, so nothing to flush
        }

        public void setComponent(WebTerminalComponent component) {
            this.component = component;
        }
    }

    /**
     * The inner WebTerminalComponent that contains the original ScreenTerminal-based implementation.
     * This is the inner class that contains the original ScreenTerminal-based implementation.
     */
    public static class WebTerminalComponent extends ScreenTerminal {

        private static final long serialVersionUID = 1L;

        private transient WebTerminal webTerminal;

        /**
         * Creates a new WebTerminalComponent with the specified dimensions.
         *
         * @param width terminal width in characters
         * @param height terminal height in characters
         */
        public WebTerminalComponent(int width, int height) {
            super(width, height);
        }

        /**
         * Sets the web terminal reference after construction to avoid this-escape issues.
         *
         * @param webTerminal the WebTerminal instance
         */
        public void setWebTerminal(WebTerminal webTerminal) {
            this.webTerminal = webTerminal;
        }

        /**
         * Writes text to the terminal.
         *
         * @param text the text to write
         * @return true if successful
         */
        public boolean write(String text) {
            return super.write(text);
        }

        /**
         * Reads and processes input through the terminal.
         *
         * @param input the input to process
         * @return the processed input
         */
        public String pipe(String input) {
            // Process special key sequences
            if (input.startsWith("~")) {
                switch (input) {
                    case "~A":
                        return "\u001b[A"; // Up arrow
                    case "~B":
                        return "\u001b[B"; // Down arrow
                    case "~C":
                        return "\u001b[C"; // Right arrow
                    case "~D":
                        return "\u001b[D"; // Left arrow
                    default:
                        return input;
                }
            }

            // Handle carriage return
            if ("\r".equals(input)) {
                return "\r\n";
            }

            return input;
        }

        /**
         * Dumps the terminal content as HTML with enhanced attribute support.
         * This method overrides the ScreenTerminal dump method to provide better
         * RGB color support and attribute handling.
         *
         * @param timeout timeout in seconds
         * @param forceUpdate whether to force an update
         * @return the terminal content as HTML
         * @throws InterruptedException if interrupted
         */
        public String dump(int timeout, boolean forceUpdate) throws InterruptedException {
            // Use the parent's dump method but enhance the output
            waitDirty();
            String originalHtml = super.dump(timeout, forceUpdate);
            if (originalHtml != null) {
                return enhanceHtmlOutput(originalHtml);
            }
            return null;
        }

        /**
         * Enhances the HTML output from ScreenTerminal to support RGB colors.
         * This method processes the original HTML and replaces basic color classes
         * with inline styles that support RGB colors.
         *
         * @param originalHtml the original HTML from ScreenTerminal
         * @return enhanced HTML with RGB color support
         */
        private String enhanceHtmlOutput(String originalHtml) {
            // For now, return the original HTML
            // TODO: Implement RGB color enhancement by parsing and replacing color classes
            return originalHtml;
        }

        /**
         * Generates a span tag with proper CSS styling for the given attributes.
         * Handles RGB colors, bold, underline, inverse, and other attributes.
         *
         * @param attr the attribute value from the cell
         * @return HTML span tag with appropriate CSS classes and inline styles
         */
        private String generateSpanTag(long attr) {
            // Attribute mask: 0xYXFFFBBB00000000L
            // X: Bit 0 - Underlined, Bit 1 - Negative, Bit 2 - Concealed, Bit 3 - Bold
            // Y: Bit 0 - Foreground set, Bit 1 - Background set
            // F: Foreground r-g-b
            // B: Background r-g-b

            int bg = (int) ((attr) & 0x0fff);
            int fg = (int) ((attr >>> 12) & 0x0fff);
            boolean underline = (attr & 0x01000000L) != 0;
            boolean inverse = (attr & 0x02000000L) != 0;
            boolean conceal = (attr & 0x04000000L) != 0;
            boolean bold = (attr & 0x08000000L) != 0;
            boolean fgset = (attr & 0x10000000L) != 0;
            boolean bgset = (attr & 0x20000000L) != 0;

            // Handle default colors
            if (!fgset) {
                fg = 0x0fff; // Default white foreground
            }
            if (!bgset) {
                bg = 0x0000; // Default black background
            }

            // Handle inverse
            if (inverse) {
                int temp = fg;
                fg = bg;
                bg = temp;
            }

            // Handle concealed
            if (conceal) {
                fg = bg; // Make text invisible by setting foreground to background
            }

            StringBuilder span = new StringBuilder("<span style='");

            // Add foreground color
            String fgColor = rgbToHex(fg);
            span.append("color:").append(fgColor).append(";");

            // Add background color
            String bgColor = rgbToHex(bg);
            span.append("background-color:").append(bgColor).append(";");

            // Add text decorations
            if (underline) {
                span.append("text-decoration:underline;");
            }

            // Add font weight
            if (bold) {
                span.append("font-weight:bold;");
            }

            span.append("'>");
            return span.toString();
        }

        /**
         * Converts a 12-bit RGB color value to a hex color string.
         * The format is 0xRGB where each component is 4 bits.
         *
         * @param color 12-bit color value
         * @return hex color string (e.g., "#ff0000")
         */
        private String rgbToHex(int color) {
            int r = ((color >> 8) & 0x0f) << 4; // Expand 4-bit to 8-bit
            int g = ((color >> 4) & 0x0f) << 4;
            int b = ((color >> 0) & 0x0f) << 4;
            return String.format("#%02x%02x%02x", r, g, b);
        }

        /**
         * Sets the terminal size.
         *
         * @param width the new width
         * @param height the new height
         * @return true if successful
         */
        public boolean setSize(int width, int height) {
            if (width < 10 || height < 5 || width > 200 || height > 100) {
                return false;
            }
            // ScreenTerminal.setSize takes width and height directly
            return super.setSize(width, height);
        }
    }

    /**
     * Generates the HTML page for the web terminal.
     */
    private String getTerminalHtml() {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "    <meta charset=\"UTF-8\">\n"
                + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "    <title>JLine Web Terminal</title>\n"
                + "    <style>\n"
                + "        body {\n"
                + "            margin: 0;\n"
                + "            padding: 20px;\n"
                + "            background-color: #000;\n"
                + "            color: #fff;\n"
                + "            font-family: 'Courier New', monospace;\n"
                + "            overflow: hidden;\n"
                + "        }\n"
                + "        \n"
                + "        #terminal {\n"
                + "            width: 100%;\n"
                + "            height: calc(100vh - 40px);\n"
                + "            background-color: #000;\n"
                + "            border: 1px solid #333;\n"
                + "            padding: 10px;\n"
                + "            box-sizing: border-box;\n"
                + "            overflow: auto;\n"
                + "            white-space: pre;\n"
                + "            font-size: 14px;\n"
                + "            line-height: 1.2;\n"
                + "        }\n"
                + "        \n"
                + "        .terminal-line {\n"
                + "            margin: 0;\n"
                + "            padding: 0;\n"
                + "        }\n"
                + "        \n"
                + "        /* ANSI color classes */\n"
                + "        .ansi-black { color: #000; }\n"
                + "        .ansi-red { color: #cd0000; }\n"
                + "        .ansi-green { color: #00cd00; }\n"
                + "        .ansi-yellow { color: #cdcd00; }\n"
                + "        .ansi-blue { color: #0000ee; }\n"
                + "        .ansi-magenta { color: #cd00cd; }\n"
                + "        .ansi-cyan { color: #00cdcd; }\n"
                + "        .ansi-white { color: #e5e5e5; }\n"
                + "        .ansi-bright-black { color: #7f7f7f; }\n"
                + "        .ansi-bright-red { color: #ff0000; }\n"
                + "        .ansi-bright-green { color: #00ff00; }\n"
                + "        .ansi-bright-yellow { color: #ffff00; }\n"
                + "        .ansi-bright-blue { color: #5c5cff; }\n"
                + "        .ansi-bright-magenta { color: #ff00ff; }\n"
                + "        .ansi-bright-cyan { color: #00ffff; }\n"
                + "        .ansi-bright-white { color: #fff; }\n"
                + "        \n"
                + "        .ansi-bg-black { background-color: #000; }\n"
                + "        .ansi-bg-red { background-color: #cd0000; }\n"
                + "        .ansi-bg-green { background-color: #00cd00; }\n"
                + "        .ansi-bg-yellow { background-color: #cdcd00; }\n"
                + "        .ansi-bg-blue { background-color: #0000ee; }\n"
                + "        .ansi-bg-magenta { background-color: #cd00cd; }\n"
                + "        .ansi-bg-cyan { background-color: #00cdcd; }\n"
                + "        .ansi-bg-white { background-color: #e5e5e5; }\n"
                + "        \n"
                + "        .ansi-bold { font-weight: bold; }\n"
                + "        .ansi-underline { text-decoration: underline; }\n"
                + "        .ansi-inverse { \n"
                + "            background-color: #fff; \n"
                + "            color: #000; \n"
                + "        }\n"
                + "        \n"
                + "        .cursor {\n"
                + "            background-color: #fff;\n"
                + "            color: #000;\n"
                + "        }\n"
                + "        \n"
                + "        #input-handler {\n"
                + "            position: absolute;\n"
                + "            left: -9999px;\n"
                + "            opacity: 0;\n"
                + "        }\n"
                + "    </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "    <div id=\"terminal\"></div>\n"
                + "    <input type=\"text\" id=\"input-handler\" autocomplete=\"off\">\n"
                + "    \n"
                + "    <script>\n"
                + "        class WebTerminal {\n"
                + "            constructor() {\n"
                + "                this.terminal = document.getElementById('terminal');\n"
                + "                this.inputHandler = document.getElementById('input-handler');\n"
                + "                this.setupEventHandlers();\n"
                + "                this.startPolling();\n"
                + "                this.inputHandler.focus();\n"
                + "            }\n"
                + "            \n"
                + "            setupEventHandlers() {\n"
                + "                // Keep input focused\n"
                + "                document.addEventListener('click', () => {\n"
                + "                    this.inputHandler.focus();\n"
                + "                });\n"
                + "                \n"
                + "                // Handle keyboard input\n"
                + "                this.inputHandler.addEventListener('keydown', (e) => {\n"
                + "                    this.handleKeyDown(e);\n"
                + "                });\n"
                + "                \n"
                + "                this.inputHandler.addEventListener('input', (e) => {\n"
                + "                    this.handleInput(e);\n"
                + "                });\n"
                + "                \n"
                + "                // Prevent losing focus\n"
                + "                this.inputHandler.addEventListener('blur', () => {\n"
                + "                    setTimeout(() => this.inputHandler.focus(), 10);\n"
                + "                });\n"
                + "            }\n"
                + "            \n"
                + "            handleKeyDown(e) {\n"
                + "                let key = '';\n"
                + "                \n"
                + "                // Handle special keys\n"
                + "                switch (e.key) {\n"
                + "                    case 'Enter':\n"
                + "                        key = '\\\\r';\n"
                + "                        break;\n"
                + "                    case 'Backspace':\n"
                + "                        key = '\\\\u007f';\n"
                + "                        break;\n"
                + "                    case 'Tab':\n"
                + "                        key = '\\\\t';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'ArrowUp':\n"
                + "                        key = '~A';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'ArrowDown':\n"
                + "                        key = '~B';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'ArrowRight':\n"
                + "                        key = '~C';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'ArrowLeft':\n"
                + "                        key = '~D';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'Home':\n"
                + "                        key = '~H';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'End':\n"
                + "                        key = '~F';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'PageUp':\n"
                + "                        key = '~1';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'PageDown':\n"
                + "                        key = '~2';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'Insert':\n"
                + "                        key = '~3';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'Delete':\n"
                + "                        key = '~4';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'F1':\n"
                + "                        key = '~a';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'F2':\n"
                + "                        key = '~b';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'F3':\n"
                + "                        key = '~c';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'F4':\n"
                + "                        key = '~d';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'F5':\n"
                + "                        key = '~e';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'F6':\n"
                + "                        key = '~f';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'F7':\n"
                + "                        key = '~g';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'F8':\n"
                + "                        key = '~h';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'F9':\n"
                + "                        key = '~i';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'F10':\n"
                + "                        key = '~j';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'F11':\n"
                + "                        key = '~k';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    case 'F12':\n"
                + "                        key = '~l';\n"
                + "                        e.preventDefault();\n"
                + "                        break;\n"
                + "                    default:\n"
                + "                        if (e.ctrlKey) {\n"
                + "                            if (e.key.length === 1) {\n"
                + "                                key = String.fromCharCode(e.key.charCodeAt(0) - 64);\n"
                + "                                e.preventDefault();\n"
                + "                            }\n"
                + "                        }\n"
                + "                        break;\n"
                + "                }\n"
                + "                \n"
                + "                if (key) {\n"
                + "                    this.sendInput(key);\n"
                + "                    this.inputHandler.value = '';\n"
                + "                }\n"
                + "            }\n"
                + "            \n"
                + "            handleInput(e) {\n"
                + "                const value = e.target.value;\n"
                + "                if (value) {\n"
                + "                    this.sendInput(value);\n"
                + "                    e.target.value = '';\n"
                + "                }\n"
                + "            }\n"
                + "            \n"
                + "            sendInput(input) {\n"
                + "                const formData = new FormData();\n"
                + "                formData.append('k', input);\n"
                + "                \n"
                + "                fetch('/terminal', {\n"
                + "                    method: 'POST',\n"
                + "                    body: formData\n"
                + "                })\n"
                + "                .then(response => response.text())\n"
                + "                .then(data => {\n"
                + "                    if (data) {\n"
                + "                        this.terminal.innerHTML = data;\n"
                + "                        this.scrollToBottom();\n"
                + "                    }\n"
                + "                })\n"
                + "                .catch(error => {\n"
                + "                    console.error('Error sending input:', error);\n"
                + "                });\n"
                + "            }\n"
                + "            \n"
                + "            startPolling() {\n"
                + "                setInterval(() => {\n"
                + "                    const formData = new FormData();\n"
                + "                    formData.append('f', '1');\n"
                + "                    \n"
                + "                    fetch('/terminal', {\n"
                + "                        method: 'POST',\n"
                + "                        body: formData\n"
                + "                    })\n"
                + "                    .then(response => response.text())\n"
                + "                    .then(data => {\n"
                + "                        if (data) {\n"
                + "                            this.terminal.innerHTML = data;\n"
                + "                            this.scrollToBottom();\n"
                + "                        }\n"
                + "                    })\n"
                + "                    .catch(error => {\n"
                + "                        console.error('Error polling terminal:', error);\n"
                + "                    });\n"
                + "                }, 100);\n"
                + "            }\n"
                + "            \n"
                + "            scrollToBottom() {\n"
                + "                this.terminal.scrollTop = this.terminal.scrollHeight;\n"
                + "            }\n"
                + "        }\n"
                + "        \n"
                + "        // Initialize terminal when page loads\n"
                + "        document.addEventListener('DOMContentLoaded', () => {\n"
                + "            new WebTerminal();\n"
                + "        });\n"
                + "    </script>\n"
                + "</body>\n"
                + "</html>";
    }
}
