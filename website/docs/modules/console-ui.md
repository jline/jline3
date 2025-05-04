---
sidebar_position: 5
---

# JLine Console UI

The `jline-console-ui` module provides UI components for console applications, such as progress bars, spinners, tables, trees, forms, and wizards. These components help you create more interactive and user-friendly command-line interfaces.

## Maven Dependency

To use the console-ui module, add the following dependency to your project:

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-console-ui</artifactId>
    <version>3.29.0</version>
</dependency>
```

## Progress Bars

The console-ui module provides progress bars to show the status of long-running operations:

```java title="ProgressBarExample.java" showLineNumbers
import org.jline.console.ui.ProgressBar;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ProgressBarExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Clear screen
        terminal.puts(Capability.clear_screen);

        // highlight-start
        // Create a progress bar
        ProgressBar progressBar = new ProgressBar(terminal, "Processing", 100);
        progressBar.setLeftBracket("[");
        progressBar.setRightBracket("]");
        progressBar.setFiller("=");
        progressBar.setRemaining(" ");
        // highlight-end

        // Start the progress bar
        progressBar.start();

        // Simulate work
        for (int i = 0; i <= 100; i++) {
            progressBar.update(i);
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Complete the progress bar
        progressBar.complete();

        terminal.writer().println("\nOperation completed successfully!");
        terminal.flush();
    }
}
```

## Spinners

Spinners are useful for indicating activity when you can't measure progress:

```java title="SpinnerExample.java"
import org.jline.console.ui.Spinner;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SpinnerExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Clear screen
        terminal.puts(Capability.clear_screen);

        // highlight-start
        // Create a spinner
        Spinner spinner = new Spinner(terminal, "Loading", Spinner.Style.DOTS);

        // Start the spinner
        spinner.start();
        // highlight-end

        // Simulate work
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Stop the spinner
        spinner.stop();

        terminal.writer().println("\nLoading complete!");
        terminal.flush();
    }
}
```

## Tables

The console-ui module provides a table component for displaying tabular data:

```java title="TableExample.java" showLineNumbers
import org.jline.console.ui.Table;
import org.jline.console.ui.Table.ColumnType;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TableExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // highlight-start
        // Create a table
        Table table = new Table();
        table.setHeader(Arrays.asList("ID", "Name", "Role", "Salary"));
        table.setColumnTypes(Arrays.asList(ColumnType.Number, ColumnType.String, ColumnType.String, ColumnType.Number));

        // Add rows
        table.addRow(Arrays.asList("1", "John Doe", "Developer", "75000"));
        table.addRow(Arrays.asList("2", "Jane Smith", "Manager", "85000"));
        table.addRow(Arrays.asList("3", "Bob Johnson", "Designer", "65000"));
        // highlight-end

        // Style the header
        table.setHeaderStyle(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).bold());

        // Style specific columns
        table.setColumnStyle(0, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        table.setColumnStyle(3, AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));

        // Display the table
        AttributedString tableString = table.render(terminal.getWidth());
        tableString.println(terminal);
        terminal.flush();
    }
}
```

## Trees

The console-ui module provides a tree component for displaying hierarchical data:

```java title="TreeExample.java" showLineNumbers
import org.jline.console.ui.Tree;
import org.jline.console.ui.Tree.Node;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import java.io.IOException;

