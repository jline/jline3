<!--

    Copyright (c) 2002-2024, the original author or authors.

    This software is distributable under the BSD license. See the terms of the
    BSD license in the documentation provided with this software.

    https://opensource.org/licenses/BSD-3-Clause

-->
# JLine [![License](https://img.shields.io/badge/License-BSD%202--Clause-orange.svg)](https://opensource.org/licenses/BSD-3-Clause) [![Maven Central](https://img.shields.io/maven-central/v/org.jline/jline.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.jline/jline) [![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/org/jline/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/jline/README.md)





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

JLine can be used with a single bundle or smaller fine-grained jars. The bundle contains all jars except `jline-groovy` that must be included in classpath if you want to use scripting capabilities.
The big bundle is named:

    jline-${jline.version}.jar

The dependencies are minimal: you may use JLine without any dependency on *nix systems, but in order to support windows or more advanced usage, you will need to add either [`Jansi`](https://repo1.maven.org/maven2/org/fusesource/jansi/jansi/1.18/jansi-1.18.jar) or [`JNA`](https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.3.1/jna-5.3.1.jar) library.

You can also use fine grained jars:
* `jline-terminal`: the `Terminal` api and implementations
* `jline-terminal-jansi`: terminal implementations leveraging the `Jansi` library
* `jline-terminal-jni`: terminal implementations leveraging the JNI native library
* `jline-terminal-jna`: terminal implementations leveraging the `JNA` library
* `jline-terminal-ffm`: terminal implementations leveraging the Foreign Functions & Mapping layer
* `jline-native`: the native library
* `jline-reader`: the line reader (including completion, history, etc...)
* `jline-style`: styling api
* `jline-remote-ssh`: helpers for using jline with [Mina SSHD](http://mina.apache.org/sshd-project/)
* `jline-remote-telnet`: helpers for using jline over telnet (including a telnet server implementation)
* `jline-builtins`: several high level tools: `less` pager, `nano` editor, `screen` multiplexer, etc...
* `jline-console`: command registry, object printer and widget implementations
* `jline-groovy`: `ScriptEngine` implementation using Groovy
* `jline-console-ui`: provides simple UI elements on ANSI terminals

# JANSI

The JANSI project has been merged into JLine.
The following artifacts are available:
* `jansi-core`: the fine-grained jar containing jansi
* `jansi`: a jar bundle which contains `jansi-core` and the needed jline dependencies

## Supported platforms

JLine supports the following platforms:
* FreeBSD
* Linux
* OS X
* Solaris
* Windows

## FFM vs JNI vs Jansi vs JNA vs Exec

To perform the required operations, JLine needs to interoperate with the OS layer.  
This is done through the JLine `TerminalProvider` interface.  The terminal builder 
will automatically select a provider amongst the ones that are available.

On the Windows platform, relying on native calls is mandatory, so you need to have 
a real provider (`jline-terminal-xxx` jar) registered and its dependencies available 
(usually the Jansi or JNA library).  Failing to do so will create a `dumb` terminal 
with no advanced capabilities.

By default, the following order will be used.

### FFM

The [FFM](https://docs.oracle.com/en/java/javase/21/core/foreign-function-and-memory-api.html#GUID-FBE990DA-C356-46E8-9109-C75567849BA8) provider is available since JLine 3.24 and when running on JDK >= 22.  
It's very lightweight with no additional dependencies. With JLine 3.26, the FFM 
provider requires JDK 22 with the `--enable-native-access=ALL-UNNAMED` JVM option.  
Note that JLine 3.24 and 3.25 uses the preview version of FFM support shipped in JDK 
21 which is incompatible with the final version in JDK 22.

### JNI

Since JLine 3.24.0, JLine provides its own JNI based provider and native libraries. 
This is the best default choice, with no additional dependency. One requirement is 
that JLine will load a native library: this is usually not a problem, but it could 
be a limitation in certain environments.

### JANSI

The [Jansi](https://github.com/fusesource/jansi) library is a library specialized in supporting ANSI sequences in 
terminals.  Historically, the JNI methods used by JLine were provided by Jansi. In 
order to minimize the maintenance cost, Jansi has been merged into JLine 3.25.

This provider has been deprecated in 3.26 in favor of the JNI provider.

### JNA

The [JNA](https://github.com/java-native-access/jna) library aims to provide an alternative way to access native methods 
without requiring writing a full JNI native library.  If JNA is in JLine's class 
loader, the provider may be used. JNA is not supported on Apple M2 architectures.

This provider has been deprecated in 3.26 in favor of the FFM provider.

### Exec

The exec provider is available on Posix systems and on Windows when running under 
[Cygwin](https://www.cygwin.com) or [MSys2](https://www.msys2.org).  This provider launches child processes whenever the 
terminal is accessed (using `Terminal.getAttributes`, `Terminal.setAttributes`, 
`Terminal.getSize`, `Terminal.setSize`).  

This provider also does not support external terminals (for example when creating a 
terminal for an incoming connection) and does not support the Windows native environment.

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
    <artifactId>jline-terminal-jni</artifactId>
    <version>${jline.version}</version>
</dependency>
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-reader</artifactId>
    <version>${jline.version}</version>
</dependency>
```

All the jars and releases are available from Maven Central, so you'll find everything at 
the following location <https://repo1.maven.org/maven2/org/jline/>.

# Building

## Requirements

* Maven 3.9.7+
* Java 8+ at runtime
* Java 22+ at build time
* Graal 23.1+ (native-image)

Check out and build:

```sh
git clone git://github.com/jline/jline3.git
cd jline3
./build rebuild
 ```

Build Graal native-image demo:

```sh
./build rebuild -Pnative-image
```

## Results

The following artifacts are build:

The big bundle includes everything (except `jline-groovy`) and is located at:

    jline/target/jline-${jline.version}.jar

The jansi bundle is located at:

    jansi/target/jansi-${jline.version}.jar

The fine grained bundles are located at:

    terminal/target/jline-terminal-${jline.version}.jar
    terminal-jansi/target/jline-jansi-${jline.version}.jar
    terminal-jna/target/jline-jna-${jline.version}.jar
    reader/target/jline-reader-${jline.version}.jar
    style/target/jline-style-${jline.version}.jar
    remote-telnet/target/jline-remote-telnet-${jline.version}.jar
    remote-ssh/target/jline-remote-ssh-${jline.version}.jar
    builtins/target/jline-builtins-${jline.version}.jar
    console/target/jline-console-${jline.version}.jar
    groovy/target/jline-groovy-${jline.version}.jar
    jansi-core/target/jansi-core-${jline.version}.jar

Maven has a concept of `SNAPSHOT`. During development, the jline version will always end 
with `-SNAPSHOT`, which means that the version is in development and not a release.

Note that all those artifacts are also installed in the local maven repository, so you 
will usually find them in the following folder: `~/.m2/repository/org/jline/`.

## Running the demo

To run the demo, simply use one of the following commands after having build `JLine`

```sh
# Gogo terminal
./build demo

# Groovy REPL
./build repl

# Graal native-image
./build graal
```

## Continuous Integration

* [Travis](https://travis-ci.org/jline/jline3)
* [AppVeyor](https://ci.appveyor.com/project/gnodet/jline3)

