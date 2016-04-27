<img src="./doc/ConsoleUI-Logo.png" width="200"  align="right">

[![Build Status](https://travis-ci.org/awegmann/consoleui.svg?branch=master)](https://travis-ci.org/awegmann/consoleui)

# Console UI

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
 
# Dependencies

Console UI uses jansi and jline for the dirty console things.







