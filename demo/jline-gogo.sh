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

cp=${TARGETDIR}/classes
# JLINE
cp=${cp}$(find ${TARGETDIR}/lib -name "jline-*.jar" -exec printf :{} ';')
# Gogo Runtime
cp=${cp}$(find ${TARGETDIR}/lib -name "org.apache.felix.gogo.runtime-*.jar" -exec printf :{} ';')
# Gogo JLine
cp=${cp}$(find ${TARGETDIR}/lib -name "org.apache.felix.gogo.jline-*.jar" -exec printf :{} ';')

opts="${JLINE_OPTS}"
logconf="${DIRNAME}/etc/logging.properties"
while [ "${1}" != "" ]; do
    case ${1} in
        'debug')
            opts="${opts} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
            shift
            ;;
        'debugs')
            opts="${opts} -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
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
        'ssh' | 'telnet' | 'remote')
            cp=${cp}$(find ${TARGETDIR}/lib -name "sshd-common-*.jar" -exec printf :{} ';')
            cp=${cp}$(find ${TARGETDIR}/lib -name "sshd-core-*.jar" -exec printf :{} ';')
            cp=${cp}$(find ${TARGETDIR}/lib -name "sshd-scp-*.jar" -exec printf :{} ';')
            cp=${cp}$(find ${TARGETDIR}/lib -name "sshd-sftp-*.jar" -exec printf :{} ';')
            cp=${cp}$(find ${TARGETDIR}/lib -name "slf4j-api-*.jar" -exec printf :{} ';')
            cp=${cp}$(find ${TARGETDIR}/lib -name "slf4j-jdk14-*.jar" -exec printf :{} ';')
            shift
            ;;
        'verbose')
            logconf="${DIRNAME}/etc/logging-verbose.properties"
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

# Launch gogo shell
echo "Launching Gogo JLine..."
echo "Classpath: $cp"
set mouse=a
java -cp $cp \
    $opts \
    -Dgosh.home="${DIRNAME}" \
    -Djava.util.logging.config.file="${logconf}" \
    org.apache.felix.gogo.jline.Main

