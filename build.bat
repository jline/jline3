@echo off
setlocal enabledelayedexpansion

set "project=jline"

:: Get the command
set "command=%~1"
if "%command%"=="" goto :usage

:: Shift arguments
shift

:: Process commands
if "%command%"=="demo" goto :demo
if "%command%"=="example" goto :example
if "%command%"=="rebuild" goto :rebuild
if "%command%"=="license-check" goto :license_check
if "%command%"=="license-format" goto :license_format
if "%command%"=="website" goto :website
if "%command%"=="website-dev" goto :website_dev
if "%command%"=="website-serve" goto :website_serve
if "%command%"=="graal" goto :graal
if "%command%"=="change-version" goto :change_version
if "%command%"=="ci-prepare" goto :ci_prepare
if "%command%"=="ci-build" goto :ci_build
if "%command%"=="release" goto :release

echo Unknown command: %command%
goto :usage

:usage
echo usage: %~n0 ^<command^> [options]
echo.
echo Available commands:
echo   demo ^<type^>           Run a demo ^(gogo, repl, password^)
echo   example ^<className^>   Run an example from org.jline.demo.examples
echo   rebuild               Clean and install the project
echo   license-check         Check license headers
echo   license-format        Format license headers
echo   website               Build the website
echo   website-dev           Start the website development server
echo   website-serve         Serve the built website
echo   graal                 Run the Graal demo
echo   change-version        Change the version of the project
echo   ci-prepare            Prepare for CI build
echo   ci-build              Build for CI
echo   release               Release automation
echo.
echo For more information on a specific command, run: %~n0 ^<command^> --help
exit /b 2

:demo
set "demo_type=%~1"
if "%demo_type%"=="" (
    echo Usage: %~n0 demo ^<type^> [options]
    echo Available demo types:
    echo   gogo      - Run the Gogo shell demo
    echo   repl      - Run the REPL demo
    echo   password  - Run the password demo
    echo   consoleui - Run the ConsoleUI demo
    echo Options:
    echo   --help       Show help message
    echo   debug        Enable remote debugging
    echo   debugs       Enable remote debugging with suspend
    echo   jansi        Add Jansi support
    echo   jna          Add JNA support
    echo   verbose      Enable verbose logging
    echo   ffm          Enable Foreign Function Memory ^(preview^)
    echo.
    echo Gogo-specific options:
    echo   ssh          Add SSH support
    echo   telnet       Add Telnet support
    echo   remote       Add remote support ^(SSH and Telnet^)
    echo.
    echo Password-specific options:
    echo   --mask=X     Use X as the mask character ^(default: *^)
    echo                Use --mask= ^(empty^) for no masking
    exit /b 1
)

shift

:: Set up common variables
set "TARGETDIR=demo\target"
set "cp=%TARGETDIR%\classes"
set "logconf=demo\etc\logging.properties"
set "JVM_OPTS="
set "APP_ARGS="
set "MAIN_CLASS="

:: Add JLine jars
if exist "%TARGETDIR%\lib" (
    for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\jline-*.jar"') do (
        set "cp=!cp!;%%i"
    )
)

:: Set up demo-specific configuration
if "%demo_type%"=="gogo" (
    :: Add Gogo Runtime and JLine
    if exist "%TARGETDIR%\lib" (
        for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\org.apache.felix.gogo.runtime-*.jar"') do (
            set "cp=!cp!;%%i"
        )
        for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\org.apache.felix.gogo.jline-*.jar"') do (
            set "cp=!cp!;%%i"
        )
    )
    set "MAIN_CLASS=org.apache.felix.gogo.jline.Main"
) else if "%demo_type%"=="repl" (
    :: Add Groovy and Ivy jars
    if exist "%TARGETDIR%\lib" (
        for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\groovy-*.jar"') do (
            set "cp=!cp!;%%i"
        )
        for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\ivy-*.jar"') do (
            set "cp=!cp!;%%i"
        )
    )
    set "MAIN_CLASS=org.jline.demo.Repl"
) else if "%demo_type%"=="password" (
    set "MAIN_CLASS=org.jline.demo.PasswordMaskingDemo"
    :: Process mask option separately for password demo
    for %%a in (%*) do (
        set "arg=%%a"
        if "!arg:~0,7!"=="--mask=" (
            set "APP_ARGS=!APP_ARGS! %%a"
        )
    )
) else if "%demo_type%"=="consoleui" (
    :: Add console-ui jar
    if exist "%TARGETDIR%\lib" (
        for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\jline-console-ui-*.jar"') do (
            set "cp=!cp!;%%i"
        )
    )
    set "MAIN_CLASS=org.jline.demo.consoleui.BasicDynamic"
) else (
    echo Unknown demo type: %demo_type%
    exit /b 1
)

