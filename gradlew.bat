@echo off
setlocal

set APP_HOME=%~dp0
set APP_HOME=%APP_HOME:~0,-1%

set DEFAULT_JVM_OPTS=
set DEFAULT_JVM_OPTS=%DEFAULT_JVM_OPTS% -Xmx64m
set DEFAULT_JVM_OPTS=%DEFAULT_JVM_OPTS% -Dfile.encoding=UTF-8
set DEFAULT_JVM_OPTS=%DEFAULT_JVM_OPTS% -XX:+HeapDumpOnOutOfMemoryError

set WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
    echo ERROR: Gradle wrapper jar not found at %WRAPPER_JAR%
    exit /b 1
)

if "%JAVA_HOME%" neq "" (
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA_EXE=java
)

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
