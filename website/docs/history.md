---
sidebar_position: 5
---

# History Management

JLine provides sophisticated history management capabilities, allowing users to recall, search, and reuse previous commands.

## Basic History Setup

To set up history in your JLine application:

```java title="HistorySetupExample.java"
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Paths;

public class HistorySetupExample {
    public static void main(String[] args) throws IOException {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().build();

        // highlight-start
        // Create a history instance
        History history = new DefaultHistory();

        // Create a line reader with history
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .history(history)
                .variable(LineReader.HISTORY_FILE, Paths.get("history.txt"))
                .build();
        // highlight-end

        System.out.println("Type commands and use up/down arrows to navigate history");
        // Now the user can navigate history with up/down arrows
        String line = reader.readLine("prompt> ");
        System.out.println("You entered: " + line);
    }
}
```

## Persistent History

JLine can save history to a file and load it when your application restarts:

```java title="PersistentHistoryExample.java"
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Paths;

public class PersistentHistoryExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        // highlight-start
        // Set the history file
        reader.setVariable(LineReader.HISTORY_FILE, Paths.get("~/.myapp_history"));
        // highlight-end

        // Use the reader...
        String line = reader.readLine("prompt> ");

        // Save history explicitly (though it's usually done automatically)
        ((DefaultHistory) reader.getHistory()).save();

        System.out.println("History saved to ~/.myapp_history");
    }
}
```

## History Size

You can control how many entries are kept in history:

```java title="HistorySizeExample.java"
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Paths;

public class HistorySizeExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // highlight-start
        // Configure history with size limits
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .variable(LineReader.HISTORY_FILE, Paths.get("~/.myapp_history"))
                .variable(LineReader.HISTORY_SIZE, 1000)         // Maximum entries in memory
                .variable(LineReader.HISTORY_FILE_SIZE, 2000)    // Maximum entries in file
                .build();
        // highlight-end

        System.out.println("History configured with size limits");
    }
}
```

## History Filtering

JLine provides options to filter what gets added to history:

```java title="HistoryFilteringExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class HistoryFilteringExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        // highlight-start
        // Don't add duplicate entries
        reader.setOption(LineReader.Option.HISTORY_IGNORE_DUPS, true);

        // Don't add entries that start with space
        reader.setOption(LineReader.Option.HISTORY_IGNORE_SPACE, true);
        // highlight-end

        // Beep when trying to navigate past the end of history
        reader.setOption(LineReader.Option.HISTORY_BEEP, true);

        // Verify history expansion (like !!, !$, etc.)
        reader.setOption(LineReader.Option.HISTORY_VERIFY, true);

        System.out.println("History filtering configured");
    }
}
```

## History Navigation

Users can navigate history using:

- **Up/Down arrows**: Move through history entries
- **Ctrl+R**: Reverse incremental search
- **Ctrl+S**: Forward incremental search (if supported by terminal)
- **Alt+&lt;**: Go to the first history entry
- **Alt+&gt;**: Go to the last history entry

## Programmatic History Access

You can access and manipulate history programmatically:

```java title="ProgrammaticHistoryAccessExample.java"
import org.jline.reader.History;
import org.jline.reader.LineReader;

public class ProgrammaticHistoryAccessExample {
    public void demonstrateHistoryAccess(LineReader reader) {
        // Get the history
        History history = reader.getHistory();

        // highlight-start
        // Iterate through history entries
        System.out.println("History entries:");
        for (History.Entry entry : history) {
            System.out.println(entry.index() + ": " + entry.line());
        }
        // highlight-end

        // Get a specific entry
        if (history.size() > 0) {
            String lastCommand = history.get(history.size() - 1);
            System.out.println("Last command: " + lastCommand);
        }

        // Add an entry programmatically
        history.add("manually added command");
        System.out.println("Added command to history");

        // Clear history (commented out to avoid actually clearing history)
        // history.purge();
    }
}
```

## History Expansion

JLine supports history expansion similar to Bash:

