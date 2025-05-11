#!/bin/sh

# Script to run the LineReaderMouseExample

SCRIPT_DIR=$(cd "$(dirname "$0")" || exit; pwd)
. "${SCRIPT_DIR}/jline-common.sh"

# Separate JVM options from application arguments
JVM_ARGS=""
APP_ARGS=""

for arg in "$@"; do
    if [ "$arg" = "--help" ]; then
        # Help is handled by setup_environment
        JVM_ARGS="$JVM_ARGS $arg"
    elif [ "$arg" = "debug" ] || [ "$arg" = "debugs" ] || [ "$arg" = "jansi" ] || \
         [ "$arg" = "jna" ] || [ "$arg" = "verbose" ] || [ "$arg" = "ffm" ]; then
        # These are JVM options
        JVM_ARGS="$JVM_ARGS $arg"
    else
        # Unknown option, pass to both
        JVM_ARGS="$JVM_ARGS $arg"
        APP_ARGS="$APP_ARGS $arg"
    fi
done

setup_environment $JVM_ARGS

run_demo org.jline.demo.examples.LineReaderMouseExample
