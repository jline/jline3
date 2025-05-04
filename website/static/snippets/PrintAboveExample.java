```java title="PrintAboveExample.java"
Terminal terminal = TerminalBuilder.builder().build();
LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

// Start a background thread to print messages
new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(1000);
                    // highlight-next-line
This line prints a message above the current input line
                    reader.printAbove("Notification #" + i);
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