```java title="LineReaderExample.java"
// Read a line of input
String line = reader.readLine("prompt> ");
System.out.println("You entered: " + line);

// Read a password (input will be masked)
String password = reader.readLine("Password: ", '*');
System.out.println("Password length: " + password.length());
```