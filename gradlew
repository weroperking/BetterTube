#!/bin/sh

#
# Copyright © 2015 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# gradlew
# Gradle wrapper script for Linux/macOS
#

set -e
set -u

APP_HOME=$(cd -P "${0%/*}" > /dev/null 2>&1 && printf '%s\n' "$PWD") || exit
[ -z "$APP_HOME" ] && APP_HOME="."

DEFAULT_JVM_OPTS=""
DEFAULT_JVM_OPTS="${DEFAULT_JVM_OPTS} -Xmx64m"
DEFAULT_JVM_OPTS="${DEFAULT_JVM_OPTS} -Dfile.encoding=UTF-8"
DEFAULT_JVM_OPTS="${DEFAULT_JVM_OPTS} -XX:+HeapDumpOnOutOfMemoryError"

if [ -n "${JAVA_HOME:-}" ]; then
    JAVA_EXE="${JAVA_HOME}/bin/java"
else
    JAVA_EXE=$(command -v java 2>/dev/null)
fi

if [ -z "${JAVA_EXE}" ]; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
    echo ""
    echo "Please set the JAVA_HOME variable in your environment to match the"
    echo "location of your Java installation."
    exit 1
fi

if [ -n "${JAVA_HOME:-}" ] && [ ! -d "${JAVA_HOME}" ]; then
    echo "ERROR: JAVA_HOME is set to an invalid directory: ${JAVA_HOME}"
    echo ""
    echo "Please set the JAVA_HOME variable in your environment to match the"
    echo "location of your Java installation."
    exit 1
fi

WRAPPER_JAR="${APP_HOME}/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_PROPS="${APP_HOME}/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "${WRAPPER_JAR}" ]; then
    echo "ERROR: Gradle wrapper jar not found at ${WRAPPER_JAR}"
    echo "Please run 'gradle wrapper' to generate the wrapper."
    exit 1
fi

exec "${JAVA_EXE}" ${DEFAULT_JVM_OPTS} ${JAVA_OPTS} -classpath "${WRAPPER_JAR}" org.gradle.wrapper.GradleWrapperMain "$@"
