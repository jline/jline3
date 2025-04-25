#!/bin/sh

# Common script for JLine demos

realpath() {
  OURPWD=${PWD}
  cd "$(dirname "${1}")"
  LINK=$(readlink "$(basename "${1}")")
  while [ "${LINK}" ]; do
    cd "$(dirname "${LINK}")"
    LINK=$(readlink "$(basename "${1}")")
  done
  REALPATH="${PWD}/$(basename "${1}")"
  cd "${OURPWD}"
  echo "${REALPATH}"
}

# Process help option
show_help() {
  SCRIPT_NAME=$(basename "$0")
  echo "Usage: $SCRIPT_NAME [options]"
  echo "Options:"
  echo "  --help       Show this help message"

  # Add demo-specific help options if provided
  if [ -n "${1:-}" ]; then
    if [ "$1" = "mask-option" ]; then
      echo "  --mask=X     Use X as the mask character (default: *)"
      echo "               Use --mask= (empty) for no masking"
    elif [ "$1" = "gogo-option" ]; then
      echo "  ssh          Add SSH support"
      echo "  telnet       Add Telnet support"
      echo "  remote       Add remote support (SSH and Telnet)"
    elif [ "$1" = "repl-option" ]; then
      # No REPL-specific options
      :
    else
      echo "  $1"
    fi
  fi

  # Add common options
  echo "  debug        Enable remote debugging"
  echo "  debugs       Enable remote debugging with suspend"
  echo "  jansi        Add Jansi support"
  echo "  jna          Add JNA support"
  echo "  verbose      Enable verbose logging"
  echo "  ffm          Enable Foreign Function Memory (preview)"
  echo ""
  echo "To test with a dumb terminal, use: TERM=dumb $SCRIPT_NAME"
  exit 0
}

setup_environment() {
  # Check for help option first
  if [ "$1" = "--help" ]; then
    show_help "$2"
  fi

  REALNAME=$(realpath "$0")
  DIRNAME=$(dirname "${REALNAME}")
  PROGNAME=$(basename "${REALNAME}")
  ROOTDIR=${DIRNAME}/..
  TARGETDIR=${DIRNAME}/target

  if [ ! -e ${TARGETDIR}/lib ] ; then
    echo "Build jline with maven before running the demo"
    exit 1
  fi;

  # Initialize cp if it's not already set
  if [ -z "${cp}" ]; then
    cp=${TARGETDIR}/classes
  fi

  # JLINE
  if [ -d ${TARGETDIR}/lib ]; then
    cp=${cp}$(find ${TARGETDIR}/lib -name "jline-*.jar" -exec printf :{} ';')
  fi

  # Separate JVM options from application arguments
  JVM_OPTS="${JLINE_OPTS}"
  APP_ARGS=""

  while [ "${1}" != "" ]; do
    case ${1} in
      'debug')
        JVM_OPTS="${JVM_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
        shift
        ;;
      'debugs')
        JVM_OPTS="${JVM_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
        shift
        ;;
      'jansi')
        cp=${cp}$(find ${TARGETDIR}/lib -name "jansi-*.jar" -exec printf :{} ';')
        shift
        ;;
      'jna')
        cp=${cp}$(find ${TARGETDIR}/lib -name "jna-*.jar" -exec printf :{} ';')
        shift
        ;;
      'verbose')
        logconf="${DIRNAME}/etc/logging-verbose.properties"
        shift
        ;;
      'ffm')
        JVM_OPTS="${JVM_OPTS} --enable-preview --enable-native-access=ALL-UNNAMED"
        shift
        ;;
      '--help')
        show_help "$2"
        ;;
      'ssh' | 'telnet' | 'remote')
        # These are handled by the calling script
        shift
        ;;
      *)
        if [ "${1:0:2}" = "--" ]; then
          # Assume anything starting with -- is an application argument
          APP_ARGS="${APP_ARGS} ${1}"
        else
          # Unknown option, assume it's a JVM option
          JVM_OPTS="${JVM_OPTS} ${1}"
        fi
        shift
        ;;
    esac
  done

  # Set opts to JVM_OPTS for backward compatibility
  opts="${JVM_OPTS}"

  cygwin=false
  mingw=false
  case "$(uname)" in
    CYGWIN*)
      cygwin=true
      ;;
    MINGW*)
      mingw=true
      ;;
  esac
  if ${cygwin}; then
    cp=$(cygpath --path --windows "${cp}")
    DIRNAME=$(cygpath --path --windows "${DIRNAME}")
  fi

  nothing() {
    # nothing to do here
    a=a
  }
  trap 'nothing' TSTP
}

run_demo() {
  MAIN_CLASS=$1

  # Launch the demo
  echo "Launching ${MAIN_CLASS}..."
  echo "Classpath: $cp"
  echo "JVM options: $opts"
  if [ -n "$APP_ARGS" ]; then
    echo "Application arguments: $APP_ARGS"
  fi

  set mouse=a

  # Run Java directly with application arguments
  # We need to handle each argument separately to avoid shell interpretation issues
  JAVA_CMD="java -cp \"$cp\" $opts -Dgosh.home=\"${DIRNAME}\" -Djava.util.logging.config.file=\"${logconf}\" ${MAIN_CLASS}"

  # Add each application argument individually
  for arg in $APP_ARGS; do
    JAVA_CMD="$JAVA_CMD \"$arg\""
  done

  # Execute the command
  eval $JAVA_CMD
}