:: Process common options
:process_demo_args
if "%~1"=="" goto :run_demo

if "%~1"=="--help" (
    :: Show help based on demo type
    if "%demo_type%"=="gogo" (
        echo Usage: %~n0 demo gogo [options]
        echo Options:
        echo   --help       Show this help message
        echo   ssh          Add SSH support
        echo   telnet       Add Telnet support
        echo   remote       Add remote support ^(SSH and Telnet^)
        echo   debug        Enable remote debugging
        echo   debugs       Enable remote debugging with suspend
        echo   jansi        Add Jansi support
        echo   jna          Add JNA support
        echo   verbose      Enable verbose logging
        echo   ffm          Enable Foreign Function Memory ^(preview^)
        echo.
        echo To test with a dumb terminal, use: set TERM=dumb ^& %~n0 demo gogo
    ) else if "%demo_type%"=="password" (
        echo Usage: %~n0 demo password [options]
        echo Options:
        echo   --help       Show this help message
        echo   --mask=X     Use X as the mask character ^(default: *^)
        echo                Use --mask= ^(empty^) for no masking
        echo   debug        Enable remote debugging
        echo   debugs       Enable remote debugging with suspend
        echo   jansi        Add Jansi support
        echo   jna          Add JNA support
        echo   verbose      Enable verbose logging
        echo   ffm          Enable Foreign Function Memory ^(preview^)
        echo.
        echo To test with a dumb terminal, use: set TERM=dumb ^& %~n0 demo password
    ) else if "%demo_type%"=="consoleui" (
        echo Usage: %~n0 demo consoleui [options]
        echo Options:
        echo   --help       Show this help message
        echo   debug        Enable remote debugging
        echo   debugs       Enable remote debugging with suspend
        echo   jansi        Add Jansi support ^(recommended for Windows^)
        echo   jna          Add JNA support ^(alternative for Windows^)
        echo   verbose      Enable verbose logging
        echo   ffm          Enable Foreign Function Memory ^(preview^)
        echo.
        echo Note: On Windows, either Jansi or JNA library must be included in classpath.
        echo To test with a dumb terminal, use: set TERM=dumb ^& %~n0 demo consoleui
    ) else (
        echo Usage: %~n0 demo %demo_type% [options]
        echo Options:
        echo   --help       Show this help message
        echo   debug        Enable remote debugging
        echo   debugs       Enable remote debugging with suspend
        echo   jansi        Add Jansi support
        echo   jna          Add JNA support
        echo   verbose      Enable verbose logging
        echo   ffm          Enable Foreign Function Memory ^(preview^)
        echo.
        echo To test with a dumb terminal, use: set TERM=dumb ^& %~n0 demo %demo_type%
    )
    exit /b 0
) else if "%~1"=="debug" (
    set "JVM_OPTS=!JVM_OPTS! -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
) else if "%~1"=="debugs" (
    set "JVM_OPTS=!JVM_OPTS! -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
) else if "%~1"=="jansi" (
    for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\jansi-*.jar"') do (
        set "cp=!cp!;%%i"
    )
) else if "%~1"=="jna" (
    for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\jna-*.jar"') do (
        set "cp=!cp!;%%i"
    )
) else if "%~1"=="verbose" (
    set "logconf=demo\etc\logging-verbose.properties"
) else if "%~1"=="ffm" (
    set "JVM_OPTS=!JVM_OPTS! --enable-preview --enable-native-access=org.jline.terminal.ffm"
) else if "%~1"=="ssh" (
    :: Process SSH option for gogo
    if "%demo_type%"=="gogo" (
        if exist "%TARGETDIR%\lib" (
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\sshd-common-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\sshd-core-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\sshd-scp-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\sshd-sftp-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\slf4j-api-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\slf4j-jdk14-*.jar"') do (
                set "cp=!cp!;%%i"
            )
        )
    )
) else if "%~1"=="telnet" (
    :: Process Telnet option for gogo
    if "%demo_type%"=="gogo" (
        if exist "%TARGETDIR%\lib" (
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\sshd-common-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\sshd-core-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\sshd-scp-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\sshd-sftp-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\slf4j-api-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\slf4j-jdk14-*.jar"') do (
                set "cp=!cp!;%%i"
            )
        )
    )
) else if "%~1"=="remote" (
    :: Process Remote option for gogo
    if "%demo_type%"=="gogo" (
        if exist "%TARGETDIR%\lib" (
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\sshd-common-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\sshd-core-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\sshd-scp-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\sshd-sftp-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\slf4j-api-*.jar"') do (
                set "cp=!cp!;%%i"
            )
            for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\slf4j-jdk14-*.jar"') do (
                set "cp=!cp!;%%i"
            )
        )
    )
) else (
    set "arg=%~1"
    if "!arg:~0,7!"=="--mask=" (
        :: Already processed for password demo
        rem Do nothing
    ) else (
        :: Unknown option, assume it's a JVM option
        set "JVM_OPTS=!JVM_OPTS! %~1"
    )
)

