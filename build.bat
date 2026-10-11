@echo off
setlocal EnableExtensions EnableDelayedExpansion
rem ---------------------------------------------------------------------------
rem  Snoozer build script for Windows
rem
rem  Usage:  build.bat [debug ^| install ^| test ^| lint ^| all ^| clean]
rem
rem    debug    (default) Build the debug APK
rem    install  Build the debug APK and install it on a connected device (adb)
rem    test     Run the unit tests
rem    lint     Run Android lint
rem    all      Debug build, unit tests and lint
rem    clean    Delete build outputs
rem
rem  Signed release APKs are built on GitHub (Actions > Release), not here.
rem
rem  Requires JDK 17+ and the Android SDK (both come with Android Studio).
rem ---------------------------------------------------------------------------

cd /d "%~dp0"

set "TARGET=%~1"
if "%TARGET%"=="" set "TARGET=debug"

rem --- Java -----------------------------------------------------------------
if not defined JAVA_HOME (
    if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
    )
)
if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_EXE=java.exe"
)
"%JAVA_EXE%" -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Install JDK 17+ or Android Studio, or set JAVA_HOME.
    exit /b 1
)

rem --- Android SDK -----------------------------------------------------------
if not exist "local.properties" (
    if not defined ANDROID_HOME if defined ANDROID_SDK_ROOT set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
    if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
    if not defined ANDROID_HOME (
        echo [ERROR] Android SDK not found. Set ANDROID_HOME or create local.properties with sdk.dir=...
        exit /b 1
    )
    rem Gradle expects forward slashes or escaped backslashes in local.properties.
    set "SDK_DIR=!ANDROID_HOME:\=/!"
    > local.properties echo sdk.dir=!SDK_DIR!
    echo Created local.properties pointing to !ANDROID_HOME!
)

rem --- Gradle tasks ----------------------------------------------------------
set "TASKS="
if /i "%TARGET%"=="debug"   set "TASKS=assembleDebug"
if /i "%TARGET%"=="install" set "TASKS=installDebug"
if /i "%TARGET%"=="test"    set "TASKS=testDebugUnitTest"
if /i "%TARGET%"=="lint"    set "TASKS=lintDebug"
if /i "%TARGET%"=="all"     set "TASKS=assembleDebug testDebugUnitTest lintDebug"
if /i "%TARGET%"=="clean"   set "TASKS=clean"
if not defined TASKS (
    echo [ERROR] Unknown target "%TARGET%".
    echo Usage: build.bat [debug^|install^|test^|lint^|all^|clean]
    exit /b 1
)

echo.
echo === Snoozer: gradlew %TASKS% ===
echo.
call gradlew.bat %TASKS%
if errorlevel 1 (
    echo.
    echo [FAILED] Build failed. See the output above.
    exit /b 1
)

echo.
echo [OK] Done.
if /i "%TARGET%"=="debug"   echo APK: app\build\outputs\apk\debug\
if /i "%TARGET%"=="all"     echo APK: app\build\outputs\apk\debug\
if /i "%TARGET%"=="lint"    echo Report: app\build\reports\lint-results-debug.html
exit /b 0
