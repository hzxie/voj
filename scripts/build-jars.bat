@echo off
setlocal EnableExtensions

REM ===========================================================================
REM Build the Spring Boot artifacts for Verwandlung Online Judge (Windows).
REM
REM Produces two self-contained, executable artifacts:
REM   web\target\voj.web.war        - the web application (embedded Tomcat)
REM   judger\target\voj.judger.jar  - the judger (bundles its JNI native DLL)
REM
REM Run them with scripts\run-web.bat and scripts\run-judger.bat.
REM
REM The judger compiles a JNI native library (JudgerCore.dll) via judger\Makefile.
REM That needs the MinGW toolchain (g++, make) plus a few Unix tools (sh, mkdir,
REM rm) which ship with Git for Windows; this script puts both on PATH and copies
REM the win32 JNI headers (jni_md.h / jawt_md.h) next to jni.h so the build finds
REM them.
REM
REM Usage:
REM   scripts\build-jars.bat            build both artifacts
REM   scripts\build-jars.bat web        build only the web WAR
REM   scripts\build-jars.bat judger     build only the judger JAR
REM
REM Overridable environment variables (defaults shown):
REM   JDK_HOME=%%JAVA_HOME%% or D:\Applications\OpenJDK   (Spring 7 supports JDK 17-25)
REM   MAVEN_HOME=D:\Applications\Maven
REM   MINGW_HOME=D:\Applications\MINGW
REM   GIT_HOME=D:\Applications\Git
REM   SKIP_TESTS=1                       set to 0 to run tests during the build
REM ===========================================================================

if not defined JDK_HOME if defined JAVA_HOME set "JDK_HOME=%JAVA_HOME%"
if not defined JDK_HOME set "JDK_HOME=D:\Applications\OpenJDK"
if not defined MAVEN_HOME set "MAVEN_HOME=D:\Applications\Maven"
if not defined MINGW_HOME set "MINGW_HOME=D:\Applications\MINGW"
if not defined GIT_HOME set "GIT_HOME=D:\Applications\Git"
if not defined SKIP_TESTS set "SKIP_TESTS=1"

set "TARGET=%~1"
if "%TARGET%"=="" set "TARGET=all"
if /I not "%TARGET%"=="all" if /I not "%TARGET%"=="web" if /I not "%TARGET%"=="judger" (
  echo Usage: %~nx0 [web^|judger^|all] 1>&2
  exit /b 1
)

REM --- Resolve project root (parent of scripts\) -----------------------------
pushd "%~dp0.." || exit /b 1
set "PROJECT_ROOT=%CD%"

REM --- Validate the JDK ------------------------------------------------------
if not exist "%JDK_HOME%\bin\java.exe" (
  echo ERROR: no JDK at JDK_HOME=%JDK_HOME% ^(install OpenJDK or override JDK_HOME^). 1>&2
  popd & exit /b 1
)
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  echo ERROR: no Maven at MAVEN_HOME=%MAVEN_HOME% ^(install Maven or override MAVEN_HOME^). 1>&2
  popd & exit /b 1
)
set "JAVA_HOME=%JDK_HOME%"
REM Git's Unix tools (mkdir, rm, sh) must precede MinGW's bin: MinGW ships no
REM mkdir, and the judger Makefile's recipes (mkdir -p / rm -rf) run under sh.
REM MinGW still supplies g++ and make, which Git does not provide.
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%GIT_HOME%\usr\bin;%MINGW_HOME%\bin;%PATH%"

echo ==^> JDK:
"%JAVA_HOME%\bin\java.exe" -version

REM --- Prepare the native (JNI) build toolchain when building the judger -----
if /I "%TARGET%"=="web" goto :skip_native
call :setup_native || (popd & exit /b 1)
:skip_native

set "MVN_ARGS="
if "%SKIP_TESTS%"=="1" set "MVN_ARGS=-DskipTests"

if /I "%TARGET%"=="web" (
  echo ==^> Building web WAR ...
  call mvn %MVN_ARGS% clean package -pl web
) else if /I "%TARGET%"=="judger" (
  echo ==^> Building judger JAR ...
  call mvn %MVN_ARGS% clean package -pl judger
) else (
  echo ==^> Building web WAR + judger JAR ...
  call mvn %MVN_ARGS% clean package
)
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  echo ==^> Build FAILED ^(exit %RC%^). 1>&2
  popd & exit /b %RC%
)

echo ==^> Done. Artifacts:
if exist "web\target\voj.web.war"        dir /b "web\target\voj.web.war"
if exist "judger\target\voj.judger.jar"  dir /b "judger\target\voj.judger.jar"
popd
exit /b 0

REM ===========================================================================
REM Sets up the toolchain the judger's JNI Makefile needs:
REM   * MinGW g++ and make on PATH (make.exe synthesised from mingw32-make if
REM     absent), with Git's Unix tools (sh, mkdir, rm) so the Makefile recipes run
REM   * the win32 JNI headers copied next to jni.h
REM ===========================================================================
:setup_native
if not exist "%MINGW_HOME%\bin\g++.exe" (
  echo ERROR: no g++ at MINGW_HOME=%MINGW_HOME% ^(install MinGW-w64 or override MINGW_HOME^). 1>&2
  exit /b 1
)
if not exist "%GIT_HOME%\usr\bin\sh.exe" (
  echo ERROR: no Unix shell at %GIT_HOME%\usr\bin ^(install Git for Windows or override GIT_HOME^). 1>&2
  echo        The judger Makefile uses mkdir -p / rm -rf, which need these tools. 1>&2
  exit /b 1
)
REM The pom invokes `make`; MinGW ships it as mingw32-make.exe.
if not exist "%MINGW_HOME%\bin\make.exe" (
  if exist "%MINGW_HOME%\bin\mingw32-make.exe" (
    echo ==^> Creating make.exe from mingw32-make.exe ...
    copy /y "%MINGW_HOME%\bin\mingw32-make.exe" "%MINGW_HOME%\bin\make.exe" >nul || (
      echo ERROR: could not create %MINGW_HOME%\bin\make.exe ^(check write permission^). 1>&2
      exit /b 1
    )
  ) else (
    echo ERROR: neither make.exe nor mingw32-make.exe found in %MINGW_HOME%\bin. 1>&2
    exit /b 1
  )
)
REM jni.h does #include "jni_md.h"; on Windows that header lives in include\win32.
if not exist "%JAVA_HOME%\include\jni_md.h" (
  if exist "%JAVA_HOME%\include\win32\jni_md.h" (
    echo ==^> Copying win32 JNI headers next to jni.h ...
    copy /y "%JAVA_HOME%\include\win32\jni_md.h"  "%JAVA_HOME%\include\jni_md.h"  >nul
    copy /y "%JAVA_HOME%\include\win32\jawt_md.h" "%JAVA_HOME%\include\jawt_md.h" >nul
  )
)
exit /b 0
