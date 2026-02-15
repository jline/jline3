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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
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
    private ExecutorService executor;
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
        if (server != null) {
            return "http://" + host + ":" + server.getAddress().getPort();
        }
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

        ThreadFactory daemonFactory = r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("WebTerminal-" + t.getId());
            return t;
        };
        executor = Executors.newCachedThreadPool(daemonFactory);
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/", new TerminalHandler());
        server.createContext("/terminal", new TerminalAjaxHandler());
        server.setExecutor(executor);
        server.start();
        running.set(true);
    }

    @Override
    protected void doClose() throws IOException {
        super.doClose();
        stop();
    }

    /**
     * Stops the HTTP server.
     */
    public void stop() {
        if (server != null && running.get()) {
            server.stop(0);
            if (executor != null) {
                executor.shutdownNow();
            }
            running.set(false);
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
     * @param timeout maximum time to wait for changes in milliseconds
     * @param forceUpdate whether to force an update even if screen is not dirty
     * @return the terminal content as HTML, or null if no update
     * @throws InterruptedException if interrupted
     */
    public String dump(long timeout, boolean forceUpdate) throws InterruptedException {
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
            try (InputStream is = getClass().getResourceAsStream("/" + resourcePath)) {
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
        }

        private String getContentType(String path) {
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".html")) return "text/html";
            return "application/octet-stream";
        }

        private void send404(HttpExchange exchange) throws IOException {
            byte[] response = "404 Not Found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }

        private void send405(HttpExchange exchange) throws IOException {
            byte[] response = "405 Method Not Allowed".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(405, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
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
            boolean hasInput = keyInput != null && !keyInput.isEmpty();

            // Process input through the terminal's line discipline
            if (hasInput) {
                String processedInput = component.pipe(keyInput);
                try {
                    processInputBytes(processedInput.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    // Terminal closed
                }
            }

            // Get terminal output - use longer timeout after input to let echo propagate,
            // and always force dump so the browser always gets the current screen state
            try {
                String output = dump(hasInput ? 100 : 10, true);
                sendResponse(exchange, output != null ? output : "");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendResponse(exchange, "");
            }
        }

        private Map<String, String> parseFormData(HttpExchange exchange) throws IOException {
            Map<String, String> params = new HashMap<>();

            byte[] bodyBytes;
            try (InputStream is = exchange.getRequestBody()) {
                bodyBytes = is.readAllBytes();
            }
            String formData = new String(bodyBytes, StandardCharsets.UTF_8);

            if (!formData.isEmpty()) {
                String[] pairs = formData.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=", 2);
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = keyValue.length == 2 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
                    params.put(key, value);
                }
            }

            return params;
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
        private final byte[] utf8Buf = new byte[4];
        private int utf8Pos;
        private int utf8Len;

        @Override
        public void write(int b) throws IOException {
            if (component == null) {
                return;
            }
            b &= 0xFF;
            if (utf8Pos == 0) {
                // Determine expected UTF-8 sequence length from the leading byte
                if (b < 0x80) {
                    component.write(String.valueOf((char) b));
                    return;
                } else if ((b & 0xE0) == 0xC0) {
                    utf8Len = 2;
                } else if ((b & 0xF0) == 0xE0) {
                    utf8Len = 3;
                } else if ((b & 0xF8) == 0xF0) {
                    utf8Len = 4;
                } else {
                    // Invalid leading byte or continuation byte on its own
                    component.write("\uFFFD");
                    return;
                }
            }
            utf8Buf[utf8Pos++] = (byte) b;
            if (utf8Pos == utf8Len) {
                String text = new String(utf8Buf, 0, utf8Len, StandardCharsets.UTF_8);
                component.write(text);
                utf8Pos = 0;
                utf8Len = 0;
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (component == null) {
                return;
            }
            // Flush any pending partial sequence first
            int i = 0;
            while (i < len && utf8Pos > 0) {
                write(b[off + i] & 0xFF);
                i++;
            }
            if (i >= len) {
                return;
            }
            // Find the last complete UTF-8 sequence boundary
            int end = off + len;
            int trailingIncomplete = 0;
            for (int j = end - 1; j >= off + i && (b[j] & 0xC0) == 0x80; j--) {
                trailingIncomplete++;
            }
            if (trailingIncomplete > 0 && end - 1 - trailingIncomplete >= off + i) {
                int leadByte = b[end - 1 - trailingIncomplete] & 0xFF;
                int expectedLen;
                if ((leadByte & 0xE0) == 0xC0) expectedLen = 2;
                else if ((leadByte & 0xF0) == 0xE0) expectedLen = 3;
                else if ((leadByte & 0xF8) == 0xF0) expectedLen = 4;
                else expectedLen = 1;
                if (trailingIncomplete + 1 < expectedLen) {
                    // Incomplete sequence at the end - buffer it
                    int incompleteStart = end - 1 - trailingIncomplete;
                    int completeLen = incompleteStart - (off + i);
                    if (completeLen > 0) {
                        component.write(new String(b, off + i, completeLen, StandardCharsets.UTF_8));
                    }
                    for (int j = incompleteStart; j < end; j++) {
                        utf8Buf[utf8Pos++] = b[j];
                    }
                    utf8Len = expectedLen;
                    return;
                }
            }
            // All complete - decode directly
            component.write(new String(b, off + i, len - i, StandardCharsets.UTF_8));
        }

        @Override
        public void flush() throws IOException {
            // No buffering beyond partial UTF-8 sequences
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
         * Delegates to the ScreenTerminal pipe method which handles
         * all special key sequences and terminal modes.
         *
         * @param input the input to process
         * @return the processed input
         */
        @Override
        public String pipe(String input) {
            return super.pipe(input);
        }

        /**
         * Dumps the terminal content as HTML with inline RGB color styles.
         *
         * @param timeout maximum time to wait for changes in milliseconds
         * @param forceUpdate whether to force an update even if screen is not dirty
         * @return the terminal content as HTML, or null if no update
         * @throws InterruptedException if interrupted
         */
        public String dump(long timeout, boolean forceUpdate) throws InterruptedException {
            int w = getWidth();
            int h = getHeight();
            long[] screen = new long[w * h];
            int[] cursor = new int[2];
            if (!dump(timeout, forceUpdate, screen, cursor)) {
                return null;
            }
            int cx = cursor[0];
            int cy = cursor[1];
            StringBuilder sb = new StringBuilder();
            long prevAttr = -1;
            sb.append("<div><pre class='term'>");
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    long d = screen[y * w + x];
                    int c = (int) (d & 0xffffffffL);
                    long a = d >> 32;
                    // Apply cursor styling
                    if (cy == y && cx == x) {
                        a = (a & 0xfffff000L) | 0x0fff; // white bg for cursor
                        a = a & ~0x0fff000L; // black fg for cursor
                    }
                    if (a != prevAttr) {
                        if (prevAttr != -1) {
                            sb.append("</span>");
                        }
                        sb.append(generateSpanTag(a));
                        prevAttr = a;
                    }
                    switch (c) {
                        case '&':
                            sb.append("&amp;");
                            break;
                        case '<':
                            sb.append("&lt;");
                            break;
                        case '>':
                            sb.append("&gt;");
                            break;
                        default:
                            if (c == 0) {
                                break; // wide char continuation
                            }
                            sb.appendCodePoint(c);
                            break;
                    }
                }
                sb.append("\n");
            }
            sb.append("</span></pre></div>");
            return sb.toString();
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
                + "        .term { margin: 0; }\n"
                + "    </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "    <div id=\"terminal\" tabindex=\"0\"></div>\n"
                + "    \n"
                + "    <script>\n"
                + "        var term = document.getElementById('terminal');\n"
                + "        var polling = false;\n"
                + "        \n"
                + "        function sendKey(key) {\n"
                + "            var xhr = new XMLHttpRequest();\n"
                + "            xhr.open('POST', '/terminal', true);\n"
                + "            xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');\n"
                + "            xhr.onload = function() {\n"
                + "                if (xhr.responseText) term.innerHTML = xhr.responseText;\n"
                + "            };\n"
                + "            xhr.send('k=' + encodeURIComponent(key));\n"
                + "        }\n"
                + "        \n"
                + "        function poll() {\n"
                + "            if (polling) return;\n"
                + "            polling = true;\n"
                + "            var xhr = new XMLHttpRequest();\n"
                + "            xhr.open('POST', '/terminal', true);\n"
                + "            xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');\n"
                + "            xhr.onload = function() {\n"
                + "                if (xhr.responseText) term.innerHTML = xhr.responseText;\n"
                + "                polling = false;\n"
                + "                setTimeout(poll, 100);\n"
                + "            };\n"
                + "            xhr.onerror = function() {\n"
                + "                polling = false;\n"
                + "                setTimeout(poll, 1000);\n"
                + "            };\n"
                + "            xhr.send('f=1');\n"
                + "        }\n"
                + "        \n"
                + "        document.addEventListener('keydown', function(e) {\n"
                + "            var key = '';\n"
                + "            switch (e.key) {\n"
                + "                case 'Enter':     key = '\\r'; break;\n"
                + "                case 'Backspace': key = '\\u007f'; break;\n"
                + "                case 'Tab':       key = '\\t'; break;\n"
                + "                case 'Escape':    key = '\\u001b'; break;\n"
                + "                case 'ArrowUp':   key = '~A'; break;\n"
                + "                case 'ArrowDown': key = '~B'; break;\n"
                + "                case 'ArrowRight':key = '~C'; break;\n"
                + "                case 'ArrowLeft': key = '~D'; break;\n"
                + "                case 'Home':      key = '~H'; break;\n"
                + "                case 'End':       key = '~F'; break;\n"
                + "                case 'PageUp':    key = '~1'; break;\n"
                + "                case 'PageDown':  key = '~2'; break;\n"
                + "                case 'Insert':    key = '~3'; break;\n"
                + "                case 'Delete':    key = '~4'; break;\n"
                + "                case 'F1':  key = '~a'; break;\n"
                + "                case 'F2':  key = '~b'; break;\n"
                + "                case 'F3':  key = '~c'; break;\n"
                + "                case 'F4':  key = '~d'; break;\n"
                + "                case 'F5':  key = '~e'; break;\n"
                + "                case 'F6':  key = '~f'; break;\n"
                + "                case 'F7':  key = '~g'; break;\n"
                + "                case 'F8':  key = '~h'; break;\n"
                + "                case 'F9':  key = '~i'; break;\n"
                + "                case 'F10': key = '~j'; break;\n"
                + "                case 'F11': key = '~k'; break;\n"
                + "                case 'F12': key = '~l'; break;\n"
                + "                default:\n"
                + "                    if (e.ctrlKey && e.key.length === 1) {\n"
                + "                        key = String.fromCharCode(e.key.charCodeAt(0) & 0x1f);\n"
                + "                    } else if (!e.ctrlKey && !e.altKey && !e.metaKey && e.key.length === 1) {\n"
                + "                        key = e.key;\n"
                + "                    }\n"
                + "                    break;\n"
                + "            }\n"
                + "            if (key) {\n"
                + "                e.preventDefault();\n"
                + "                sendKey(key);\n"
                + "            }\n"
                + "        });\n"
                + "        \n"
                + "        poll();\n"
                + "    </script>\n"
                + "</body>\n"
                + "</html>";
    }
}
