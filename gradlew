#!/bin/sh
# Gradle start up script for POSIX generated for Gradle 9.6.0
APP_HOME=$( cd "${APP_HOME:-./}" && pwd -P ) || exit
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
exec "${JAVA_HOME:+$JAVA_HOME/bin/}java" $DEFAULT_JVM_OPTS -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