shift
goto :process_demo_args

:run_demo
:: Check if JDK version supports --enable-native-access
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "java_version=%%v"
)
set "java_version=%java_version:"=%"
set "java_version=%java_version:~0,2%"

:: Remove any non-numeric characters
set "java_version=%java_version:~0,2%"
if "%java_version:~1,1%"=="." set "java_version=%java_version:~0,1%"

:: Check Java version for --enable-native-access
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set "java_version=%%v"
set "java_version=%java_version:"=%"
set "java_version=%java_version:~0,2%"
if "%java_version:~1,1%"=="." set "java_version=%java_version:~0,1%"
:: Default to JDK 8 behavior if version not detected
if not defined java_version set "java_version=8"
:: Add --enable-native-access only for Java 16+
echo %java_version% | findstr /C:"16" /C:"17" /C:"18" /C:"19" /C:"20" /C:"21" /C:"22" >nul
if not errorlevel 1 set "JVM_OPTS=%JVM_OPTS% --enable-native-access=org.jline.terminal.ffm"

:: Launch the demo
echo Launching %MAIN_CLASS%...
echo Classpath: %cp%
echo JVM options: %JVM_OPTS%
if defined APP_ARGS echo Application arguments: %APP_ARGS%

:: Run Java directly with application arguments
java -cp "%cp%" %JVM_OPTS% -Dgosh.home="demo" -Djava.util.logging.config.file="%logconf%" %MAIN_CLASS% %APP_ARGS%

goto :eof

:example
set "example_name=%~1"
if "%example_name%"=="" (
    echo Usage: %~n0 example ^<ExampleClassName^> [options]
    echo Available examples:
    for /f "tokens=*" %%i in ('dir /b /s demo\src\main\java\org\jline\demo\examples\*.java') do (
        for /f "tokens=* delims=" %%j in ("%%~ni") do echo   %%j
    )
    exit /b 1
)

shift
set "TARGETDIR=demo\target"
set "cp=%TARGETDIR%\classes"

:: Add JLine jars
if exist "%TARGETDIR%\lib" (
    for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\jline-*.jar"') do (
        set "cp=!cp!;%%i"
    )
)

