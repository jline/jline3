```java title="BasicCompletionExample.java"
public static void main(String[] args) throws IOException {
    // highlight-start
    // Create a simple completer with fixed options
    Completer completer = new StringsCompleter("help", "exit", "list", "version");
    // highlight-end

    // Create a line reader with the completer
    Terminal terminal = TerminalBuilder.builder().build();
    LineReader reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(completer)
            .build();

    System.out.println("Type a command and press Tab to see completions");
    // Now when the user presses Tab, they'll see the available commands
    String line = reader.readLine("prompt> ");
    System.out.println("You entered: " + line);
}
```