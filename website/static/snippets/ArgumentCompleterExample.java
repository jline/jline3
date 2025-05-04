```java title="ArgumentCompleterExample.java"
public static void main(String[] args) throws IOException {
    // First argument is a command, second is a file
    Completer commandCompleter = new StringsCompleter("open", "save", "delete");
    Completer fileCompleter = new FilesCompleter(Paths.get(System.getProperty("user.dir")));

    // highlight-start
    Completer argCompleter = new ArgumentCompleter(commandCompleter, fileCompleter);
    // highlight-end

    Terminal terminal = TerminalBuilder.builder().build();
    LineReader reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(argCompleter)
            .build();

    System.out.println("Type a command followed by a file path and press Tab");
    String line = reader.readLine("cmd> ");
    System.out.println("You entered: " + line);
}
```