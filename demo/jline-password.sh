#!/bin/sh

# Script to run the PasswordMaskingDemo

SCRIPT_DIR=$(cd "$(dirname "$0")" || exit; pwd)
. "${SCRIPT_DIR}/jline-common.sh"

# Define password-specific help options
PASSWORD_HELP="mask-option"

# Separate JVM options from application arguments
JVM_ARGS=""
APP_ARGS=""

for arg in "$@"; do
    if [ "${arg:0:7}" = "--mask=" ]; then
        # This is an application argument
        APP_ARGS="$APP_ARGS $arg"
    elif [ "$arg" = "--help" ]; then
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

# Pass the PASSWORD_HELP separately for help display only
if [ "$JVM_ARGS" = "--help" ]; then
    show_help "$PASSWORD_HELP"
    exit 0
fi

setup_environment $JVM_ARGS

run_demo org.jline.demo.PasswordMaskingDemo
