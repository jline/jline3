```java title="LineReaderCreationExample.java"
public static void main(String[] args) throws IOException {
    // Create a terminal
    Terminal terminal = TerminalBuilder.builder().build();

    // highlight-start
    // Create a basic line reader
    LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
    // highlight-end

    // Create a line reader with custom configuration
    LineReader customReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .appName("MyApp")
            .variable(LineReader.HISTORY_FILE, Paths.get("history.txt"))
            .option(LineReader.Option.AUTO_FRESH_LINE, true)
            .option(LineReader.Option.HISTORY_BEEP, false)
            .build();
}
```