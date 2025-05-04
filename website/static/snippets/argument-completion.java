```java title="TabCompletionExample.java"
// Create a more complex completer for command arguments
Completer argCompleter = new ArgumentCompleter(
        new StringsCompleter("open", "close", "save"),
        new FilesCompleter(Paths.get(System.getProperty("user.dir"))));

// Create a line reader with the argument completer
LineReader argReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .completer(argCompleter)
        .build();

System.out.println("Type 'open', 'close', or 'save' followed by a file path");
System.out.println("Press Tab to complete the file path");
String command = argReader.readLine("command> ");
System.out.println("You entered: " + command);
```