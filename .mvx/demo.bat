@echo off
setlocal enabledelayedexpansion

REM Demo runner script for mvx (Windows)
set demo_name=%1
shift

if "%demo_name%"=="" (
  echo Usage: mvx demo ^<name^> [options]
  echo.
  echo Built-in demos:
  echo   gogo      - Run the Gogo shell demo
  echo   repl      - Run the REPL demo
  echo   password  - Run the password demo
  echo   console   - Run the JLine Console Provider demo
  echo   consoleui - Run the ConsoleUI demo ^(deprecated^)
  echo   prompt    - Run the new Prompt API demo
  echo   curses    - Run the Curses TUI demo
  echo   graal     - Run the GraalVM native demo
  echo.
  echo Options:
  echo   debug     - Enable remote debugging
  echo   debugs    - Enable remote debugging with suspend
  echo   verbose   - Enable verbose logging
  echo   ffm       - Enable Foreign Function Memory
  echo.
  echo Password-specific options:
  echo   --mask=X  - Use X as the mask character ^(default: *^)
  echo               Use --mask= ^(empty^) for no masking
  echo.
  echo Example demos ^(from org.jline.demo.examples^):
  echo   Run 'mvx demo ^<ExampleClassName^>' for any example class
  echo   Available examples: ^(see demo/src/main/java/org/jline/demo/examples/^)
  exit /b 1
)

REM Set up classpath and options
set TARGETDIR=demo\target
set cp=%TARGETDIR%\classes
set logconf=demo\etc\logging.properties
set JVM_OPTS=
set APP_ARGS=
set MAIN_CLASS=

REM Add JLine jars
if exist %TARGETDIR%\lib (
  for %%f in (%TARGETDIR%\lib\*.jar) do (
    set cp=!cp!;%%f
  )
)

REM Determine if this is a built-in demo or example class based on known demo types
if "%demo_name%"=="gogo" goto demo_gogo
if "%demo_name%"=="repl" goto demo_repl
if "%demo_name%"=="password" goto demo_password
if "%demo_name%"=="console" goto demo_console
if "%demo_name%"=="consoleui" goto demo_consoleui
if "%demo_name%"=="prompt" goto demo_prompt
if "%demo_name%"=="curses" goto demo_curses
if "%demo_name%"=="graal" goto demo_graal
goto example

:demo_gogo
set MAIN_CLASS=org.apache.felix.gogo.jline.Main
goto process_options

:demo_repl
set MAIN_CLASS=org.jline.demo.Repl
goto process_options

:demo_password
set MAIN_CLASS=org.jline.demo.PasswordMaskingDemo
goto process_options

:demo_console
set MAIN_CLASS=org.jline.demo.examples.ConsoleProviderExample
goto process_options

:demo_consoleui
set MAIN_CLASS=org.jline.demo.consoleui.BasicDynamic
goto process_options

:demo_prompt
set MAIN_CLASS=org.jline.demo.examples.PromptDynamicExample
goto process_options

:demo_curses
set MAIN_CLASS=org.jline.demo.CursesDemo
goto process_options

:demo_graal
echo Running GraalVM native demo
graal\target\graal %*
goto end

:example
set example_name=%demo_name%

REM Check if the example class exists
if not exist demo\src\main\java\org\jline\demo\examples\%example_name%.java (
  echo Demo '%example_name%' not found.
  echo.
  echo Available built-in demos: gogo, repl, password, console, consoleui, prompt, curses, graal
  echo.
  echo Available example demos:
  for %%f in (demo\src\main\java\org\jline\demo\examples\*.java) do (
    set filename=%%~nf
    echo   !filename!
  )
  exit /b 1
)

set MAIN_CLASS=org.jline.demo.examples.%example_name%
goto process_options

