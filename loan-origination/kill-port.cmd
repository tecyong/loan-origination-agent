@echo off
setlocal EnableDelayedExpansion

title Kill Process by Port

:: Read port argument or prompt user
set "PORT=%~1"
if "%PORT%"=="" (
    set /p "PORT=Enter port number: "
)

:: Validate non-empty
if "%PORT%"=="" (
    echo [ERROR] No port specified.
    goto END
)

echo.
echo ============================================================
echo   Searching for process listening on port %PORT%...
echo ============================================================
echo.

set "FOUND_PID="

:: Search netstat for TCP connections on the specified port
for /f "tokens=5" %%a in ('netstat -ano -p tcp ^| findstr /r /c:":%PORT% "') do (
    if not "%%a"=="0" (
        set "FOUND_PID=%%a"
    )
)

if "%FOUND_PID%"=="" (
    echo [INFO] No active TCP process found listening on port %PORT%.
    echo.
    goto END
)

:: Display process information using tasklist
echo Found process on port %PORT%:
echo ------------------------------------------------------------
tasklist /fi "PID eq %FOUND_PID%" /fo list
echo ------------------------------------------------------------
echo.

:: Prompt user for confirmation
set /p "CONFIRM=Do you want to kill PID %FOUND_PID%? [Y/N]: "
if /i "%CONFIRM%"=="Y" (
    echo.
    echo Terminating PID %FOUND_PID%...
    taskkill /PID %FOUND_PID% /F
    if !errorlevel! equ 0 (
        echo [SUCCESS] Process %FOUND_PID% has been killed.
    ) else (
        echo [ERROR] Failed to terminate PID %FOUND_PID%. You may need to run this command prompt as Administrator.
    )
) else (
    echo.
    echo [CANCELLED] Process %FOUND_PID% was not killed.
)

:END
echo.
pause
