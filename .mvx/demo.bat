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
  echo   consoleui - Run the ConsoleUI demo ^(deprecated^)
  echo   prompt    - Run the new Prompt API demo
  echo   graal     - Run the GraalVM native demo
  echo.
  echo Example demos ^(from org.jline.demo.examples^):
  echo   Run 'mvx demo ^<ExampleClassName^>' for any example class
  echo   Available examples: ^(see demo/src/main/java/org/jline/demo/examples/^)
  exit /b 1
)

REM Set up classpath
set TARGETDIR=demo\target
set cp=%TARGETDIR%\classes

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
if "%demo_name%"=="consoleui" goto demo_consoleui
if "%demo_name%"=="prompt" goto demo_prompt
if "%demo_name%"=="graal" goto demo_graal
goto example

:demo_gogo
set MAIN_CLASS=org.apache.felix.gogo.jline.Main
goto run_demo

:demo_repl
set MAIN_CLASS=org.jline.demo.Repl
goto run_demo

:demo_password
set MAIN_CLASS=org.jline.demo.PasswordMaskingDemo
goto run_demo

:demo_consoleui
set MAIN_CLASS=org.jline.demo.consoleui.BasicDynamic
goto run_demo

:demo_prompt
set MAIN_CLASS=org.jline.demo.examples.PromptDynamicExample
goto run_demo

:demo_graal
echo Running GraalVM native demo
graal\target\graal %*
goto end

:run_demo
echo Running demo: %MAIN_CLASS%
java -cp "%cp%" -Dgosh.home="demo" %MAIN_CLASS% %*
goto end

:example
set example_name=%demo_name%

REM Check if the example class exists
if not exist demo\src\main\java\org\jline\demo\examples\%example_name%.java (
  echo Demo '%example_name%' not found.
  echo.
  echo Available built-in demos: gogo, repl, password, consoleui, prompt, graal
  echo.
  echo Available example demos:
  for %%f in (demo\src\main\java\org\jline\demo\examples\*.java) do (
    set filename=%%~nf
    echo   !filename!
  )
  exit /b 1
)

echo Running demo: org.jline.demo.examples.%example_name%
java -cp "%cp%" org.jline.demo.examples.%example_name% %*

:end
