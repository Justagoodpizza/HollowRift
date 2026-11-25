#!/bin/sh
#
# Maven Wrapper Startup Script
#

# Read configuration from .mvn/wrapper/maven-wrapper.properties
MAVEN_WRAPPER_PROPERTIES="$(dirname "$0")/.mvn/wrapper/maven-wrapper.properties"

WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    WRAPPER_URL=$(grep '^wrapperUrl=' "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)
fi

WRAPPER_JAR="$HOME/.m2/wrapper/maven-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Maven Wrapper..."
    mkdir -p "$(dirname "$WRAPPER_JAR")"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
    else
        echo "Error: Neither curl nor wget found. Please install one of them." >&2
        exit 1
    fi
    
    if [ ! -s "$WRAPPER_JAR" ]; then
        echo "Error: Failed to download Maven Wrapper JAR" >&2
        rm -f "$WRAPPER_JAR"
        exit 1
    fi
fi

# Run from project directory with maven.multiModuleProjectDirectory set
PROJECT_DIR="$(dirname "$0")"
cd "$PROJECT_DIR"
exec java -Dmaven.multiModuleProjectDirectory="$PROJECT_DIR" -cp "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"