```java title="HistoryExpansionExample.java"
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class HistoryExpansionExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // highlight-start
        // Enable history expansion
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .option(LineReader.Option.HISTORY_EXPAND, true)
                .build();
        // highlight-end

        System.out.println("History expansion enabled. You can use:");
        System.out.println("!! - repeat the last command");
        System.out.println("!n - repeat command number n");
        System.out.println("!-n - repeat nth previous command");
        System.out.println("!string - repeat last command starting with string");
        System.out.println("!?string - repeat last command containing string");
        System.out.println("^string1^string2 - replace string1 with string2 in the last command");

        String line = reader.readLine("prompt> ");
        System.out.println("You entered: " + line);
    }
}
```

## Custom History Implementation

You can create your own history implementation by implementing the `History` interface:

```java title="CustomHistory.java" showLineNumbers
import org.jline.reader.History;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CustomHistory implements History {
    private final List<String> entries = new ArrayList<>();

    @Override
    public void add(String line) {
        // highlight-start
        // Custom logic for adding entries
        entries.add(line);
        // Maybe save to a database or other storage
        // highlight-end
    }

    @Override
    public String get(int index) {
        return entries.get(index);
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public int index() {
        return entries.size() - 1;
    }

    @Override
    public Iterator<Entry> iterator() {
        return new Iterator<Entry>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < entries.size();
            }

            @Override
            public Entry next() {
                final int currentIndex = index++;
                return new Entry() {
                    @Override
                    public int index() {
                        return currentIndex;
                    }

                    @Override
                    public String line() {
                        return entries.get(currentIndex);
                    }
                };
            }
        };
    }

    @Override
    public void purge() {
        entries.clear();
    }
}
```

## Advanced History Features

### Timestamped History

You can create a history implementation that records timestamps:

```java title="TimestampedHistory.java"
import org.jline.reader.impl.history.DefaultHistory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class TimestampedHistory extends DefaultHistory {
    private final Map<String, Instant> timestamps = new HashMap<>();

    @Override
    public void add(String line) {
        // highlight-start
        super.add(line);
        timestamps.put(line, Instant.now());
        // highlight-end
    }

    public Instant getTimestamp(String line) {
        return timestamps.get(line);
    }

    public String getFormattedTimestamp(String line) {
        Instant timestamp = timestamps.get(line);
        if (timestamp != null) {
            return timestamp.toString();
        }
        return "Unknown";
    }
}
```

### Searchable History

Implement custom search functionality:

```java title="HistorySearchExample.java"
import org.jline.reader.History;
import org.jline.reader.LineReader;

import java.util.ArrayList;
import java.util.List;

public class HistorySearchExample {
    // highlight-start
    public List<String> searchHistory(LineReader reader, String term) {
        List<String> results = new ArrayList<>();
        History history = reader.getHistory();

        for (History.Entry entry : history) {
            if (entry.line().contains(term)) {
                results.add(entry.line());
            }
        }
        // highlight-end

        return results;
    }

    public void demonstrateHistorySearch(LineReader reader) {
        System.out.println("Searching history for 'git':");
        List<String> gitCommands = searchHistory(reader, "git");

        for (String command : gitCommands) {
            System.out.println(" - " + command);
        }
    }
}
```

### History Event Listeners

You can listen for history events:

```java title="HistoryListenerExample.java"
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class HistoryListenerExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Create a history listener
        History.Listener historyListener = new History.Listener() {
            @Override
            public void onAdd(History history, String line) {
                System.out.println("Added to history: " + line);
            }

            @Override
            public void onRemove(History history, String line) {
                System.out.println("Removed from history: " + line);
            }
        };
        // highlight-end

        // Add the listener to a DefaultHistory instance
        DefaultHistory history = new DefaultHistory();
        history.addListener(historyListener);

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .history(history)
                .build();

        System.out.println("Type commands to see history events:");
        String line = reader.readLine("prompt> ");
        System.out.println("You entered: " + line);
    }
}
```

## Best Practices

- Always set a history file for persistent history
- Configure appropriate history size limits
- Consider enabling HISTORY_IGNORE_DUPS to avoid clutter
- Provide clear documentation on history navigation for users
- Consider security implications of storing sensitive commands
- Implement history purging for sensitive operations
- Test history functionality with various input patterns
