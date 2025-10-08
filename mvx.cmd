@echo off
rem ##############################################################################
rem mvx Bootstrap Script for Windows
rem
rem This script acts as a bootstrap for mvx, automatically downloading and
rem caching the appropriate binary version for Windows.
rem
rem Similar to Maven Wrapper (mvnw.cmd), this allows projects to use mvx without
rem requiring users to install it separately.
rem ##############################################################################

setlocal enabledelayedexpansion

rem Default values
set DEFAULT_MVX_VERSION=latest
set DEFAULT_DOWNLOAD_URL=https://github.com/gnodet/mvx/releases

rem Determine the mvx version to use
set MVX_VERSION_TO_USE=%MVX_VERSION%
if "%MVX_VERSION_TO_USE%"=="" (
    if exist ".mvx\mvx.properties" (
        for /f "tokens=2 delims==" %%i in ('findstr "^mvxVersion=" ".mvx\mvx.properties" 2^>nul') do set MVX_VERSION_TO_USE=%%i
    )
)
if "%MVX_VERSION_TO_USE%"=="" (
    set MVX_VERSION_TO_USE=%DEFAULT_MVX_VERSION%
)

rem Remove any whitespace
set MVX_VERSION_TO_USE=%MVX_VERSION_TO_USE: =%

rem Determine download URL
set DOWNLOAD_URL_TO_USE=%MVX_DOWNLOAD_URL%
if "%DOWNLOAD_URL_TO_USE%"=="" (
    set DOWNLOAD_URL_TO_USE=%DEFAULT_DOWNLOAD_URL%
)

rem Get user home directory
set HOME_DIR=%USERPROFILE%
if "%HOME_DIR%"=="" set HOME_DIR=%HOMEDRIVE%%HOMEPATH%
if "%HOME_DIR%"=="" set HOME_DIR=.

rem Check verbosity level
set VERBOSITY=normal
:check_args
if "%~1"=="" goto args_done
if "%~1"=="-v" set VERBOSITY=verbose
if "%~1"=="--verbose" set VERBOSITY=verbose
if "%~1"=="-q" set VERBOSITY=quiet
if "%~1"=="--quiet" set VERBOSITY=quiet
shift
goto check_args
:args_done

rem Handle update-bootstrap command specially
if "%1"=="update-bootstrap" (
    call :handle_update_bootstrap
    exit /b !errorlevel!
)

rem Only show bootstrap info in verbose mode
if "%VERBOSITY%"=="verbose" (
    echo mvx
    echo Platform: windows-amd64
    echo Requested version: %MVX_VERSION_TO_USE%
)

rem Check for local development binaries only when explicitly using dev version
if "%MVX_VERSION_TO_USE%"=="dev" (
    if exist ".\mvx-dev.exe" (
        if "%VERBOSITY%"=="verbose" (
            echo Using local development binary: .\mvx-dev.exe
            echo.
        )
        ".\mvx-dev.exe" %*
        goto :eof
    )

    rem Check for global development binary (shared across all projects)
    set GLOBAL_DEV_BINARY=%HOME_DIR%\.mvx\dev\mvx.exe
    if exist "%GLOBAL_DEV_BINARY%" (
        if "%VERBOSITY%"=="verbose" (
            echo Using global development binary: %GLOBAL_DEV_BINARY%
            echo.
        )
        "%GLOBAL_DEV_BINARY%" %*
        goto :eof
    )
)

rem Resolve version (handle "latest")
set RESOLVED_VERSION=%MVX_VERSION_TO_USE%
if "%MVX_VERSION_TO_USE%"=="latest" (
    echo Resolving latest version...
    call :get_latest_version RESOLVED_VERSION
    if errorlevel 1 (
        echo Error: Could not determine latest version
        exit /b 1
    )
    echo Latest version: !RESOLVED_VERSION!
)

rem Check cached version
set CACHE_DIR=%HOME_DIR%\.mvx\versions\%RESOLVED_VERSION%
set CACHED_BINARY=%CACHE_DIR%\mvx.exe

if exist "%CACHED_BINARY%" (
    echo Using cached mvx binary: %CACHED_BINARY%
    echo.
    "%CACHED_BINARY%" %*
    goto :eof
)

rem Need to download
echo mvx %RESOLVED_VERSION% not found, downloading...

rem Create cache directory
if not exist "%CACHE_DIR%" mkdir "%CACHE_DIR%"

rem Construct download URL
set BINARY_NAME=mvx-windows-amd64.exe
set DOWNLOAD_URL_FULL=%DOWNLOAD_URL_TO_USE%/download/v%RESOLVED_VERSION%/%BINARY_NAME%

