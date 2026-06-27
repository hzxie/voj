@echo off
setlocal EnableExtensions

REM ===========================================================================
REM Local test runner for Verwandlung Online Judge (Windows).
REM
REM Runs the web and judger module test suites against a local MySQL instance,
REM mirroring the AppVeyor CI flow. Unlike macOS, Windows CAN build the judger's
REM JNI native library, so the judger tests run here too (they need the MinGW
REM toolchain + Git's Unix tools; see scripts\build-jars.bat).
REM
REM Usage:
REM   scripts\test-local.bat
REM   scripts\test-local.bat web        run only the web module tests
REM   scripts\test-local.bat judger     run only the judger module tests
REM
REM NOTE: the judger tests log in as a low-privilege local account
REM (system.username / system.password in voj-test-windows.properties, default
REM 'appveyor'). On your machine that account must exist, or override it via
REM SYSTEM_USERNAME / SYSTEM_PASSWORD below, otherwise the runner tests fail.
REM
REM Overridable environment variables (defaults shown):
REM   JDK_HOME=%%JAVA_HOME%% or D:\Applications\OpenJDK   (Spring 7 supports JDK 17-25)
REM   MAVEN_HOME=D:\Applications\Maven
REM   MINGW_HOME=D:\Applications\MINGW
REM   GIT_HOME=D:\Applications\Git
REM   MYSQL_BIN=mysql
REM   DB_HOST=127.0.0.1  DB_PORT=3306  DB_USER=root  DB_PASS=  TEST_DB=test
REM   SYSTEM_USERNAME=   SYSTEM_PASSWORD=   (override the judger sandbox account)
REM   KEEP_DB=0          set to 1 to keep the test database after the run
REM ===========================================================================

if not defined JDK_HOME if defined JAVA_HOME set "JDK_HOME=%JAVA_HOME%"
if not defined JDK_HOME set "JDK_HOME=D:\Applications\OpenJDK"
if not defined MAVEN_HOME set "MAVEN_HOME=D:\Applications\Maven"
if not defined MINGW_HOME set "MINGW_HOME=D:\Applications\MINGW"
if not defined GIT_HOME set "GIT_HOME=D:\Applications\Git"
if not defined MYSQL_BIN set "MYSQL_BIN=mysql"
if not defined DB_HOST set "DB_HOST=127.0.0.1"
if not defined DB_PORT set "DB_PORT=3306"
if not defined DB_USER set "DB_USER=root"
if not defined DB_PASS set "DB_PASS="
if not defined TEST_DB set "TEST_DB=test"
if not defined KEEP_DB set "KEEP_DB=0"

set "TARGET=%~1"
if "%TARGET%"=="" set "TARGET=all"
if /I not "%TARGET%"=="all" if /I not "%TARGET%"=="web" if /I not "%TARGET%"=="judger" (
  echo Usage: %~nx0 [web^|judger^|all] 1>&2
  exit /b 1
)

pushd "%~dp0.." || exit /b 1
set "PROJECT_ROOT=%CD%"

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

REM --- Native toolchain (needed for the judger module's JNI build) -----------
if /I not "%TARGET%"=="web" (
  call :setup_native || (popd & exit /b 1)
)

set "MYSQL_AUTH=-h %DB_HOST% -P %DB_PORT% -u %DB_USER%"
if not "%DB_PASS%"=="" set "MYSQL_AUTH=%MYSQL_AUTH% -p%DB_PASS%"

REM --- 1. Verify MySQL is reachable ------------------------------------------
echo ==^> Checking MySQL at %DB_HOST%:%DB_PORT% ...
%MYSQL_BIN% %MYSQL_AUTH% -e "SELECT VERSION();" >nul 2>&1
if errorlevel 1 (
  echo ERROR: cannot connect to MySQL at %DB_HOST%:%DB_PORT%. 1>&2
  echo        Start MySQL first, or override DB_* / MYSQL_BIN. 1>&2
  popd & exit /b 1
)

REM --- 2. Test DB provisioning is automatic ---------------------------------
REM The Spring test context runs sql\schema.sql + seed.sql + test-data.sql and the
REM test JDBC URL carries createDatabaseIfNotExist=true plus the required sql_mode,
REM so there is nothing to create or load here. The cleanup at the end drops the
REM test database afterwards (unless KEEP_DB=1).

REM --- 3. Stage the judger's Windows test properties -------------------------
if /I not "%TARGET%"=="web" (
  copy /y "judger\src\test\resources\voj-test-windows.properties" "judger\src\test\resources\voj-test.properties" >nul
  if defined SYSTEM_USERNAME (
    echo ==^> Overriding judger sandbox account with SYSTEM_USERNAME=%SYSTEM_USERNAME% ...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$f='judger\src\test\resources\voj-test.properties'; $c=Get-Content -Raw $f; $c=$c -replace '(?m)^system\.username\s*=.*$', ('system.username = ' + $env:SYSTEM_USERNAME); $c=$c -replace '(?m)^system\.password\s*=.*$', ('system.password = ' + $env:SYSTEM_PASSWORD); Set-Content -NoNewline -Path $f -Value $c"
  )
)

set "RC=0"

REM --- 4. Run the web module tests -------------------------------------------
if /I not "%TARGET%"=="judger" (
  echo ==^> Running web module tests ...
  call mvn test -f web\pom.xml
  if errorlevel 1 set "RC=1"
)

REM --- 5. Run the judger module tests ----------------------------------------
if /I not "%TARGET%"=="web" (
  if "%RC%"=="0" (
    echo ==^> Running judger module tests ...
    call mvn test -f judger\pom.xml
    if errorlevel 1 set "RC=1"
  ) else (
    echo ==^> Skipping judger tests because the web tests failed. 1>&2
  )
)

REM --- 6. Cleanup ------------------------------------------------------------
if not "%KEEP_DB%"=="1" (
  echo ==^> Dropping test database '%TEST_DB%' ...
  %MYSQL_BIN% %MYSQL_AUTH% -e "DROP DATABASE IF EXISTS `%TEST_DB%`;" 2>nul
)

if "%RC%"=="0" ( echo ==^> Done. ) else ( echo ==^> Tests FAILED. 1>&2 )
popd
exit /b %RC%

REM ===========================================================================
REM Sets up the judger's JNI build toolchain (make + win32 JNI headers).
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
if not exist "%JAVA_HOME%\include\jni_md.h" (
  if exist "%JAVA_HOME%\include\win32\jni_md.h" (
    echo ==^> Copying win32 JNI headers next to jni.h ...
    copy /y "%JAVA_HOME%\include\win32\jni_md.h"  "%JAVA_HOME%\include\jni_md.h"  >nul
    copy /y "%JAVA_HOME%\include\win32\jawt_md.h" "%JAVA_HOME%\include\jawt_md.h" >nul
  )
)
exit /b 0
