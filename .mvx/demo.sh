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
    echo "Running demo: ${MAIN_CLASS}"
    java -cp "$cp" -Dgosh.home="demo" ${MAIN_CLASS} "$@"
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

    echo "Running demo: org.jline.demo.examples.${example_name}"
    java -cp "$cp" org.jline.demo.examples.${example_name} "$@"
    ;;
esac