echo Downloading mvx %RESOLVED_VERSION% for windows-amd64...
echo Downloading from: %DOWNLOAD_URL_FULL%

rem Download using PowerShell (available on Windows 7+ with .NET 4.0+)
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DOWNLOAD_URL_FULL%' -OutFile '%CACHED_BINARY%' -UseBasicParsing}"

if not exist "%CACHED_BINARY%" (
    echo Error: Failed to download mvx binary
    exit /b 1
)

if "%VERBOSITY%"=="verbose" (
    echo Using mvx binary: %CACHED_BINARY%
    echo.
)

rem Execute mvx with all arguments
"%CACHED_BINARY%" %*
goto :eof

rem Function to get latest version from GitHub API
:get_latest_version
set "result_var=%~1"
set API_URL=https://api.github.com/repos/gnodet/mvx/releases/latest

rem Use PowerShell to get the latest version
for /f "delims=" %%i in ('powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $response = Invoke-RestMethod -Uri '%API_URL%' -UseBasicParsing; $response.tag_name -replace '^v', ''}"') do (
    set "%result_var%=%%i"
)

if "!%result_var%!"=="" (
    exit /b 1
)
exit /b 0

rem Function to handle update-bootstrap command
:handle_update_bootstrap
echo 🔍 Checking for mvx bootstrap updates...

rem Get latest version
call :get_latest_version LATEST_VERSION
if errorlevel 1 (
    echo Error: Failed to get latest version
    exit /b 1
)

rem Get current version from properties file
set CURRENT_VERSION=
if exist ".mvx\mvx.properties" (
    for /f "tokens=2 delims==" %%i in ('findstr "^mvxVersion=" ".mvx\mvx.properties" 2^>nul') do set CURRENT_VERSION=%%i
)

if "%CURRENT_VERSION%"=="" (
    echo No current version found, will update to latest
) else if "%CURRENT_VERSION%"=="%LATEST_VERSION%" (
    echo ✅ Bootstrap scripts are already up to date ^(version %CURRENT_VERSION%^)
    exit /b 0
) else (
    echo 📦 Update available: %CURRENT_VERSION% → %LATEST_VERSION%
)

echo ⬇️  Downloading bootstrap scripts...

set BASE_URL=https://raw.githubusercontent.com/gnodet/mvx/v%LATEST_VERSION%

rem Download mvx script
echo Downloading mvx script...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%BASE_URL%/mvx' -OutFile 'mvx.new' -UseBasicParsing}"
if exist "mvx.new" (
    move "mvx.new" "mvx" >nul
) else (
    echo Error: Failed to download mvx script
    exit /b 1
)

rem Download mvx.cmd script
echo Downloading mvx.cmd script...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%BASE_URL%/mvx.cmd' -OutFile 'mvx.cmd.new' -UseBasicParsing}"
if exist "mvx.cmd.new" (
    move "mvx.cmd.new" "mvx.cmd" >nul
) else (
    echo Error: Failed to download mvx.cmd script
    exit /b 1
)

rem Create .mvx directory if it doesn't exist
if not exist ".mvx" mkdir ".mvx"

rem Update mvx.properties with new version
if exist ".mvx\mvx.properties" (
    rem Update existing properties file
    powershell -Command "& {(Get-Content '.mvx\mvx.properties') -replace '^mvxVersion=.*', 'mvxVersion=%LATEST_VERSION%' | Set-Content '.mvx\mvx.properties'}"
) else (
    rem Download and update properties file
    echo Downloading mvx.properties...
    powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%BASE_URL%/.mvx/mvx.properties' -OutFile '.mvx\mvx.properties' -UseBasicParsing}" 2>nul
    if exist ".mvx\mvx.properties" (
        powershell -Command "& {(Get-Content '.mvx\mvx.properties') -replace '^mvxVersion=.*', 'mvxVersion=%LATEST_VERSION%' | Set-Content '.mvx\mvx.properties'}"
    ) else (
        rem Create minimal properties file if download fails
        echo # mvx Configuration > ".mvx\mvx.properties"
        echo mvxVersion=%LATEST_VERSION% >> ".mvx\mvx.properties"
    )
)

echo ✅ Bootstrap scripts updated successfully to version %LATEST_VERSION%
echo 📝 Files updated:
echo   - mvx ^(Unix/Linux/macOS script^)
echo   - mvx.cmd ^(Windows script^)
echo   - .mvx\mvx.properties ^(version specification^)

exit /b 0
