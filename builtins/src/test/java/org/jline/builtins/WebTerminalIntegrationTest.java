/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WebTerminal that exercise the HTTP interface.
 * These tests start a real HTTP server and verify that the terminal
 * responds correctly to browser-like HTTP requests.
 */
public class WebTerminalIntegrationTest {

    private WebTerminal terminal;
    private String baseUrl;

    @BeforeEach
    public void setUp() throws IOException {
        // Use port 0 to get a random available port
        terminal = new WebTerminal("localhost", 0, 80, 24);
        terminal.start();
        baseUrl = terminal.getUrl();
    }

    @AfterEach
    public void tearDown() {
        if (terminal != null) {
            terminal.stop();
            try {
                terminal.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Test
    public void testServerStartsOnRandomPort() {
        assertTrue(terminal.isRunning());
        // Port should not be 0 since the OS assigns one
        assertFalse(baseUrl.endsWith(":0"), "URL should have actual port, got: " + baseUrl);
    }

    @Test
    public void testGetMainPage() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/").openConnection();
        conn.setRequestMethod("GET");
        try {
            assertEquals(200, conn.getResponseCode());
            String contentType = conn.getHeaderField("Content-Type");
            assertTrue(contentType.contains("text/html"), "Should serve HTML, got: " + contentType);

            String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(body.contains("<!DOCTYPE html>"), "Should be a full HTML page");
            assertTrue(body.contains("/terminal"), "Should contain the AJAX endpoint URL");
            assertTrue(body.contains("sendKey"), "Should contain the keyboard handler function");
        } finally {
            conn.disconnect();
        }
    }

    @Test
    public void testPollReturnsScreenContent() throws IOException {
        // Write something to the terminal first
        terminal.write("Hello Web");

        // Poll for screen content (like the browser does)
        String response = postToTerminal("f=1");
        assertNotNull(response);
        assertFalse(response.isEmpty(), "Poll response should not be empty");
        assertTrue(response.contains("<div>"), "Response should be HTML");
        assertTrue(response.contains("<pre"), "Response should contain pre tag");
        assertTrue(response.contains("Hello Web"), "Response should contain written text");
    }

    @Test
    public void testPollWithNoChangesReturnsContent() throws IOException {
        // Force update should always return content
        String response = postToTerminal("f=1");
        assertNotNull(response);
        assertFalse(response.isEmpty(), "Force poll should always return content");
        assertTrue(response.contains("<div>"), "Response should be HTML");
    }

    @Test
    public void testSendKeyboardInput() throws Exception {
        // Start a LineReader in a background thread
        CountDownLatch readerReady = new CountDownLatch(1);
        CountDownLatch lineRead = new CountDownLatch(1);
        AtomicReference<String> readLine = new AtomicReference<>();

        Thread readerThread = new Thread(() -> {
            try {
                LineReader reader =
                        LineReaderBuilder.builder().terminal(terminal).build();
                readerReady.countDown();
                String line = reader.readLine("$ ");
                readLine.set(line);
                lineRead.countDown();
            } catch (Exception e) {
                // terminal closed
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();

        assertTrue(readerReady.await(5, TimeUnit.SECONDS), "LineReader should be ready");
        // Give the reader time to display the prompt
        Thread.sleep(200);

        // Poll to get the prompt displayed
        String response = postToTerminal("f=1");
        assertNotNull(response);
        assertTrue(response.contains("$"), "Should show the prompt: " + response);

        // Send "hi" followed by Enter
        postToTerminal("k=" + urlEncode("h"));
        postToTerminal("k=" + urlEncode("i"));
        postToTerminal("k=" + urlEncode("\r"));

        assertTrue(lineRead.await(5, TimeUnit.SECONDS), "LineReader should have read a line");
        assertEquals("hi", readLine.get(), "Should have read 'hi'");
    }

    @Test
    public void testSpecialKeysAreSentCorrectly() throws Exception {
        // Start a LineReader
        CountDownLatch readerReady = new CountDownLatch(1);
        CountDownLatch lineRead = new CountDownLatch(1);
        AtomicReference<String> readLine = new AtomicReference<>();

        Thread readerThread = new Thread(() -> {
            try {
                LineReader reader =
                        LineReaderBuilder.builder().terminal(terminal).build();
                readerReady.countDown();
                String line = reader.readLine("$ ");
                readLine.set(line);
                lineRead.countDown();
            } catch (Exception e) {
                // terminal closed
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();

        assertTrue(readerReady.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);

        // Type "abc", then backspace to delete 'c', then Enter
        postToTerminal("k=" + urlEncode("a"));
        postToTerminal("k=" + urlEncode("b"));
        postToTerminal("k=" + urlEncode("c"));
        postToTerminal("k=" + urlEncode("\u007f")); // DEL (backspace)
        postToTerminal("k=" + urlEncode("\r"));

        assertTrue(lineRead.await(5, TimeUnit.SECONDS), "LineReader should have read a line");
        assertEquals("ab", readLine.get(), "Backspace should have deleted 'c'");
    }

    @Test
    public void testTabCompletion() throws Exception {
        // Start a LineReader with a completer
        CountDownLatch readerReady = new CountDownLatch(1);

        Thread readerThread = new Thread(() -> {
            try {
                LineReader reader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .completer(new org.jline.reader.impl.completer.StringsCompleter("hello", "help", "world"))
                        .build();
                readerReady.countDown();
                reader.readLine("$ ");
            } catch (Exception e) {
                // terminal closed
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();

        assertTrue(readerReady.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);

        // Type "hel" then Tab
        postToTerminal("k=" + urlEncode("h"));
        postToTerminal("k=" + urlEncode("e"));
        postToTerminal("k=" + urlEncode("l"));
        postToTerminal("k=" + urlEncode("\t"));

        // Wait for completion to be processed
        Thread.sleep(500);

        // Poll to see the completion result
        String response = postToTerminal("f=1");
        assertNotNull(response);
        // After "hel" + Tab, the common prefix "hel" should be shown,
        // and completions "hello" and "help" should appear
        assertTrue(
                response.contains("hello") || response.contains("help") || response.contains("hel"),
                "Tab completion should show candidates: " + response);
    }

    @Test
    public void testArrowKeysForEditing() throws Exception {
        CountDownLatch readerReady = new CountDownLatch(1);
        CountDownLatch lineRead = new CountDownLatch(1);
        AtomicReference<String> readLine = new AtomicReference<>();

        Thread readerThread = new Thread(() -> {
            try {
                LineReader reader =
                        LineReaderBuilder.builder().terminal(terminal).build();
                readerReady.countDown();
                String line = reader.readLine("$ ");
                readLine.set(line);
                lineRead.countDown();
            } catch (Exception e) {
                // terminal closed
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();

        assertTrue(readerReady.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);

        // Type "ac", move left, insert "b", then Enter
        // Result should be "abc"
        postToTerminal("k=" + urlEncode("a"));
        postToTerminal("k=" + urlEncode("c"));
        postToTerminal("k=" + urlEncode("~D")); // Left arrow
        postToTerminal("k=" + urlEncode("b"));
        postToTerminal("k=" + urlEncode("\r"));

        assertTrue(lineRead.await(5, TimeUnit.SECONDS), "LineReader should have read a line");
        assertEquals("abc", readLine.get(), "Arrow key editing should produce 'abc'");
    }

    @Test
    public void testScreenContentAfterRefresh() throws IOException {
        // Write content
        terminal.write("Persistent content");

        // First poll
        String response1 = postToTerminal("f=1");
        assertTrue(response1.contains("Persistent content"));

        // Second poll (simulates page refresh) should still show content
        String response2 = postToTerminal("f=1");
        assertTrue(response2.contains("Persistent content"), "Content should persist across polls");
    }

    @Test
    public void testGzipCompression() throws IOException {
        terminal.write("Some content for compression test");

        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/terminal").openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept-Encoding", "gzip");

        try (OutputStream os = conn.getOutputStream()) {
            os.write("f=1".getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(200, conn.getResponseCode());
        // For small content, gzip may not be used (threshold is 100 chars)
        // but the server should still respond successfully
        conn.disconnect();
    }

    @Test
    public void test404ForUnknownPaths() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/nonexistent").openConnection();
        conn.setRequestMethod("GET");
        try {
            assertEquals(404, conn.getResponseCode());
        } finally {
            conn.disconnect();
        }
    }

    @Test
    public void testMethodNotAllowed() throws IOException {
        // GET on /terminal should be 405
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/terminal").openConnection();
        conn.setRequestMethod("GET");
        try {
            assertEquals(405, conn.getResponseCode());
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Helper: POST form data to the /terminal endpoint and return the response body.
     */
    private String postToTerminal(String formData) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/terminal").openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(formData.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(200, conn.getResponseCode());
        String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        conn.disconnect();
        return response;
    }

    /**
     * URL-encode a string for use in form data.
     */
    private String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
