<!--

    Copyright (c) 2002-2017, the original author or authors.

    This software is distributable under the BSD license. See the terms of the
    BSD license in the documentation provided with this software.

    http://www.opensource.org/licenses/bsd-license.php

-->
# JLine [![License](https://img.shields.io/badge/License-BSD%202--Clause-orange.svg)](https://opensource.org/licenses/BSD-2-Clause) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jline/jline/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jline/jline) [![Build Status: Linux](https://travis-ci.org/jline/jline3.svg?branch=master)](https://travis-ci.org/jline/jline3) [![Build Status: Windows](https://ci.appveyor.com/api/projects/status/github/jline/jline3?svg=true)](https://ci.appveyor.com/project/gnodet/jline3) [![DepShield Badge](https://depshield.sonatype.org/badges/jline/jline3/depshield.svg)](https://depshield.github.io)


JLine is a Java library for handling console input. It is similar in functionality to [BSD editline](http://www.thrysoee.dk/editline/) and [GNU readline](http://www.gnu.org/s/readline/) but with additional features that bring it in par with [ZSH line editor](http://zsh.sourceforge.net/Doc/Release/Zsh-Line-Editor.html). People familiar with the readline/editline capabilities for modern shells (such as bash and tcsh) will find most of the command editing features of JLine to be familiar.

JLine 3.x is an evolution of [JLine 2.x](https://github.com/jline/jline2).

# License

JLine is distributed under the [BSD License](https://opensource.org/licenses/BSD-3-Clause), meaning that you are completely free to redistribute, modify, or sell it with almost no restrictions.

# Documentation

* [demos](https://github.com/jline/jline3/wiki/Demos)
* [wiki](https://github.com/jline/jline3/wiki)
* [javadoc](https://www.javadoc.io/doc/org.jline/jline/)

# Forums

* [jline-users](https://groups.google.com/group/jline-users)
* [jline-dev](https://groups.google.com/group/jline-dev)

# Artifacts

JLine can be used with a single bundle or smaller fined grained jars.
The big bundle is named:

    jline-${jline.version}.jar

The dependencies are minimal: you may use JLine without any dependency on *nix systems, but in order to support windows or more advanced usage, you will need to add either [`jansi`](https://repo1.maven.org/maven2/org/fusesource/jansi/jansi/1.17/jansi-1.17.jar) or [`jna`](https://repo1.maven.org/maven2/net/java/dev/jna/jna/4.5.1/jna-4.5.1.jar) library.

You can also use finer grained jars:
* `jline-terminal`: the `Terminal` api and implementations
* `jline-terminal-jansi`: terminal implementations leveraging the `jansi` library
* `jline-terminal-jna`: terminal implementations leveraging the `jna` library
* `jline-reader`: the line reader (including completion, history, etc...)
* `jline-style`: styling api
* `jline-remote-ssh`: helpers for using jline with [Mina SSHD](http://mina.apache.org/sshd-project/)
* `jline-remote-telnet`: helpers for using jline over telnet (including a telnet server implementation)
* `jline-builtins`: several high level tools: `less` pager, `nano` editor, `screen` multiplexer, etc...

## Supported platforms

JLine supports the following platforms:
* FreeBSD
* Linux
* OS X
* Solaris
* Windows

## Jansi vs JNA

To access the JVM's main terminal under a \*nix system, no additional dependency will be needed.  However, such usage will make use of child processes whenever the terminal is accessed (using `Terminal.getAttributes`, `Terminal.setAttributes`, `Terminal.getSize`, `Terminal.setSize`).  If one of the Jansi or JNA library is present, it will be used and JLine will use native calls instead of child processes.  This also allows the use of pseudo-terminals when dealing with non system terminals (for example when creating a terminal for an incoming connection).

On the Windows platform, relying on native calls is mandatory, so you need to have either Jansi or JNA library in your classpath along with the `jline-terminal-jansi` or `jline-terminal-jna` jar.  Failing to do so will create a `dumb` terminal with no advanced capabilities.

There is no difference between JLine's support for Jansi and JNA.  Both will provide the exact same behaviors. So it's a matter of preference: Jansi is a smaller but more focused library while JNA is a bigger but more generic and versatile one.

# Maven Usage

Use the following definition to use JLine in your maven project:

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>${jline.version}</version>
</dependency>
```

JLine can also be used with more low-level jars:

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-terminal</artifactId>
    <version>${jline.version}</version>
</dependency>
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-reader</artifactId>
    <version>${jline.version}</version>
</dependency>
```

All the jars and releases are available from Maven Central, so you'll find everything at the following location <https://repo1.maven.org/maven2/org/jline/>.

# Building

## Requirements

* Maven 3.3+ (prefer included maven-wrapper)
* Java 8+

Check out and build:

```sh
git clone git://github.com/jline/jline3.git
cd jline3
./build rebuild
 ```

## Results

The following artifacts are build:

The big bundle includes everything and is located at:

    jline/target/jline-${jline.version}.jar

The finer grained bundles are located at:

    terminal/target/jline-terminal-${jline.version}.jar
    terminal-jansi/target/jline-jansi-${jline.version}.jar
    terminal-jna/target/jline-jna-${jline.version}.jar
    reader/target/jline-reader-${jline.version}.jar
    style/target/jline-style-${jline.version}.jar
    builtins/target/jline-builtins-${jline.version}.jar
    remote-telnet/target/jline-remote-telnet-${jline.version}.jar
    remote-ssh/target/jline-remote-ssh-${jline.version}.jar

Maven has a concept of `SNAPSHOT`. During development, the jline version will always ends with `-SNAPSHOT`, which means that the version is in development and not a release.

Note that all those artifacts are also installed in the local maven repository, so you will usually find them in the following folder: `~/.m2/repository/org/jline/`.

## Running the demo

To run the demo, simply use the following command after having build `JLine`

```sh
./build demo
```

## Continuous Integration

* [Travis](https://travis-ci.org/jline/jline3)
* [AppVeyor](https://ci.appveyor.com/project/gnodet/jline3)