:process_options
REM Process options
:options_loop
if "%1"=="" goto run_demo
if "%1"=="debug" (
  if "!JVM_OPTS!"=="" (
    set JVM_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
  ) else (
    set JVM_OPTS=!JVM_OPTS! -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
  )
  shift
  goto options_loop
)
if "%1"=="debugs" (
  if "!JVM_OPTS!"=="" (
    set JVM_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005
  ) else (
    set JVM_OPTS=!JVM_OPTS! -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005
  )
  shift
  goto options_loop
)
if "%1"=="verbose" (
  set logconf=demo\etc\logging-verbose.properties
  shift
  goto options_loop
)
if "%1"=="ffm" (
  if "!JVM_OPTS!"=="" (
    set JVM_OPTS=--enable-native-access=ALL-UNNAMED
  ) else (
    set JVM_OPTS=!JVM_OPTS! --enable-native-access=ALL-UNNAMED
  )
  shift
  goto options_loop
)
REM Remaining arguments (--mask= or any unknown option) go to the application.
REM Disable delayed expansion while reading %1 so ! characters are preserved,
REM then re-enable to append to APP_ARGS.  The for /f variable %%a survives
REM endlocal, carrying the concatenated value back to the outer scope.
setlocal disabledelayedexpansion
set "da_arg=%~1"
setlocal enabledelayedexpansion
for /f "delims=" %%a in ("!APP_ARGS! "!da_arg!"") do endlocal & endlocal & set "APP_ARGS=%%a"
shift
goto options_loop

:run_demo
echo Running demo: %MAIN_CLASS%
REM Display with delayed expansion disabled to preserve ! in argument values.
setlocal disabledelayedexpansion
if defined JVM_OPTS echo JVM options: %JVM_OPTS%
if defined APP_ARGS echo Application arguments: %APP_ARGS%
endlocal

if "%demo_name%"=="console" goto run_console_demo

REM Disable delayed expansion so ! and special characters in arguments reach Java literally.
REM All variables already have their final values, so early expansion (%var%) is safe.
setlocal disabledelayedexpansion
if "%JVM_OPTS%"=="" (
  java -cp "%cp%" -Dgosh.home="demo" -Djava.util.logging.config.file="%logconf%" %MAIN_CLASS% %APP_ARGS%
) else (
  java -cp "%cp%" %JVM_OPTS% -Dgosh.home="demo" -Djava.util.logging.config.file="%logconf%" %MAIN_CLASS% %APP_ARGS%
)
endlocal
goto end

:run_console_demo
REM Console provider demo: use module path so System.console() picks up the JLine provider
set mp=
if exist %TARGETDIR%\lib (
  for %%f in (%TARGETDIR%\lib\jline-*.jar) do (
    if "!mp!"=="" (
      set mp=%%f
    ) else (
      set mp=!mp!;%%f
    )
  )
)
echo Using module path for console provider
REM Disable delayed expansion so ! and special characters in arguments reach Java literally.
setlocal disabledelayedexpansion
if "%JVM_OPTS%"=="" (
  java --module-path "%mp%" ^
    --add-modules org.jline.console.provider,org.jline.reader,org.jline.terminal ^
    --add-exports java.base/jdk.internal.io=org.jline.console.provider ^
    --enable-native-access=org.jline.terminal.ffm ^
    -Djdk.console=org.jline.console.provider ^
    -cp "%TARGETDIR%\classes" ^
    -Dgosh.home="demo" ^
    -Djava.util.logging.config.file="%logconf%" ^
    %MAIN_CLASS% %APP_ARGS%
) else (
  java --module-path "%mp%" ^
    --add-modules org.jline.console.provider,org.jline.reader,org.jline.terminal ^
    --add-exports java.base/jdk.internal.io=org.jline.console.provider ^
    --enable-native-access=org.jline.terminal.ffm ^
    -Djdk.console=org.jline.console.provider ^
    -cp "%TARGETDIR%\classes" ^
    %JVM_OPTS% ^
    -Dgosh.home="demo" ^
    -Djava.util.logging.config.file="%logconf%" ^
    %MAIN_CLASS% %APP_ARGS%
)
endlocal
goto end

:end
