@echo off
rem Script to run the PasswordMaskingDemo

setlocal EnableDelayedExpansion

set SCRIPT_DIR=%~dp0

rem Define password-specific help options
set PASSWORD_HELP=mask-option

rem Separate JVM options from application arguments
set JVM_ARGS=
set APP_ARGS=

:parse_args
if "%~1"=="" goto :end_parse_args
set ARG=%~1

if "%ARG:~0,7%"=="--mask=" (
    rem This is an application argument
    set APP_ARGS=!APP_ARGS! %ARG%
) else if "%ARG%"=="--help" (
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

rem Handle help separately
if "%JVM_ARGS%"=="--help" (
    call :show_help %PASSWORD_HELP%
    exit /b 0
)

call "%SCRIPT_DIR%jline-common.bat" org.jline.demo.PasswordMaskingDemo %JVM_ARGS% %APP_ARGS%
exit /b %ERRORLEVEL%

:show_help
echo Usage: %~nx0 [options]
echo Options:
echo   --help       Show this help message

rem Add demo-specific help options if provided
if not "%~1"=="" (
    if "%~1"=="mask-option" (
        echo   --mask=X     Use X as the mask character (default: *)
        echo                Use --mask= (empty) for no masking
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
