@echo off
setlocal EnableExtensions

REM ===========================================================================
REM Run the Verwandlung Online Judge web application (Windows).
REM
REM Launches the self-contained Spring Boot executable JAR (embedded Tomcat).
REM By default it serves http://localhost:8080/voj and expects MySQL to be
REM reachable. Deployment config is read from VOJ_* environment variables at
REM runtime (see web\src\main\resources\voj.properties); the preflight and the
REM launched jar honour the same ones.
REM
REM If an ActiveMQ broker is already listening it is used; otherwise the web app
REM hosts an embedded broker itself (no external ActiveMQ needed). Set
REM VOJ_JMS_BROKER_EMBEDDED explicitly to opt out of that auto-detection.
REM
REM Build it first with scripts\build-jars.bat.
REM
REM Usage:
REM   scripts\run-web.bat
REM   scripts\run-web.bat --server.port=9090           override Boot properties
REM   set "JAVA_OPTS=-Xmx512m" ^& scripts\run-web.bat
REM
REM Overridable environment variables (defaults shown):
REM   JDK_HOME=%%JAVA_HOME%% or D:\Applications\OpenJDK
REM   JAVA_OPTS=                          extra JVM options
REM   MYSQL_BIN=mysql                     mysql client used for checks/import
REM   VOJ_DB_HOST=localhost  VOJ_DB_PORT=3306  VOJ_DB_USERNAME=root
REM   VOJ_DB_PASSWORD=  VOJ_DB_NAME=voj
REM   VOJ_JMS_BROKER_URL=tcp://localhost:61616
REM   VOJ_JMS_BROKER_EMBEDDED=           true/false; auto-detected when unset
REM   SKIP_PREFLIGHT=                     set to 1 to skip the service checks
REM ===========================================================================

if not defined JDK_HOME if defined JAVA_HOME set "JDK_HOME=%JAVA_HOME%"
if not defined JDK_HOME set "JDK_HOME=D:\Applications\OpenJDK"
if not defined JAVA_OPTS set "JAVA_OPTS="
if not defined MYSQL_BIN set "MYSQL_BIN=mysql"
if not defined VOJ_DB_HOST set "VOJ_DB_HOST=localhost"
if not defined VOJ_DB_PORT set "VOJ_DB_PORT=3306"
if not defined VOJ_DB_USERNAME set "VOJ_DB_USERNAME=root"
if not defined VOJ_DB_PASSWORD set "VOJ_DB_PASSWORD="
if not defined VOJ_DB_NAME set "VOJ_DB_NAME=voj"

REM Derive the broker host/port (for the reachability probe) from VOJ_JMS_BROKER_URL.
set "JMS_URL=%VOJ_JMS_BROKER_URL%"
if not defined JMS_URL set "JMS_URL=tcp://localhost:61616"
set "JMS_HP=%JMS_URL:tcp://=%"
for /f "tokens=1,2 delims=:" %%a in ("%JMS_HP%") do ( set "JMS_HOST=%%a" & set "JMS_PORT=%%b" )
if not defined JMS_HOST set "JMS_HOST=localhost"
if not defined JMS_PORT set "JMS_PORT=61616"

pushd "%~dp0.." || exit /b 1
set "PROJECT_ROOT=%CD%"

if exist "%JDK_HOME%\bin\java.exe" (
  set "JAVA_BIN=%JDK_HOME%\bin\java.exe"
) else (
  where java >nul 2>&1 && set "JAVA_BIN=java"
)
if not defined JAVA_BIN (
  echo ERROR: no Java runtime found ^(set JDK_HOME or put java on PATH^). 1>&2
  popd & exit /b 1
)

set "JAR=%PROJECT_ROOT%\web\target\voj.web.jar"
if not exist "%JAR%" (
  echo ERROR: %JAR% not found. Build it first: scripts\build-jars.bat web 1>&2
  popd & exit /b 1
)

if not "%SKIP_PREFLIGHT%"=="1" (
  call :preflight || (popd & exit /b 1)
)

echo ==^> Java:
"%JAVA_BIN%" -version

