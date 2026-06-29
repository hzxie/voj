@echo off
setlocal EnableExtensions

REM ===========================================================================
REM Run the Verwandlung Online Judge judger (Windows).
REM
REM Launches the self-contained Spring Boot executable JAR. It is a headless
REM (non-web) application that listens on ActiveMQ for submissions and judges
REM them, so it expects MySQL and ActiveMQ to be reachable. Deployment config is
REM read from VOJ_* environment variables at runtime (see
REM judger\src\main\resources\voj.properties); the preflight and the launched jar
REM honour the same ones.
REM
REM Build it first with scripts\build-jars.bat.
REM
REM Usage:
REM   scripts\run-judger.bat
REM   scripts\run-judger.bat --jms.broker.url=tcp://host:61616   override props
REM   set "JAVA_OPTS=-Xmx512m" ^& scripts\run-judger.bat
REM
REM Overridable environment variables (defaults shown):
REM   JDK_HOME=%%JAVA_HOME%% or D:\Applications\OpenJDK
REM   JAVA_OPTS=                          extra JVM options
REM   MYSQL_BIN=mysql                     mysql client used for checks/import
REM   VOJ_DB_HOST=localhost  VOJ_DB_PORT=3306  VOJ_DB_USERNAME=root
REM   VOJ_DB_PASSWORD=  VOJ_DB_NAME=voj
REM   VOJ_JMS_BROKER_URL=tcp://localhost:61616
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

set "JAR=%PROJECT_ROOT%\judger\target\voj.judger.jar"
if not exist "%JAR%" (
  echo ERROR: %JAR% not found. Build it first: scripts\build-jars.bat judger 1>&2
  popd & exit /b 1
)

if not "%SKIP_PREFLIGHT%"=="1" (
  call :preflight || (popd & exit /b 1)
)

echo ==^> Java:
"%JAVA_BIN%" -version

echo ==^> Starting voj.judger ...
REM --enable-native-access grants the JNI runner native access; without it the JDK
REM 24+ prints a restricted-method warning and a future JDK will block JNI outright.
"%JAVA_BIN%" --enable-native-access=ALL-UNNAMED %JAVA_OPTS% -jar "%JAR%" %*
set "RC=%ERRORLEVEL%"
popd
exit /b %RC%

REM ===========================================================================
REM Verifies ActiveMQ and MySQL are reachable; creates/imports the voj schema
REM when it is empty (requires the mysql client; skipped with a warning if absent).
REM The judger is a broker client and cannot host one, so a missing broker is a
REM hard error (start the web app or a standalone broker first).
REM ===========================================================================
:preflight
echo ==^> Checking ActiveMQ at %JMS_HOST%:%JMS_PORT% ...
call :tcp_check "%JMS_HOST%" "%JMS_PORT%"
if errorlevel 1 (
  echo ERROR: cannot reach ActiveMQ at %JMS_HOST%:%JMS_PORT%. 1>&2
  echo        Start the broker ^(or the web app that hosts it^) and retry. 1>&2
  exit /b 1
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
