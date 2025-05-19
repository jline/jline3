---
title: Using Classpath Resources
---

# Using Classpath Resources

JLine provides support for loading configuration files and resources from the classpath. This is particularly useful for applications that want to provide default configurations bundled with the application JAR file.

## ClasspathResourceUtil

The `ClasspathResourceUtil` class provides utility methods for working with classpath resources:

```java
import org.jline.builtins.util.ClasspathResourceUtil;
import java.nio.file.Path;

// Get a resource from the classpath
Path resourcePath = ClasspathResourceUtil.getResourcePath("/nano/jnanorc");

// Get a resource using a specific class's classloader
Path resourcePath = ClasspathResourceUtil.getResourcePath("/nano/jnanorc", MyClass.class);

// Get a resource using a specific classloader
Path resourcePath = ClasspathResourceUtil.getResourcePath("/nano/jnanorc", myClassLoader);
```

## ConfigurationPath with Classpath Resources

The `ConfigurationPath` class can be configured to load resources from the classpath:

```java
import org.jline.builtins.ConfigurationPath;
import java.nio.file.Path;
import java.nio.file.Paths;

// Create a ConfigurationPath that looks for resources in the classpath
ConfigurationPath configPath = ConfigurationPath.fromClasspath("/nano");

// Or with both classpath and user-specific config
ConfigurationPath configPath = new ConfigurationPath(
    "/nano",                                          // classpath resource path
    Paths.get(System.getProperty("user.home"), ".myApp") // user-specific settings
);

// Get a configuration file
Path nanorcPath = configPath.getConfig("jnanorc");
```

## Using Classpath Resources with Nano

You can configure Nano to use configuration files from the classpath:

```java
import org.jline.builtins.Nano;
import org.jline.builtins.ConfigurationPath;
import org.jline.builtins.Options;
import org.jline.terminal.Terminal;

// Create a ConfigurationPath that looks for resources in the classpath
ConfigurationPath configPath = ConfigurationPath.fromClasspath("/nano");

// Parse command-line options
String[] argv = new String[] { "file.txt" };
Options opt = Options.compile(Nano.usage()).parse(argv);

// Create a Nano instance with the classpath configuration
Nano nano = new Nano(terminal, currentDir, opt, configPath);
```

## Using Classpath Resources with Less

Similarly, you can configure Less to use configuration files from the classpath:

```java
import org.jline.builtins.Less;
import org.jline.builtins.ConfigurationPath;
import org.jline.builtins.Options;
import org.jline.builtins.Source;
import org.jline.terminal.Terminal;

// Create a ConfigurationPath that looks for resources in the classpath
ConfigurationPath configPath = ConfigurationPath.fromClasspath("/less");

// Parse command-line options
String[] argv = new String[] { "file.txt" };
Options opt = Options.compile(Less.usage()).parse(argv);

// Create a Less instance with the classpath configuration
Less less = new Less(terminal, configPath);
less.run(opt, Source.create(Paths.get("file.txt")));
```

## Using Classpath Resources with SyntaxHighlighter

The `SyntaxHighlighter` class can load nanorc files directly from the classpath:

```java
import org.jline.builtins.SyntaxHighlighter;

// Load a nanorc file from the classpath
SyntaxHighlighter highlighter = SyntaxHighlighter.build("classpath:/nano/jnanorc");

// Or use a specific syntax
SyntaxHighlighter javaHighlighter = SyntaxHighlighter.build("classpath:/nano/java.nanorc");
```

## Bundling Resources in Your Application

To bundle nanorc files with your application, place them in your resources directory:

```
src/main/resources/
└── nano/
    ├── jnanorc
    ├── java.nanorc
    ├── xml.nanorc
    └── ...
```

Then, in your `pom.xml`, make sure these resources are included in your JAR:

```xml
<build>
    <resources>
        <resource>
            <directory>src/main/resources</directory>
        </resource>
    </resources>
</build>
```

This allows your application to access these resources at runtime, even when running from a JAR file.
