@echo off
setlocal
set DIR=%~dp0
if not defined JAVA_HOME (
  set JAVA_CMD=java.exe
) else (
  set JAVA_CMD=%JAVA_HOME%\bin\java.exe
)
"%JAVA_CMD%" -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
