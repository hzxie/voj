@echo off
setlocal EnableExtensions

REM ===========================================================================
REM Run the Verwandlung Online Judge judger (Windows).
REM
REM Launches the self-contained Spring Boot executable JAR. It is a headless
REM (non-web) application that listens on ActiveMQ for submissions and judges
REM them, so it expects MySQL and ActiveMQ to be reachable (see
REM judger\src\main\resources\voj.properties).
REM
REM Build it first with scripts\build-jars.bat.
REM
REM Usage:
REM   scripts\run-judger.bat
REM   scripts\run-judger.bat --jms.broker.url=tcp://host:61616   override props
REM   set "JAVA_OPTS=-Xmx512m" ^& scripts\run-judger.bat
REM
REM Before launching it verifies ActiveMQ and MySQL are reachable and, when the
REM voj schema is empty, imports voj.sql automatically.
REM
REM Overridable environment variables (defaults shown):
REM   JDK_HOME=%%JAVA_HOME%% or D:\Applications\OpenJDK
REM   JAVA_OPTS=                          extra JVM options
REM   MYSQL_BIN=mysql                     mysql client used for checks/import
REM   DB_HOST=localhost  DB_PORT=3306  DB_USER=root  DB_PASS=  DB_NAME=voj
REM   JMS_HOST=localhost JMS_PORT=61616
REM   SKIP_PREFLIGHT=                     set to 1 to skip the service checks
REM ===========================================================================

if not defined JDK_HOME if defined JAVA_HOME set "JDK_HOME=%JAVA_HOME%"
if not defined JDK_HOME set "JDK_HOME=D:\Applications\OpenJDK"
if not defined JAVA_OPTS set "JAVA_OPTS="
if not defined MYSQL_BIN set "MYSQL_BIN=mysql"
if not defined DB_HOST set "DB_HOST=localhost"
if not defined DB_PORT set "DB_PORT=3306"
if not defined DB_USER set "DB_USER=root"
if not defined DB_PASS set "DB_PASS="
if not defined DB_NAME set "DB_NAME=voj"
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
"%JAVA_BIN%" %JAVA_OPTS% -jar "%JAR%" %*
set "RC=%ERRORLEVEL%"
popd
exit /b %RC%

REM ===========================================================================
REM Verifies ActiveMQ and MySQL are reachable; creates/imports the voj schema
REM when it is empty (requires the mysql client; skipped with a warning if absent).
REM ===========================================================================
:preflight
echo ==^> Checking ActiveMQ at %JMS_HOST%:%JMS_PORT% ...
call :tcp_check "%JMS_HOST%" "%JMS_PORT%"
if errorlevel 1 (
  echo ERROR: cannot reach ActiveMQ at %JMS_HOST%:%JMS_PORT%. 1>&2
  echo        Start the broker ^(e.g. 'activemq start'^) and retry. 1>&2
  exit /b 1
)

echo ==^> Checking MySQL at %DB_HOST%:%DB_PORT% ...
call :tcp_check "%DB_HOST%" "%DB_PORT%"
if errorlevel 1 (
  echo ERROR: cannot reach MySQL at %DB_HOST%:%DB_PORT%. 1>&2
  echo        Start the MySQL server and retry. 1>&2
  exit /b 1
)

where %MYSQL_BIN% >nul 2>&1
if errorlevel 1 (
  echo WARN: mysql client ^('%MYSQL_BIN%'^) not found; skipping schema check 1>&2
  echo       and auto-import. Set MYSQL_BIN to enable them. 1>&2
  exit /b 0
)

set "MYSQL_AUTH=--protocol=TCP -h %DB_HOST% -P %DB_PORT% -u %DB_USER%"
if not "%DB_PASS%"=="" set "MYSQL_AUTH=%MYSQL_AUTH% -p%DB_PASS%"

%MYSQL_BIN% %MYSQL_AUTH% -e "CREATE DATABASE IF NOT EXISTS `%DB_NAME%` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" 2>nul
if errorlevel 1 (
  echo ERROR: failed to connect to MySQL ^(check DB_USER / DB_PASS^). 1>&2
  exit /b 1
)

set "TABLE_COUNT="
for /f %%c in ('%MYSQL_BIN% %MYSQL_AUTH% -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='%DB_NAME%'" 2^>nul') do set "TABLE_COUNT=%%c"
if not defined TABLE_COUNT set "TABLE_COUNT=0"

if "%TABLE_COUNT%"=="0" (
  if not exist "%PROJECT_ROOT%\voj.sql" (
    echo ERROR: database '%DB_NAME%' is empty but %PROJECT_ROOT%\voj.sql is missing. 1>&2
    exit /b 1
  )
  echo ==^> Database '%DB_NAME%' is empty; importing voj.sql ...
  %MYSQL_BIN% %MYSQL_AUTH% %DB_NAME% < "%PROJECT_ROOT%\voj.sql"
  if errorlevel 1 (
    echo ERROR: failed to import voj.sql. 1>&2
    exit /b 1
  )
  echo ==^> Imported voj.sql into '%DB_NAME%'.
) else (
  echo ==^> Database '%DB_NAME%' already has %TABLE_COUNT% table^(s^); skipping import.
)
exit /b 0

REM --- TCP reachability probe: %1=host %2=port; errorlevel 0 when open --------
:tcp_check
powershell -NoProfile -ExecutionPolicy Bypass -Command "$c=New-Object Net.Sockets.TcpClient; try { $iar=$c.BeginConnect('%~1',[int]'%~2',$null,$null); if($iar.AsyncWaitHandle.WaitOne(3000) -and $c.Connected){exit 0}else{exit 1} } catch { exit 1 } finally { $c.Close() }"
exit /b %ERRORLEVEL%
