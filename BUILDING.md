# Building JLine

This document provides instructions for building JLine from source.

## Requirements

* Maven 3.9.7+
* Java 8+ at runtime
* Java 22+ at build time
* Graal 23.1+ (for native-image builds)

## Basic Build Instructions

Check out and build:

```sh
git clone git://github.com/jline/jline3.git
cd jline3
./build rebuild
```

## Build Results

The following artifacts are built:

### Main Bundle

The big bundle includes everything (except `jline-groovy`) and is located at:

```
jline/target/jline-${jline.version}.jar
```

### Jansi Bundle

The jansi bundle is located at:

```
jansi/target/jansi-${jline.version}.jar
```

### Fine-grained Bundles

The fine-grained bundles are located at:

```
terminal/target/jline-terminal-${jline.version}.jar
terminal-jansi/target/jline-jansi-${jline.version}.jar
terminal-jna/target/jline-jna-${jline.version}.jar
terminal-jni/target/jline-jni-${jline.version}.jar
terminal-ffm/target/jline-ffm-${jline.version}.jar
native/target/jline-native-${jline.version}.jar
reader/target/jline-reader-${jline.version}.jar
style/target/jline-style-${jline.version}.jar
remote-telnet/target/jline-remote-telnet-${jline.version}.jar
remote-ssh/target/jline-remote-ssh-${jline.version}.jar
builtins/target/jline-builtins-${jline.version}.jar
console/target/jline-console-${jline.version}.jar
console-ui/target/jline-console-ui-${jline.version}.jar
groovy/target/jline-groovy-${jline.version}.jar
jansi-core/target/jansi-core-${jline.version}.jar
```

Maven has a concept of `SNAPSHOT`. During development, the jline version will always end with `-SNAPSHOT`, which means that the version is in development and not a release.

Note that all those artifacts are also installed in the local maven repository, so you will usually find them in the following folder: `~/.m2/repository/org/jline/`.

## Running the Demos

To run the demos, simply use one of the following commands after having built JLine:

```sh
# Gogo terminal
./build demo

# Groovy REPL
./build repl
```

## Website

JLine includes a documentation website built with Docusaurus. The website includes code snippets extracted from actual working code in the `demo/src/main/java/org/jline/demo/examples` directory.

### Building the Website

To build the website:

```sh
./build website
```

This will:
1. Extract code snippets from example classes to the target directory
2. Build the website

All generated files (build output, snippets, node_modules) will be placed in the `website/target` directory.

### Previewing the Website

For development with live reloading (includes extracting code snippets):

```sh
./build website-dev
```

To preview the already built website (after running `./build website`):

```sh
./build website-serve
```

### Deployment

The website is automatically deployed to jline.org when changes are pushed to the master branch.

## Advanced Build Options

### Building Graal Native Image Demo

Build Graal native-image demo:

```sh
./build rebuild -Pnative-image
```

Run the Graal native image:

```sh
./build graal
```

### Building Native Libraries

JLine includes native libraries for various platforms. To build them:

```sh
cd native
make native
```

For cross-compilation to all supported platforms:

```sh
cd native
make native-all
```

This requires Docker for cross-compilation.

## Maven Profiles

JLine's build includes several Maven profiles:

* `bundle` - Builds the main bundle jars (default)
* `native-image` - Builds the Graal native image demo
* `javadoc` - Generates Javadoc
* `license-check` - Checks license headers
* `license-format` - Formats license headers

Example:

```sh
./build rebuild -Pjavadoc
```

## Continuous Integration

JLine uses GitHub Actions for continuous integration. The build configuration is in `.github/workflows/master-build.yml`.

## Release Process

To create a release:

```sh
./build release <version> <next-version>
```

For example:

```sh
./build release 3.30.0 3.30.1-SNAPSHOT
```

This will:
1. Update the version to the release version
2. Tag the release
3. Deploy the release artifacts
4. Update the version to the next development version
