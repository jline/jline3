@echo off

set DIRNAME=%~dp0%
set ROOTDIR=%DIRNAME%\..
set TARGETDIR=%ROOTDIR%\target

set JANSI_VERSION=1.14
set JNA_VERSION=4.2.2
set GOGO_RUNTIME_VERSION=1.0.2
set GOGO_JLINE_VERSION=1.0.2

rem initialization
if not exist %TARGETDIR%\lib (
  mkdir %TARGETDIR%\lib
)

rem JLINE
set cp=""
pushd %TARGETDIR%
for %%G in (*.jar) do call:APPEND_TO_CLASSPATH %%G
popd
if not "%cp" == "" goto CLASSPATH_END
    echo Build jline with maven before running the demo
    goto END
: APPEND_TO_CLASSPATH
set filename=%~1
set suffix=%filename:~-4%
if %suffix% equ .jar set cp=%cp%;%TARGETDIR%\%filename%
goto :EOF

:CLASSPATH_END

rem JANSI
if not exist %TARGETDIR%\lib\jansi-%JANSI_VERSION%.jar (
  echo "Downloading Jansi..."
  %DIRNAME%\wget.exe -O %TARGETDIR%\lib\jansi-%JANSI_VERSION%.jar http://repo1.maven.org/maven2/org/fusesource/jansi/jansi/%JANSI_VERSION%/jansi-%JANSI_VERSION%.jar
)
set cp=%cp%;%TARGETDIR%\lib\jansi-%JANSI_VERSION%.jar

rem JNA
if not exist %TARGETDIR%\lib\jna-%JNA_VERSION%.jar (
  echo "Downloading JNA..."
  %DIRNAME%\wget.exe -O %TARGETDIR%\lib\jna-%JNA_VERSION%.jar http://repo1.maven.org/maven2/net/java/dev/jna/jna/%JNA_VERSION%/jna-%JNA_VERSION%.jar
)
set cp=%cp%;%TARGETDIR%\lib\jna-%JNA_VERSION%.jar

rem Gogo Runtime
if not exist %TARGETDIR%\lib\org.apache.felix.gogo.runtime-%GOGO_RUNTIME_VERSION%.jar (
  echo "Downloading Gogo Runtime..."
  %DIRNAME%\wget.exe -O %TARGETDIR%\lib\org.apache.felix.gogo.runtime-%GOGO_RUNTIME_VERSION%.jar http://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.gogo.runtime/%GOGO_RUNTIME_VERSION%/org.apache.felix.gogo.runtime-%GOGO_RUNTIME_VERSION%.jar
)
set cp=%cp%;%TARGETDIR%\lib\org.apache.felix.gogo.runtime-%GOGO_RUNTIME_VERSION%.jar

rem Gogo JLine
if not exist %TARGETDIR%\lib\org.apache.felix.gogo.jline-%GOGO_JLINE_VERSION%.jar (
  echo "Downloading Gogo JLine..."
  %DIRNAME%\wget.exe -O %TARGETDIR%\lib\org.apache.felix.gogo.jline-%GOGO_JLINE_VERSION%.jar http://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.gogo.jline/%GOGO_JLINE_VERSION%/org.apache.felix.gogo.jline-%GOGO_JLINE_VERSION%.jar
)
set cp=%cp%;%TARGETDIR%\lib\org.apache.felix.gogo.jline-%GOGO_JLINE_VERSION%.jar


set opts=
:RUN_LOOP
    if "%1" == "jansi" goto :EXECUTE_JANSI
    if "%1" == "jna" goto :EXECUTE_JNA
    if "%1" == "debug" goto :EXECUTE_DEBUG
    if "%1" == "debugs" goto :EXECUTE_DEBUGS
	goto :EXECUTE

:EXECUTE_JANSI
    set cp=%cp%;%TARGETDIR%/lib/jansi-%JANSI_VERSION%.jar
    shift
    goto :RUN_LOOP

:EXECUTE_JNA
    set cp=%cp%;%TARGETDIR%/lib/jna-%JNA_VERSION%.jar
    shift
    goto :RUN_LOOP

:EXECUTE_DEBUG
    set opts=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
    shift
    goto :RUN_LOOP

:EXECUTE_DEBUGS
    set opts=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005
    shift
    goto :RUN_LOOP

:EXECUTE
rem Launch gogo shell
echo "Classpath: %cp%"
echo "Launching Gogo JLine..."
java -cp %cp% %opts% org.apache.felix.gogo.jline.Main

:END