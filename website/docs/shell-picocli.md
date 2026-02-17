---
sidebar_position: 9
title: "Shell: Picocli Integration"
---

# Picocli Integration

The `PicocliCommandRegistry` implements both `CommandRegistry` (old API) and `CommandGroup` (new API), so picocli commands work with both the old and new shell infrastructure.

## Basic Usage

<CodeSnippet name="PicocliJLineExample" />

## How It Works

`PicocliCommandRegistry` wraps a picocli `CommandLine` and:

1. Exposes each subcommand as a JLine `Command`
2. Provides tab completion from picocli's option and subcommand definitions
3. Generates `CommandDescription` for TailTipWidgets
4. Delegates execution to picocli's `CommandLine.execute()`

## Maven Dependencies

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-picocli</artifactId>
    <version>${jline.version}</version>
</dependency>
<dependency>
    <groupId>info.picocli</groupId>
    <artifactId>picocli</artifactId>
    <version>${picocli.version}</version>
</dependency>
```
