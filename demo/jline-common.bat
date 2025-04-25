@echo off
rem Common script for JLine demos

setlocal EnableDelayedExpansion

set SCRIPT_DIR=%~dp0
set TARGET_DIR=%SCRIPT_DIR%target

rem Process help option
if "%~1"=="--help" (
    call :show_help %2
    exit /b 0
)

if not exist "%TARGET_DIR%\lib" (
    echo Build jline with maven before running the demo
    exit /b 1
)

set CP=%TARGET_DIR%\classes

rem Add JLine jars
for %%j in ("%TARGET_DIR%\lib\jline-*.jar") do (
    set CP=!CP!;%%j
)

set OPTS=%JLINE_OPTS%
set LOGCONF=%SCRIPT_DIR%etc\logging.properties

:parse_args
if "%~1"=="" goto :end_parse_args
if "%~1"=="debug" (
    set OPTS=%OPTS% -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
    shift
    goto :parse_args
)
if "%~1"=="debugs" (
    set OPTS=%OPTS% -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005
    shift
    goto :parse_args
)
if "%~1"=="jansi" (
    for %%j in ("%TARGET_DIR%\lib\jansi-*.jar") do (
        set CP=!CP!;%%j
    )
    shift
    goto :parse_args
)
if "%~1"=="jna" (
    for %%j in ("%TARGET_DIR%\lib\jna-*.jar") do (
        set CP=!CP!;%%j
    )
    shift
    goto :parse_args
)
if "%~1"=="verbose" (
    set LOGCONF=%SCRIPT_DIR%etc\logging-verbose.properties
    shift
    goto :parse_args
)
if "%~1"=="ffm" (
    set OPTS=%OPTS% --enable-preview --enable-native-access=ALL-UNNAMED
    shift
    goto :parse_args
)
if "%~1"=="--help" (
    call :show_help %2
    exit /b 0
)
set OPTS=%OPTS% %1
shift
goto :parse_args
:end_parse_args

rem Function to run a demo
:run_demo
set MAIN_CLASS=%~1
shift

rem The remaining arguments are application arguments
set APP_ARGS=%*

echo Launching %MAIN_CLASS%...
echo Classpath: %CP%
echo JVM options: %OPTS%
if not "%APP_ARGS%"=="" (
    echo Application arguments: %APP_ARGS%
)

java -cp "%CP%" %OPTS% -Dgosh.home="%SCRIPT_DIR%" -Djava.util.logging.config.file="%LOGCONF%" %MAIN_CLASS% %APP_ARGS%
exit /b 0

:show_help
echo Usage: %~nx0 [options]
echo Options:
echo   --help       Show this help message

rem Add demo-specific help options if provided
if not "%~1"=="" (
    if "%~1"=="mask-option" (
        echo   --mask=X     Use X as the mask character (default: *)
        echo                Use --mask= (empty) for no masking
    ) else if "%~1"=="gogo-option" (
        echo   ssh          Add SSH support
        echo   telnet       Add Telnet support
        echo   remote       Add remote support (SSH and Telnet)
    ) else if "%~1"=="repl-option" (
        rem No REPL-specific options
    ) else (
        echo %~1
    )
)

rem Add common options
echo   debug        Enable remote debugging
echo   debugs       Enable remote debugging with suspend
echo   jansi        Add Jansi support
echo   jna          Add JNA support
echo   verbose      Enable verbose logging
echo   ffm          Enable Foreign Function Memory (preview)
echo.
echo To test with a dumb terminal, use: set TERM=dumb ^& %~nx0
exit /b 0