public class TreeExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // highlight-start
        // Create a tree
        Tree tree = new Tree();

        // Create nodes
        Node root = tree.createNode("Project");

        Node src = tree.createNode("src");
        Node main = tree.createNode("main");
        Node java = tree.createNode("java");
        Node resources = tree.createNode("resources");

        Node test = tree.createNode("test");
        Node testJava = tree.createNode("java");
        Node testResources = tree.createNode("resources");

        Node docs = tree.createNode("docs");

        // Build the tree structure
        root.addChild(src);
        src.addChild(main);
        main.addChild(java);
        main.addChild(resources);

        src.addChild(test);
        test.addChild(testJava);
        test.addChild(testResources);

        root.addChild(docs);
        // highlight-end

        // Style nodes
        root.setStyle(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).bold());
        java.setStyle(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        testJava.setStyle(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));

        // Display the tree
        AttributedString treeString = tree.render(terminal.getWidth());
        treeString.println(terminal);
        terminal.flush();
    }
}
```

## Forms

The console-ui module provides a form component for collecting user input:

```java title="FormExample.java" showLineNumbers
import org.jline.console.ui.Form;
import org.jline.console.ui.Form.Field;
import org.jline.console.ui.Form.FieldType;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class FormExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        // highlight-start
        // Create a form
        Form form = new Form();
        form.setTitle("User Registration");

        // Add fields
        form.addField(new Field("username", "Username", FieldType.TEXT)
                .setRequired(true)
                .setDescription("Enter your username (3-20 characters)"));

        form.addField(new Field("email", "Email", FieldType.EMAIL)
                .setRequired(true)
                .setDescription("Enter your email address"));

        form.addField(new Field("password", "Password", FieldType.PASSWORD)
                .setRequired(true)
                .setDescription("Enter a strong password"));

        form.addField(new Field("role", "Role", FieldType.SELECT)
                .setOptions(Arrays.asList("User", "Admin", "Guest"))
                .setDefaultValue("User")
                .setDescription("Select your role"));

        form.addField(new Field("bio", "Biography", FieldType.TEXTAREA)
                .setDescription("Tell us about yourself"));

        form.addField(new Field("newsletter", "Subscribe to newsletter", FieldType.CHECKBOX)
                .setDefaultValue("true")
                .setDescription("Receive updates via email"));
        // highlight-end

        // Display the form and collect input
        Map<String, String> values = form.display(reader);

        // Show the collected values
        terminal.writer().println("\nForm submitted with the following values:");
        values.forEach((key, value) -> terminal.writer().println(key + ": " + value));
        terminal.flush();
    }
}
```

## Wizards

The console-ui module provides a wizard component for guiding users through multi-step processes:

```java title="WizardExample.java" showLineNumbers
import org.jline.console.ui.Wizard;
import org.jline.console.ui.Wizard.Page;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WizardExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        // highlight-start
        // Create a wizard
        Wizard wizard = new Wizard();
        wizard.setTitle("Installation Wizard");

        // Create pages
        Page welcomePage = new Page("welcome", "Welcome")
                .setContent("Welcome to the installation wizard. This wizard will guide you through the installation process.")
                .addAction("next", "Continue", "Go to the next page");

        Page licensePage = new Page("license", "License Agreement")
                .setContent("Please read the license agreement carefully.\n\n" +
                        "This is a sample license agreement. In a real application, you would include the actual license text here.")
                .addAction("back", "Back", "Go back to the previous page")
                .addAction("accept", "Accept", "Accept the license agreement")
                .addAction("decline", "Decline", "Decline the license agreement and exit");

        Page configPage = new Page("config", "Configuration")
                .setContent("Please configure the installation options.")
                .addField("installDir", "Installation Directory", "/usr/local/app")
                .addField("port", "Port Number", "8080")
                .addCheckbox("startService", "Start service after installation", true)
                .addAction("back", "Back", "Go back to the previous page")
                .addAction("next", "Next", "Go to the next page");

        Page summaryPage = new Page("summary", "Summary")
                .setContent("Please review your installation settings.")
                .addAction("back", "Back", "Go back to the previous page")
                .addAction("install", "Install", "Begin the installation");

        Page finishPage = new Page("finish", "Installation Complete")
                .setContent("The installation has been completed successfully.")
                .addAction("finish", "Finish", "Exit the wizard");

        // Add pages to the wizard
        wizard.addPage(welcomePage);
        wizard.addPage(licensePage);
        wizard.addPage(configPage);
        wizard.addPage(summaryPage);
        wizard.addPage(finishPage);
        // highlight-end

        // Run the wizard
        Map<String, Object> context = new HashMap<>();
        String result = wizard.run(reader, context);

        // Show the result
        terminal.writer().println("\nWizard completed with result: " + result);
        terminal.writer().println("\nCollected values:");
        context.forEach((key, value) -> terminal.writer().println(key + ": " + value));
        terminal.flush();
    }
}
```

## Menus

The console-ui module provides a menu component for displaying options:

```java title="MenuExample.java"
import org.jline.console.ui.Menu;
import org.jline.console.ui.Menu.Item;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;

