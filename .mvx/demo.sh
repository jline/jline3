#!/bin/bash

# Demo runner script for mvx
demo_name="$1"
shift

if [ -z "$demo_name" ]; then
  echo "Usage: ./mvx demo <name> [options]"
  echo ""
  echo "Built-in demos:"
  echo "  gogo      - Run the Gogo shell demo"
  echo "  repl      - Run the REPL demo"
  echo "  password  - Run the password demo"
  echo "  consoleui - Run the ConsoleUI demo (deprecated)"
  echo "  prompt    - Run the new Prompt API demo"
  echo "  graal     - Run the GraalVM native demo"
  echo ""
  echo "Options:"
  echo "  debug     - Enable remote debugging"
  echo "  debugs    - Enable remote debugging with suspend"
  echo "  verbose   - Enable verbose logging"
  echo "  ffm       - Enable Foreign Function Memory"
  echo ""
  echo "Password-specific options:"
  echo "  --mask=X  - Use X as the mask character (default: *)"
  echo "              Use --mask= (empty) for no masking"
  echo ""
  echo "Example demos (from org.jline.demo.examples):"
  echo "  Run './mvx demo <ExampleClassName>' for any example class"
  echo "  Available examples:"
  find demo/src/main/java/org/jline/demo/examples -name "*.java" 2>/dev/null | sed 's/.*\/\([^\/]*\)\.java/    \1/' | sort | head -10
  echo "    ... and many more ($(find demo/src/main/java/org/jline/demo/examples -name "*.java" 2>/dev/null | wc -l | tr -d ' ') total)"
  exit 1
fi

# Set up classpath
TARGETDIR="demo/target"
cp="${TARGETDIR}/classes"
logconf="demo/etc/logging.properties"
JVM_OPTS=""
APP_ARGS=""
MAIN_CLASS=""

# Add JLine jars
if [ -d ${TARGETDIR}/lib ]; then
  cp=${cp}$(find ${TARGETDIR}/lib -name "*.jar" -exec printf :{} ';')
fi

# Determine if this is a built-in demo or example class based on known demo types
case "$demo_name" in
  "gogo"|"repl"|"password"|"consoleui"|"prompt"|"graal")
    # This is a built-in demo
    case "$demo_name" in
      "gogo")
        MAIN_CLASS="org.apache.felix.gogo.jline.Main"
        ;;
      "repl")
        MAIN_CLASS="org.jline.demo.Repl"
        ;;
      "password")
        MAIN_CLASS="org.jline.demo.PasswordMaskingDemo"
        ;;
      "consoleui")
        MAIN_CLASS="org.jline.demo.consoleui.BasicDynamic"
        ;;
      "prompt")
        MAIN_CLASS="org.jline.demo.examples.PromptDynamicExample"
        ;;
      "graal")
        # Special case for graal - run the native executable
        echo "Running GraalVM native demo"
        exec graal/target/graal "$@"
        ;;
    esac
    ;;
  *)
    # This is an example class
    example_name="$demo_name"
    
    # Check if the example class exists
    if ! find demo/src/main/java/org/jline/demo/examples -name "${example_name}.java" 2>/dev/null | grep -q .; then
      echo "Demo '${example_name}' not found."
      echo ""
      echo "Available built-in demos: gogo, repl, password, consoleui, prompt, graal"
      echo ""
      echo "Available example demos:"
      find demo/src/main/java/org/jline/demo/examples -name "*.java" 2>/dev/null | sed 's/.*\/\([^\/]*\)\.java/  \1/' | sort
      exit 1
    fi
    
    MAIN_CLASS="org.jline.demo.examples.${example_name}"
    ;;
esac

# Process options
for arg in "$@"; do
  case ${arg} in
    'debug')
      if [ -z "$JVM_OPTS" ]; then
        JVM_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
      else
        JVM_OPTS="${JVM_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
      fi
      ;;
    'debugs')
      if [ -z "$JVM_OPTS" ]; then
        JVM_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
      else
        JVM_OPTS="${JVM_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
      fi
      ;;
    'verbose')
      logconf="demo/etc/logging-verbose.properties"
      ;;
    'ffm')
      if [ -z "$JVM_OPTS" ]; then
        JVM_OPTS="--enable-native-access=ALL-UNNAMED"
      else
        JVM_OPTS="${JVM_OPTS} --enable-native-access=ALL-UNNAMED"
      fi
      ;;
    --mask=*)
      # Pass mask option as application argument for password demo
      APP_ARGS="${APP_ARGS} ${arg}"
      ;;
    *)
      # Unknown option, assume it's an application argument
      APP_ARGS="${APP_ARGS} ${arg}"
      ;;
  esac
done

# Check if JDK version supports --enable-native-access
if [[ "$JVM_OPTS" == *"--enable-native-access=ALL-UNNAMED"* ]]; then
  java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
  if [ "$java_version" -lt 16 ] 2>/dev/null; then
    echo "Warning: --enable-native-access requires Java 16 or later"
  fi
fi

# Run the demo
if [ -n "$MAIN_CLASS" ]; then
  echo "Running demo: ${MAIN_CLASS}"
  if [ -n "$JVM_OPTS" ]; then
    echo "JVM options: $JVM_OPTS"
  fi
  if [ -n "$APP_ARGS" ]; then
    echo "Application arguments: $APP_ARGS"
  fi
  
  # Build and execute the Java command
  # Use eval to properly handle arguments with spaces
  if [ -n "$JVM_OPTS" ]; then
    eval java -cp \"$cp\" $JVM_OPTS -Dgosh.home=\"demo\" -Djava.util.logging.config.file=\"${logconf}\" ${MAIN_CLASS} $APP_ARGS
  else
    eval java -cp \"$cp\" -Dgosh.home=\"demo\" -Djava.util.logging.config.file=\"${logconf}\" ${MAIN_CLASS} $APP_ARGS
  fi
fi