:: Process options
set "JVM_OPTS="
:process_args
if "%~1"=="" goto :run_example
if "%~1"=="debug" (
    set "JVM_OPTS=!JVM_OPTS! -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
) else if "%~1"=="debugs" (
    set "JVM_OPTS=!JVM_OPTS! -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
) else if "%~1"=="jansi" (
    for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\jansi-*.jar"') do (
        set "cp=!cp!;%%i"
    )
) else if "%~1"=="jna" (
    for /f "tokens=*" %%i in ('dir /b /s "%TARGETDIR%\lib\jna-*.jar"') do (
        set "cp=!cp!;%%i"
    )
) else if "%~1"=="ffm" (
    set "JVM_OPTS=!JVM_OPTS! --enable-preview --enable-native-access=org.jline.terminal.ffm"
) else (
    set "JVM_OPTS=!JVM_OPTS! %~1"
)
shift
goto :process_args

:run_example
:: Check if the example class exists
set "found="
for /f "tokens=*" %%i in ('dir /b /s "demo\src\main\java\org\jline\demo\examples\%example_name%.java" 2^>nul') do (
    set "found=yes"
)

if not defined found (
    echo Example class '%example_name%' not found.
    echo Available examples:
    for /f "tokens=*" %%i in ('dir /b /s demo\src\main\java\org\jline\demo\examples\*.java') do (
        for /f "tokens=* delims=" %%j in ("%%~ni") do echo   %%j
    )
    exit /b 1
)

:: Run the example
echo Running example: org.jline.demo.examples.%example_name%
echo Classpath: %cp%
echo JVM options: %JVM_OPTS%

java -cp "%cp%" %JVM_OPTS% org.jline.demo.examples.%example_name%
goto :eof

:rebuild
:: Check if the first argument is "rebuild" and remove it if so
set "mvn_args="
:parse_rebuild_args
if "%~1"=="" goto :do_rebuild
if "%~1"=="rebuild" (
    shift
    goto :parse_rebuild_args
)
set "mvn_args=%mvn_args% %~1"
shift
goto :parse_rebuild_args

:do_rebuild
call mvnw.cmd clean install%mvn_args%
goto :eof

:license_check
call mvnw.cmd -Plicense-check -N %*
goto :eof

:license_format
call mvnw.cmd -Plicense-format -N %*
goto :eof

:website
cd website
echo Installing dependencies...
call npm install

echo Building website...
call npm run build

echo Website built successfully in website\target directory
echo To preview the website, run: %~n0 website-serve
cd ..
goto :eof

:website_dev
cd website
echo Starting development server...
call npm run start-with-snippets
cd ..
goto :eof

:website_serve
cd website
echo Serving website from website\target directory...
call npm run serve
cd ..
goto :eof

:graal
call graal\target\graal.bat %*
goto :eof

:change_version
set "newVersion=%~1"
if "%newVersion%"=="" (
    echo Usage: %~n0 change-version ^<version^>
    exit /b 1
)

call mvnw.cmd org.eclipse.tycho:tycho-versions-plugin:0.25.0:set-version ^
    -Dtycho.mode=maven ^
    -Dartifacts=%project% ^
    -Dproperties=%project%.version ^
    -DnewVersion="%newVersion%"
goto :eof

:ci_prepare
call %~n0 license-check %*
goto :eof

:ci_build
if "%TRAVIS_PULL_REQUEST%"=="false" (
    set "goal=deploy"
) else (
    set "goal=install"
)
call mvnw.cmd clean %goal% %*
goto :eof

:release
set "version=%~1"
set "nextVersion=%~2"
if "%version%"=="" (
    echo Usage: %~n0 release ^<version^> ^<next-version^>
    exit /b 1
)
if "%nextVersion%"=="" (
    echo Usage: %~n0 release ^<version^> ^<next-version^>
    exit /b 1
)
set "releaseTag=release-%version%"

:: update version and tag
call %~n0 change-version "%version%"
git commit -a -m "update version: %version%"
git tag %releaseTag%

:: deploy release
call mvnw.cmd -Pbuildsupport-release clean deploy

:: update to next version
call %~n0 change-version "%nextVersion%"
git commit -a -m "update version: %nextVersion%"
goto :eof
