@echo off

set DIRNAME=%~dp0%
set ROOTDIR=%DIRNAME%\..
set TARGETDIR=%DIRNAME%target

rem initialization
if not exist %TARGETDIR%\lib (
    echo Build jline with maven before running the demo
    goto END
)

goto :SETUP_CLASSPATH

: APPEND_TO_CLASSPATH
set filename=%~1
set cp=%cp%;%TARGETDIR%\lib\%filename%
goto :EOF

:SETUP_CLASSPATH
set cp=%TARGETDIR%\classes;%TARGETDIR%\test-classes
rem JLINE
pushd %TARGETDIR%\lib
for %%G in (jline-*.jar) do call:APPEND_TO_CLASSPATH %%G

set "opts=%JLINE_OPTS%"
:RUN_LOOP
    if "%1" == "jansi" goto :EXECUTE_JANSI
    if "%1" == "jna" goto :EXECUTE_JNA
    if "%1" == "debug" goto :EXECUTE_DEBUG
    if "%1" == "debugs" goto :EXECUTE_DEBUGS
    if "%1" == "" goto :EXECUTE_MAIN
    set "opts=%opts% %~1"
    shift
    goto :RUN_LOOP

:EXECUTE_JANSI
    for %%G in (jansi-*.jar) do call:APPEND_TO_CLASSPATH %%G
    shift
    goto :RUN_LOOP

:EXECUTE_JNA
    for %%G in (jna-*.jar) do call:APPEND_TO_CLASSPATH %%G
    shift
    goto :RUN_LOOP

:EXECUTE_DEBUG
    set "opts=%opts% -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
    shift
    goto :RUN_LOOP

:EXECUTE_DEBUGS
    set "opts=%opts% -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
    shift
    goto :RUN_LOOP

:EXECUTE_MAIN
popd

echo Launching ConsoleUI...
echo Classpath: %cp%
java -cp %cp% %opts% org.jline.consoleui.examples.BasicDynamic

:END