# Building JLine

This document provides instructions for building JLine from source.

## Requirements

* JLine 4.x requires Java 11+ at runtime

JLine uses [mvx](https://gnodet.github.io/mvx/) for build environment management, which automatically downloads and manages the required tools.

## Basic Build Instructions

Check out and build:

```sh
git clone git://github.com/jline/jline3.git
cd jline3
./mvx rebuild
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
./mvx demo

# Groovy REPL
./mvx demo repl
```

## Website

JLine includes a documentation website built with Docusaurus. The website includes code snippets extracted from actual working code in the `demo/src/main/java/org/jline/demo/examples` directory.

### Building the Website

To build the website:

```sh
./mvx website build
```

This will:

1. Extract code snippets from example classes to the target directory
2. Build the website

All generated files (build output, snippets, node_modules) will be placed in the `website/target` directory.

### Previewing the Website

For development with live reloading (includes extracting code snippets):

```sh
./mvx website-dev
```

To serve the already built website:

```sh
./mvx website-serve
```

Note: Both `website-dev` and `website-serve` automatically install dependencies if needed.

## Running Demos

JLine includes interactive demos and examples to showcase different features. These work on both Unix/Linux/macOS and Windows.

All demos are run using the unified `./mvx demo` command:

```sh
# Show available demos and examples
./mvx demo

# Run built-in demos
./mvx demo gogo      # Gogo shell demo
./mvx demo repl      # REPL demo with Groovy
./mvx demo password  # Password masking demo
./mvx demo consoleui # ConsoleUI demo (deprecated)
./mvx demo prompt    # New Prompt API demo
./mvx demo graal     # GraalVM native demo

# Run example classes (from org.jline.demo.examples)
./mvx demo JLineExample
./mvx demo BasicTerminalCreation
./mvx demo PromptDynamicExample
./mvx demo MouseEventHandlingExample
# ... and many more (139 total examples available)
```

The demo command automatically detects whether you're requesting a built-in demo or an example class, providing a unified interface for all demonstrations.

### Deployment

The website is automatically deployed to jline.org when changes are pushed to the jline-3.x branch.

## Advanced Build Options

### Building Graal Native Image Demo

Build Graal native-image demo:

```sh
./mvx rebuild -Pnative-image
```

Run the Graal native image:

```sh
./mvx graal
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

### Signing macOS Native Libraries

macOS requires native libraries to be signed with an Apple Developer ID for
applications to pass [notarization](https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution).
The CI build verifies that the macOS `.jnilib` binaries are properly signed on
every push and pull request.

**Prerequisites:**

* An [Apple Developer Program](https://developer.apple.com/programs/) membership ($99/year)
* A "Developer ID Application" certificate installed in your Keychain

**Setting up the certificate (one-time):**

1. Enroll in the Apple Developer Program at https://developer.apple.com/programs/
2. Create a "Developer ID Application" certificate (choose **G2 Sub-CA**) via
   Xcode → Settings → Accounts → Manage Certificates, or through the
   [Apple Developer portal](https://developer.apple.com/account/resources/certificates/list)
3. Verify the certificate is installed:

```sh
security find-identity -v -p codesigning
# Should show: "Developer ID Application: Your Name (TEAMID)"
```

**Signing the binaries:**

After rebuilding native libraries with `make native-all`, sign them on your Mac:

```sh
cd native
make sign
```

This signs all three macOS architectures (x86, x86\_64, arm64) with the
Developer ID certificate. The signing identity defaults to the one configured
in the Makefile and can be overridden:

```sh
make sign APPLE_SIGNING_IDENTITY="Developer ID Application: Your Name (TEAMID)"
```

**Verifying signatures:**

```sh
cd native
make verify-sign
```

This checks that each binary has a valid signature from a Developer ID
Application certificate (not ad-hoc or other certificate types).

**Workflow:**

```sh
cd native
make native-all    # cross-compile all platforms
make sign          # sign macOS binaries
make verify-sign   # verify signatures
cd ..
git add native/src/main/resources/org/jline/nativ/Mac/
git commit -m "chore: rebuild and sign native libraries"
```

The signed binaries are committed to git so that release builds are
reproducible — the release workflow publishes exactly what is in the repository.

## Maven Profiles

JLine's build includes several Maven profiles:

* `bundle` - Builds the main bundle jars (default)
* `native-image` - Builds the Graal native image demo
* `javadoc` - Generates Javadoc

Example:

```sh
./mvx rebuild -Pjavadoc
```

## Continuous Integration

JLine uses GitHub Actions for continuous integration. The build configuration is in `.github/workflows/master-build.yml`.

## Release Process

To create a release:

```sh
./mvx run release <version> <next-version>
```

For example:

```sh
./mvx run release 3.30.0 3.30.1-SNAPSHOT
```

This will:

1. Update the version to the release version
2. Tag the release
3. Deploy the release artifacts
4. Update the version to the next development version
