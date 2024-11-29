#!/bin/sh

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

REALNAME=$(realpath "$0")
DIRNAME=$(dirname "${REALNAME}")
PROGNAME=$(basename "${REALNAME}")
ROOTDIR=${DIRNAME}/..
TARGETDIR=${DIRNAME}/target

if [ ! -e ${TARGETDIR}/lib ] ; then
  echo "Build jline with maven before running the demo"
  exit
fi;

cp=${TARGETDIR}/classes:${TARGETDIR}/test-classes
# JLINE
cp=${cp}$(find ${TARGETDIR}/lib -name "jline-*.jar" -exec printf :{} ';')

opts="${JLINE_OPTS}"
while [ "${1}" != "" ]; do
    case ${1} in
        'debug')
            opts="${opts} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
            shift
            ;;
        'debugs')
            opts="${opts} -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
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
        *)
            opts="${opts} ${1}"
            shift
            ;;
    esac
done

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

echo "Launching ConsoleUI..."
echo "Classpath: $cp"
set mouse=a
java -cp $cp \
    $opts \
    org.jline.consoleui.examples.BasicDynamic

