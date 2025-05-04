```java title="LineReaderInputExample.java"
public void demonstrateInput(LineReader reader) {
    // Read a line with a prompt
    String line = reader.readLine("prompt> ");
    System.out.println("You entered: " + line);

    // highlight-start
    // Read a line with a right prompt (displayed at the right edge)
    String lineWithRightPrompt = reader.readLine("prompt> ", "right prompt", (Character) null, null);
    System.out.println("You entered: " + lineWithRightPrompt);
    // highlight-end

    // Read a masked line (for passwords)
    String password = reader.readLine("Password: ", '*');
    System.out.println("Password accepted");
}
```