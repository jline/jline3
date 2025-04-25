@echo off
rem Script to run the Gogo JLine demo

setlocal EnableDelayedExpansion

set SCRIPT_DIR=%~dp0

rem Define gogo-specific help options
set GOGO_HELP=gogo-option

rem Add Gogo Runtime and JLine jars
set TARGET_DIR=%SCRIPT_DIR%target
set CP=%TARGET_DIR%\classes

for %%j in ("%TARGET_DIR%\lib\org.apache.felix.gogo.runtime-*.jar") do (
    set CP=!CP!;%%j
)
for %%j in ("%TARGET_DIR%\lib\org.apache.felix.gogo.jline-*.jar") do (
    set CP=!CP!;%%j
)

rem Process SSH/Telnet/Remote options
set ADD_SSH=false
for %%a in (%*) do (
    if "%%a"=="ssh" set ADD_SSH=true
    if "%%a"=="telnet" set ADD_SSH=true
    if "%%a"=="remote" set ADD_SSH=true
)

if "%ADD_SSH%"=="true" (
    for %%j in ("%TARGET_DIR%\lib\sshd-common-*.jar") do (
        set CP=!CP!;%%j
    )
    for %%j in ("%TARGET_DIR%\lib\sshd-core-*.jar") do (
        set CP=!CP!;%%j
    )
    for %%j in ("%TARGET_DIR%\lib\sshd-scp-*.jar") do (
        set CP=!CP!;%%j
    )
    for %%j in ("%TARGET_DIR%\lib\sshd-sftp-*.jar") do (
        set CP=!CP!;%%j
    )
    for %%j in ("%TARGET_DIR%\lib\slf4j-api-*.jar") do (
        set CP=!CP!;%%j
    )
    for %%j in ("%TARGET_DIR%\lib\slf4j-jdk14-*.jar") do (
        set CP=!CP!;%%j
    )
)

rem Handle help separately
if "%1"=="--help" (
    call :show_help %GOGO_HELP%
    exit /b 0
)

call "%SCRIPT_DIR%jline-common.bat" org.apache.felix.gogo.jline.Main %*
exit /b %ERRORLEVEL%

:show_help
echo Usage: %~nx0 [options]
echo Options:
echo   --help       Show this help message

rem Add demo-specific help options if provided
if not "%~1"=="" (
    if "%~1"=="gogo-option" (
        echo   ssh          Add SSH support
        echo   telnet       Add Telnet support
        echo   remote       Add remote support (SSH and Telnet)
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