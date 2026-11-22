@rem Gradle startup script for Windows (Gradle 9.6.0)
@if "%DEBUG%"=="" @echo off
set APP_HOME=%~dp0
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"
"%JAVA_HOME%\bin\java.exe" %DEFAULT_JVM_OPTS% -classpath "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
