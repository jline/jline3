@echo off
rem Script to run the LineReaderMouseExample

setlocal EnableDelayedExpansion

set SCRIPT_DIR=%~dp0

rem Separate JVM options from application arguments
set JVM_ARGS=
set APP_ARGS=

:parse_args
if "%~1"=="" goto :end_parse_args
set ARG=%~1

if "%ARG%"=="--help" (
    rem Help is handled by jline-common.bat
    set JVM_ARGS=!JVM_ARGS! %ARG%
) else if "%ARG%"=="debug" (
    set JVM_ARGS=!JVM_ARGS! %ARG%
) else if "%ARG%"=="debugs" (
    set JVM_ARGS=!JVM_ARGS! %ARG%
) else if "%ARG%"=="jansi" (
    set JVM_ARGS=!JVM_ARGS! %ARG%
) else if "%ARG%"=="jna" (
    set JVM_ARGS=!JVM_ARGS! %ARG%
) else if "%ARG%"=="verbose" (
    set JVM_ARGS=!JVM_ARGS! %ARG%
) else if "%ARG%"=="ffm" (
    set JVM_ARGS=!JVM_ARGS! %ARG%
) else (
    rem Unknown option, pass to both
    set JVM_ARGS=!JVM_ARGS! %ARG%
    set APP_ARGS=!APP_ARGS! %ARG%
)

shift
goto :parse_args
:end_parse_args

call "%SCRIPT_DIR%jline-common.bat" org.jline.demo.examples.LineReaderMouseExample %JVM_ARGS% %APP_ARGS%
exit /b %ERRORLEVEL%

:show_help
echo Usage: %~nx0 [options]
echo Options:
echo   --help       Show this help message

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
