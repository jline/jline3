#!/bin/sh

# Script to run the Gogo JLine demo

SCRIPT_DIR=$(cd "$(dirname "$0")" || exit; pwd)
. "${SCRIPT_DIR}/jline-common.sh"

# We need to add the Gogo jars before calling setup_environment
# because setup_environment will use the cp variable
TARGETDIR="${SCRIPT_DIR}/target"

# Add Gogo Runtime
if [ -d ${TARGETDIR}/lib ]; then
  # Initialize cp if it's not already set
  if [ -z "${cp}" ]; then
    cp="${TARGETDIR}/classes"
  fi

  cp=${cp}$(find ${TARGETDIR}/lib -name "org.apache.felix.gogo.runtime-*.jar" -exec printf :{} ';')
  # Add Gogo JLine
  cp=${cp}$(find ${TARGETDIR}/lib -name "org.apache.felix.gogo.jline-*.jar" -exec printf :{} ';')
fi

# Define gogo-specific help options
GOGO_HELP="gogo-option"

# Process SSH/Telnet/Remote options
for arg in "$@"; do
    case ${arg} in
        'ssh' | 'telnet' | 'remote')
            if [ -d ${TARGETDIR}/lib ]; then
              cp=${cp}$(find ${TARGETDIR}/lib -name "sshd-common-*.jar" -exec printf :{} ';')
              cp=${cp}$(find ${TARGETDIR}/lib -name "sshd-core-*.jar" -exec printf :{} ';')
              cp=${cp}$(find ${TARGETDIR}/lib -name "sshd-scp-*.jar" -exec printf :{} ';')
              cp=${cp}$(find ${TARGETDIR}/lib -name "sshd-sftp-*.jar" -exec printf :{} ';')
              cp=${cp}$(find ${TARGETDIR}/lib -name "slf4j-api-*.jar" -exec printf :{} ';')
              cp=${cp}$(find ${TARGETDIR}/lib -name "slf4j-jdk14-*.jar" -exec printf :{} ';')
            fi
            ;;
    esac
done

# Pass the GOGO_HELP separately for help display only
if [ "$1" = "--help" ]; then
    show_help "$GOGO_HELP"
    exit 0
fi

setup_environment "$@"

run_demo org.apache.felix.gogo.jline.Main
