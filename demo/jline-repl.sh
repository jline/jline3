#!/bin/sh

# Script to run the Groovy REPL demo

SCRIPT_DIR=$(cd "$(dirname "$0")" || exit; pwd)
. "${SCRIPT_DIR}/jline-common.sh"

# We need to add the Groovy jars before calling setup_environment
# because setup_environment will use the cp variable
TARGETDIR="${SCRIPT_DIR}/target"

# Add Groovy and Ivy jars
if [ -d ${TARGETDIR}/lib ]; then
  # Initialize cp if it's not already set
  if [ -z "${cp}" ]; then
    cp="${TARGETDIR}/classes"
  fi

  cp=${cp}$(find ${TARGETDIR}/lib -name "groovy-*.jar" -exec printf :{} ';')
  cp=${cp}$(find ${TARGETDIR}/lib -name "ivy-*.jar" -exec printf :{} ';')
fi

# No REPL-specific help options
REPL_HELP=""

# Pass the REPL_HELP separately for help display only
if [ "$1" = "--help" ]; then
    show_help "$REPL_HELP"
    exit 0
fi

setup_environment "$@"

# Launch Groovy REPL
run_demo org.jline.demo.Repl