echo ==^> Starting voj.web ^(http://localhost:8080/voj^) ...
"%JAVA_BIN%" %JAVA_OPTS% -jar "%JAR%" %*
set "RC=%ERRORLEVEL%"
popd
exit /b %RC%

REM ===========================================================================
REM Verifies the ActiveMQ broker (auto-falling back to an embedded one when none
REM is reachable) and MySQL; creates/imports the voj schema when it is empty
REM (requires the mysql client; skipped with a warning if absent).
REM ===========================================================================
:preflight
REM --- ActiveMQ: respect an explicit choice, otherwise auto-detect ----------
if defined VOJ_JMS_BROKER_EMBEDDED (
  if /I "%VOJ_JMS_BROKER_EMBEDDED%"=="true" (
    echo ==^> ActiveMQ broker is embedded in the web app; skipping external broker check.
  ) else (
    echo ==^> Checking ActiveMQ at %JMS_HOST%:%JMS_PORT% ...
    call :tcp_check "%JMS_HOST%" "%JMS_PORT%"
    if errorlevel 1 (
      echo ERROR: VOJ_JMS_BROKER_EMBEDDED=false but no broker is reachable at %JMS_HOST%:%JMS_PORT%. 1>&2
      echo        Start the broker ^(e.g. 'activemq start'^) and retry, 1>&2
      echo        or clear VOJ_JMS_BROKER_EMBEDDED to host one in the web app. 1>&2
      exit /b 1
    )
  )
) else (
  call :tcp_check "%JMS_HOST%" "%JMS_PORT%"
  if errorlevel 1 (
    set "VOJ_JMS_BROKER_EMBEDDED=true"
    echo ==^> No ActiveMQ broker at %JMS_HOST%:%JMS_PORT%; the web app will host an embedded one.
  ) else (
    set "VOJ_JMS_BROKER_EMBEDDED=false"
    echo ==^> Found an ActiveMQ broker at %JMS_HOST%:%JMS_PORT%; using it.
  )
)

echo ==^> Checking MySQL at %VOJ_DB_HOST%:%VOJ_DB_PORT% ...
call :tcp_check "%VOJ_DB_HOST%" "%VOJ_DB_PORT%"
if errorlevel 1 (
  echo ERROR: cannot reach MySQL at %VOJ_DB_HOST%:%VOJ_DB_PORT%. 1>&2
  echo        Start the MySQL server and retry. 1>&2
  exit /b 1
)

where %MYSQL_BIN% >nul 2>&1
if errorlevel 1 (
  echo WARN: mysql client ^('%MYSQL_BIN%'^) not found; skipping schema check 1>&2
  echo       and auto-import. Set MYSQL_BIN to enable them. 1>&2
  exit /b 0
)

set "MYSQL_AUTH=--protocol=TCP -h %VOJ_DB_HOST% -P %VOJ_DB_PORT% -u %VOJ_DB_USERNAME%"
if not "%VOJ_DB_PASSWORD%"=="" set "MYSQL_AUTH=%MYSQL_AUTH% -p%VOJ_DB_PASSWORD%"

%MYSQL_BIN% %MYSQL_AUTH% -e "CREATE DATABASE IF NOT EXISTS `%VOJ_DB_NAME%` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" 2>nul
if errorlevel 1 (
  echo ERROR: failed to connect to MySQL ^(check VOJ_DB_USERNAME / VOJ_DB_PASSWORD^). 1>&2
  exit /b 1
)

set "TABLE_COUNT="
for /f %%c in ('%MYSQL_BIN% %MYSQL_AUTH% -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='%VOJ_DB_NAME%'" 2^>nul') do set "TABLE_COUNT=%%c"
if not defined TABLE_COUNT set "TABLE_COUNT=0"

if "%TABLE_COUNT%"=="0" (
  echo ==^> Database '%VOJ_DB_NAME%' is empty; importing sql\schema.sql + seed.sql + demo.sql ...
  for %%f in (schema.sql seed.sql demo.sql) do (
    if not exist "%PROJECT_ROOT%\sql\%%f" (
      echo ERROR: database '%VOJ_DB_NAME%' is empty but %PROJECT_ROOT%\sql\%%f is missing. 1>&2
      exit /b 1
    )
    echo     - %%f
    %MYSQL_BIN% %MYSQL_AUTH% %VOJ_DB_NAME% < "%PROJECT_ROOT%\sql\%%f"
    if errorlevel 1 (
      echo ERROR: failed to import sql\%%f. 1>&2
      exit /b 1
    )
  )
  echo ==^> Imported sql\schema.sql + seed.sql + demo.sql into '%VOJ_DB_NAME%'.
) else (
  echo ==^> Database '%VOJ_DB_NAME%' already has %TABLE_COUNT% table^(s^); skipping import.
)
exit /b 0

REM --- TCP reachability probe: %1=host %2=port; errorlevel 0 when open --------
:tcp_check
powershell -NoProfile -ExecutionPolicy Bypass -Command "$c=New-Object Net.Sockets.TcpClient; try { $iar=$c.BeginConnect('%~1',[int]'%~2',$null,$null); if($iar.AsyncWaitHandle.WaitOne(3000) -and $c.Connected){exit 0}else{exit 1} } catch { exit 1 } finally { $c.Close() }"
exit /b %ERRORLEVEL%
