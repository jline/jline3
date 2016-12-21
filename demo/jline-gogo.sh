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

if [ ! -e ${ROOTDIR}/jline/target ] ; then
  echo "Build jline with maven before running the demo"
  exit
fi;

JLINE_VERSION=$(ls ${ROOTDIR}/jline/target/jline-*-SNAPSHOT.jar  | sed -e 's#.*/jline-## ; s#SNAPSHOT.*#SNAPSHOT#')
JANSI_VERSION=$(cat ${ROOTDIR}/pom.xml| grep jansi.version\> | sed -e 's#^.*<jansi.version>## ; s#</jansi.*##')
JNA_VERSION=$(cat ${ROOTDIR}/pom.xml| grep jna.version\> | sed -e 's#^.*<jna.version>## ; s#</jna.*##')
GOGO_RUNTIME_VERSION=$(cat ${ROOTDIR}/pom.xml| grep gogo.runtime.version\> | sed -e 's#^.*<gogo.runtime.version>## ; s#</gogo.*##')
GOGO_JLINE_VERSION=$(cat ${ROOTDIR}/pom.xml| grep gogo.jline.version\> | sed -e 's#^.*<gogo.jline.version>## ; s#</gogo.*##')

# JLINE
cp=${ROOTDIR}/jline/target/jline-${JLINE_VERSION}.jar
# Gogo Runtime
cp=$cp:${ROOTDIR}/deps/gogo-runtime/target/gogo-runtime-${GOGO_RUNTIME_VERSION}-${JLINE_VERSION}.jar
# Gogo JLine
cp=$cp:${ROOTDIR}/deps/gogo-jline/target/gogo-jline-${GOGO_JLINE_VERSION}-${JLINE_VERSION}.jar

usejars=false
opts=""
while [ "${1}" != "" ]; do
    case ${1} in
        'debug')
            opts="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
            shift
            ;;
        'debugs')
            opts="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
            shift
            ;;
        'jansi')
            cp=$cp:${ROOTDIR}/deps/jansi/target/jansi-${JANSI_VERSION}-${JLINE_VERSION}.jar
            shift
            ;;
        'jna')
            cp=$cp:${ROOTDIR}/deps/jna/target/jna-${JNA_VERSION}-${JLINE_VERSION}.jar
            shift
            ;;
        'jars')
            usejars=true
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
fi

# Launch gogo shell
echo "Classpath: $cp"
echo "Launching Gogo JLine..."
set mouse=a
if ${usejars}; then
    java -cp $cp $opts "-Dgosh.home=${DIRNAME}" org.apache.felix.gogo.jline.Main
else
    java --module-path $cp $opts "-Dgosh.home=${DIRNAME}" --module org.apache.felix.gogo.jline/org.apache.felix.gogo.jline.Main
fi
