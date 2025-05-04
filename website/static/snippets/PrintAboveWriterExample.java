```java title="PrintAboveWriterExample.java"
Terminal terminal = TerminalBuilder.builder().build();
LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

// highlight-start
// Create a PrintAboveWriter
PrintAboveWriter writer = new PrintAboveWriter(reader);
// highlight-end

// Start a background thread to print messages
new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(1000);

                    // Create a styled message
                    AttributedStringBuilder asb = new AttributedStringBuilder();
                    asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                            .append("Notification #")
                            .append(String.valueOf(i))
                            .style(AttributedStyle.DEFAULT);

                    // Print the message above the current line
                    writer.write(asb.toAnsi(terminal));
                    writer.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        })
        .start();

// Read input normally
while (true) {
    String line = reader.readLine("prompt> ");
    System.out.println("You entered: " + line);
}
```