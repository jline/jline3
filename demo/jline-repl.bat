@echo off
rem Script to run the Groovy REPL demo

setlocal EnableDelayedExpansion

set SCRIPT_DIR=%~dp0

rem No REPL-specific help options
set REPL_HELP=repl-option

rem Add Groovy and Ivy jars
set TARGET_DIR=%SCRIPT_DIR%target
set CP=%TARGET_DIR%\classes

for %%j in ("%TARGET_DIR%\lib\groovy-*.jar") do (
    set CP=!CP!;%%j
)
for %%j in ("%TARGET_DIR%\lib\ivy-*.jar") do (
    set CP=!CP!;%%j
)

rem Handle help separately
if "%1"=="--help" (
    call :show_help %REPL_HELP%
    exit /b 0
)

call "%SCRIPT_DIR%jline-common.bat" org.jline.demo.Repl %*
exit /b %ERRORLEVEL%

:show_help
echo Usage: %~nx0 [options]
echo Options:
echo   --help       Show this help message

rem Add demo-specific help options if provided
if not "%~1"=="" (
    if "%~1"=="repl-option" (
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