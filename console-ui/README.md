<img src="./doc/ConsoleUI-Logo.png" width="200"  align="right" alt="ConsoleUI logo">

# ConsoleUI

Tiny java library that provides simple UI elements on ANSI console based terminals. ConsoleUI is inspired by 
[Inquirer.js](https://github.com/SBoudrias/Inquirer.js) which is written in JavaScript.

ConsoleUI has been initially implemented using JLine2 by Andreas Wegmann. After ConsoleUI has been upgraded to use JLine3 
it has been merged into JLine3.

# Intention

I was impressed by JavaScript based Yeoman which leads the user through the process of creating new projects
by querying with a simple user interface on the console. An investigation how this is done, brought 
me to Inquirer.js which implements a very simple and intuitive set of controls for checkbox, list and text input.

Because I didn't find anything comparable to this in the Java ecosystem, I decided to write `Console UI`
as a library with the same easy 'look and feel'. Some parts of the API are also comparable, but Console UI is not
a Java clone of Inquirer.js.

# Features

 Console UI currently supports:

 - Text input with completion and GNU ReadLine compatible editing
 - Checkboxes
 - Lists
 - Expandable Choices (multiple key based answers for a question with help and optional list navigation)
 - Yes/No-Questions

A screen recording of the basic elements demo can be fund on YouTube [console UI demo](https://youtu.be/6dB3CyOX9rU).

# Dependencies

Console UI uses JLine for the dirty console things.

# Maven artefact

ConsoleUI releases are available at Maven Central [org.jline » jline-console-ui](https://search.maven.org/artifact/org.jline/jline-console-ui)

# Test Run

You can get an idea how the project works by looking at `org.jline.consoleui.examples.Basic`.  
You can run this by executing the following from the project root:

    ./jline-console-ui.sh

# Usage

*Hint: see the [how to](doc/howto.md) to get a more detailed documentation how to use ConsoleUI.*


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
    * creates a confirmation prompt. This prompt lets the user answer with 'yes' or 'no' to a given question.
- createInputPrompt()
    * creates an input prompt. This prompt is a classic entry line like a shell. Because of the underlying readline
      implementation it offers you to provide completers (like file name completer or string completer). In addition
      to his, you can define a mask character which is printed on the screen instead of the typed keys like used
      for hidden password entry.
- createListPrompt()
    * creates a list prompt. This prompt lets the user choose one item from a given list.




