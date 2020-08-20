<img src="./doc/ConsoleUI-Logo.png" width="200"  align="right" alt="ConsoleUI logo">

[![Build Status](https://travis-ci.org/awegmann/consoleui.svg?branch=master)](https://travis-ci.org/awegmann/consoleui)

# ConsoleUI

[![Join the chat at https://gitter.im/awegmann/consoleui](https://badges.gitter.im/awegmann/consoleui.svg)](https://gitter.im/awegmann/consoleui?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Tiny java library that provides simple UI elements on ANSI console based terminals. ConsoleUI is inspired by 
[Inquirer.js](https://github.com/SBoudrias/Inquirer.js) which is written in JavaScript.

# Intention

I was impressed by JavaScript based Yeoman which leads the user through the process of creating new projects
by querying with a simple user interface on the console. An investigation how this is done, brought 
me to Inquirer.js which implements a very simple and intuitive set of controls for checkbox, list and text input.

Because I didn't found anything comparable to this in the Java eco system, I decided to write `Console UI`
as a library with the same easy 'look and feel'. Some parts of the API are also comparable, but Console UI is not
a Java clone of Inquirer.js.

# Features

 Console UI currently supports:

 - Text input with completion and GNU ReadLine compatible editing
 - Checkboxes
 - Lists
 - Expandable Choices (multiple key based answers for a question with help and optional list navigation)
 - Yes/No-Questions

A screen recording of the basic elements demo can be fund on Youtube [console UI demo](https://youtu.be/6dB3CyOX9rU).

# Dependencies

Console UI uses jansi and jline for the dirty console things.

# Maven artefact

ConsoleUI releases are available at Maven Central [de.codeshelf.consoleui » consoleui](https://search.maven.org/artifact/de.codeshelf.consoleui/consoleui)

# Test Run

You can get an idea how the project works by looking at `de.codeshelf.consoleui.Basic`.  
You can run this by executing the following from the project root:

    gradlew fatJar 
    java -jar build/libs/consoleui-all-0.0.10.jar     # <- replace with the latest version

# Usage

*Hint: see the [how to](doc/howto.md) to get a more detailed documentation how to use ConsoleUI.*


Before you can use ConsoleUI the AnsiConsole library has to be initialized.

    AnsiConsole.systemInstall();

Entry point to the builder classes is to create a new object of type `ConsolePrompt`.
    
    ConsolePrompt prompt = new ConsolePrompt();

From the prompt object, use the `getPromptBuilder()` method to create the builder for all subsequent UI elements 
you want to use.
    
    PromptBuilder promptBuilder = prompt.getPromptBuilder();

From with this `PromptBuilder` you can access UI builder with the following methods:

- createCheckboxPrompt()
    * creates a checkbox prompt. This prompt lets the user choose any number of items of a given list.
- createChoicePrompt()
    * creates a choice prompt. This prompt lets the user choose one from a given number of possible answers.     
- createConfirmPromp()
    * creates a confirm prompt. This prompt lets the user answer with 'yes' or 'no' to a given question.
- createInputPrompt()
    * creates a input prompt. This prompt is a classic entry line like a shell. Because of the underlying readline
      implementation it offers you to provide completers (like file name completer or string completer). In addition
      to his, you can define a mask character which is printed on the screen instead of the typed keys like used
      for hidden password entry.
- createListPrompt()
    * creates a list prompt. This prompt lets the user choose one item from a given list.


# Changes

### Version 0.0.13

- Fixed bug #22: lists are not rendered correctly  

### Version 0.0.12

- Fixed Bug #20: Lists higher than the terminal height were not handled correctly. 
  ConsoleUI now supports scrolling for checkbox promt and list prompt.
  To configure the height of the view port, either an absolute number of lines or a fraction (percentage) of the 
  screen height can be defined.