public class MenuExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        // highlight-start
        // Create a menu
        Menu menu = new Menu();
        menu.setTitle("Main Menu");

        // Add items
        menu.addItem(new Item("file", "File Operations")
                .setDescription("Perform file operations"));

        menu.addItem(new Item("edit", "Edit")
                .setDescription("Edit files"));

        menu.addItem(new Item("view", "View")
                .setDescription("View files"));

        menu.addItem(new Item("help", "Help")
                .setDescription("Get help"));

        menu.addItem(new Item("exit", "Exit")
                .setDescription("Exit the application"));
        // highlight-end

        // Style the menu
        menu.setTitleStyle(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).bold());
        menu.setSelectedStyle(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold());

        // Display the menu and get selection
        String selection = menu.display(reader);

        // Handle the selection
        terminal.writer().println("\nYou selected: " + selection);
        terminal.flush();
    }
}
```

## Notifications

The console-ui module provides a notification component for displaying messages:

```java title="NotificationExample.java"
import org.jline.console.ui.Notification;
import org.jline.console.ui.Notification.Type;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class NotificationExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // highlight-start
        // Create notifications
        Notification infoNotification = new Notification(Type.INFO, "Information", "This is an informational message.");
        Notification warningNotification = new Notification(Type.WARNING, "Warning", "This is a warning message.");
        Notification errorNotification = new Notification(Type.ERROR, "Error", "This is an error message.");
        Notification successNotification = new Notification(Type.SUCCESS, "Success", "This is a success message.");
        // highlight-end

        // Display notifications
        infoNotification.display(terminal);
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        warningNotification.display(terminal);
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        errorNotification.display(terminal);
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        successNotification.display(terminal);

        terminal.writer().println("\nAll notifications displayed.");
        terminal.flush();
    }
}
```

## Combining UI Components

You can combine multiple UI components to create a more sophisticated interface:

```java title="CombinedUIExample.java" showLineNumbers
import org.jline.console.ui.Menu;
import org.jline.console.ui.Menu.Item;
import org.jline.console.ui.Notification;
import org.jline.console.ui.Notification.Type;
import org.jline.console.ui.ProgressBar;
import org.jline.console.ui.Spinner;
import org.jline.console.ui.Table;
import org.jline.console.ui.Table.ColumnType;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class CombinedUIExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        // Clear screen
        terminal.puts(Capability.clear_screen);

        // Create a menu
        Menu menu = new Menu();
        menu.setTitle("File Operations");
        menu.addItem(new Item("list", "List Files"));
        menu.addItem(new Item("copy", "Copy Files"));
        menu.addItem(new Item("delete", "Delete Files"));
        menu.addItem(new Item("exit", "Exit"));

        // Main application loop
        while (true) {
            // Display the menu
            String selection = menu.display(reader);

            if ("exit".equals(selection)) {
                break;
            }

            // Handle the selection
            switch (selection) {
                case "list":
                    // Show a spinner while "loading" files
                    Spinner spinner = new Spinner(terminal, "Loading files", Spinner.Style.DOTS);
                    spinner.start();

                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    spinner.stop();

                    // Display files in a table
                    Table table = new Table();
                    table.setHeader(Arrays.asList("Name", "Size", "Modified"));
                    table.setColumnTypes(Arrays.asList(ColumnType.String, ColumnType.Number, ColumnType.String));

                    table.addRow(Arrays.asList("file1.txt", "1024", "2023-05-15"));
                    table.addRow(Arrays.asList("file2.txt", "2048", "2023-05-16"));
                    table.addRow(Arrays.asList("file3.txt", "4096", "2023-05-17"));

                    table.render(terminal.getWidth()).println(terminal);
                    break;

                case "copy":
                    // Show a progress bar for copying
                    ProgressBar progressBar = new ProgressBar(terminal, "Copying files", 100);
                    progressBar.start();

                    for (int i = 0; i <= 100; i++) {
                        progressBar.update(i);
                        try {
                            TimeUnit.MILLISECONDS.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    progressBar.complete();

                    // Show a success notification
                    new Notification(Type.SUCCESS, "Copy Complete", "Files copied successfully.").display(terminal);
                    break;

                case "delete":
                    // Show a warning notification
                    new Notification(Type.WARNING, "Delete Files", "This operation cannot be undone.").display(terminal);

                    // Ask for confirmation
                    String confirm = reader.readLine("Are you sure you want to delete the files? (y/n): ");

                    if ("y".equalsIgnoreCase(confirm)) {
                        // Show a progress bar for deleting
                        ProgressBar deleteBar = new ProgressBar(terminal, "Deleting files", 100);
                        deleteBar.start();

                        for (int i = 0; i <= 100; i++) {
                            deleteBar.update(i);
                            try {
                                TimeUnit.MILLISECONDS.sleep(30);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        deleteBar.complete();

                        // Show a success notification
                        new Notification(Type.SUCCESS, "Delete Complete", "Files deleted successfully.").display(terminal);
                    } else {
                        // Show an info notification
                        new Notification(Type.INFO, "Delete Cancelled", "Operation cancelled by user.").display(terminal);
                    }
                    break;
            }

            // Wait for user to press Enter before showing the menu again
            reader.readLine("\nPress Enter to continue...");
            terminal.puts(Capability.clear_screen);
        }

        terminal.writer().println("Goodbye!");
        terminal.flush();
    }
}
```

## Best Practices

When using the JLine console-ui module, consider these best practices:

1. **Use Appropriate UI Components**: Choose the right component for each task to provide the best user experience.

2. **Provide Clear Feedback**: Use progress bars, spinners, and notifications to keep users informed about what's happening.

3. **Style Components Consistently**: Use consistent styling across all UI components to create a cohesive look and feel.

4. **Handle Terminal Resizing**: Make sure your UI components adapt to changes in terminal size.

5. **Consider Terminal Capabilities**: Some terminals may not support all UI features, so check capabilities before using advanced components.

6. **Provide Keyboard Navigation**: Ensure that all UI components can be navigated using the keyboard.

7. **Use Descriptive Labels**: Provide clear labels and descriptions for all UI elements.

8. **Handle Errors Gracefully**: Display appropriate error messages when something goes wrong.

9. **Combine Components Thoughtfully**: When combining multiple UI components, make sure they work well together and don't overwhelm the user.

10. **Test on Different Terminals**: Test your UI on different terminal emulators to ensure compatibility.
