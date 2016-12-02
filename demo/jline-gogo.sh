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
TARGETDIR=${ROOTDIR}/target

if [ ! -e ${TARGETDIR} ] ; then
  echo "Build jline with maven before running the demo"
  exit
fi;
if [ ! -e ${TARGETDIR}/lib ] ; then
  mkdir ${TARGETDIR}/lib
fi;

JLINE_VERSION=$(ls ${TARGETDIR}/jline-*-SNAPSHOT.jar  | sed -e 's#.*/jline-## ; s#SNAPSHOT.*#SNAPSHOT#')
JANSI_VERSION=$(cat ${ROOTDIR}/pom.xml| grep jansi.version\> | sed -e 's#^.*<jansi.version>## ; s#</jansi.*##')
JNA_VERSION=$(cat ${ROOTDIR}/pom.xml| grep jna.version\> | sed -e 's#^.*<jna.version>## ; s#</jna.*##')
GOGO_RUNTIME_VERSION=1.0.0
GOGO_JLINE_VERSION=1.0.0

# JLINE
cp=${TARGETDIR}/jline-${JLINE_VERSION}.jar

# JANSI
if [ ! -f ${TARGETDIR}/lib/jansi-${JANSI_VERSION}.jar ] ; then
  echo "Downloading Jansi..."
  wget -O ${TARGETDIR}/lib/jansi-${JANSI_VERSION}.jar http://repo1.maven.org/maven2/org/fusesource/jansi/jansi/${JANSI_VERSION}/jansi-${JANSI_VERSION}.jar
fi
cp=$cp:${TARGETDIR}/lib/jansi-${JANSI_VERSION}.jar

# Gogo Runtime
if [ ! -f ${TARGETDIR}/lib/org.apache.felix.gogo.runtime-${GOGO_RUNTIME_VERSION}.jar ] ; then
  echo "Downloading Gogo Runtime..."
  wget -O ${TARGETDIR}/lib/org.apache.felix.gogo.runtime-${GOGO_RUNTIME_VERSION}.jar http://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.gogo.runtime/${GOGO_RUNTIME_VERSION}/org.apache.felix.gogo.runtime-${GOGO_RUNTIME_VERSION}.jar
fi
cp=$cp:${TARGETDIR}/lib/org.apache.felix.gogo.runtime-${GOGO_RUNTIME_VERSION}.jar

# Gogo JLine
if [ ! -f ${TARGETDIR}/lib/org.apache.felix.gogo.jline-${GOGO_JLINE_VERSION}.jar ] ; then
  echo "Downloading Gogo JLine..."
  wget -O ${TARGETDIR}/lib/org.apache.felix.gogo.jline-${GOGO_JLINE_VERSION}.jar http://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.gogo.jline/${GOGO_JLINE_VERSION}/org.apache.felix.gogo.jline-${GOGO_JLINE_VERSION}.jar
fi
cp=$cp:${TARGETDIR}/lib/org.apache.felix.gogo.jline-${GOGO_JLINE_VERSION}.jar

# Jansi
if [ ! -f ${TARGETDIR}/lib/jansi-${JANSI_VERSION}.jar ] ; then
  echo "Downloading Jansi..."
  wget -O ${TARGETDIR}/lib/jansi-${JANSI_VERSION}.jar http://repo1.maven.org/maven2/org/fusesource/jansi/jansi/${JANSI_VERSION}/jansi-${JANSI_VERSION}.jar
fi

# JNA
if [ ! -f ${TARGETDIR}/lib/jna-${JNA_VERSION}.jar ] ; then
  echo "Downloading JNA..."
  wget -O ${TARGETDIR}/lib/jna-${JNA_VERSION}.jar http://repo1.maven.org/maven2/net/java/dev/jna/jna/${JNA_VERSION}/jna-${JNA_VERSION}.jar
fi

opts=""
while [ "${1}" != "" ]; do
    case ${1} in
        'debug')
            opts="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
            shift
            ;;
        'jansi')
            cp=$cp:${TARGETDIR}/lib/jansi-${JANSI_VERSION}.jar
            shift
            ;;
        'jna')
            cp=$cp:${TARGETDIR}/lib/jna-${JNA_VERSION}.jar
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
java -cp $cp $opts org.apache.felix.gogo.jline.Main

