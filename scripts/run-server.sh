#!/usr/bin/env bash
# Build the WAR, download Tomcat 10 into scripts/.tomcat (first run only),
# deploy, and run it in the foreground with env vars pointing at local Postgres.
#
# Requires: Maven (brew install maven), Java 21+ (brew install openjdk@25),
# and that ./scripts/setup-db.sh has been run.
#
# Ctrl-C to stop.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if ! command -v mvn >/dev/null 2>&1; then
    echo "mvn not found. Install with: brew install maven" >&2
    exit 1
fi

if [ -z "${JAVA_HOME:-}" ]; then
    if [ -d "/opt/homebrew/opt/openjdk@25" ]; then
        export JAVA_HOME="/opt/homebrew/opt/openjdk@25"
    elif [ -d "/opt/homebrew/opt/openjdk@21" ]; then
        export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
    fi
fi
if [ -n "${JAVA_HOME:-}" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

TOMCAT_VER="10.1.34"
TOMCAT_DIR="$SCRIPT_DIR/.tomcat"
UPLOAD_DIR="${PHOTO_UPLOAD_DIR:-$SCRIPT_DIR/.upload}"
mkdir -p "$UPLOAD_DIR"

echo "== Building WAR with Maven"
(cd "$ROOT" && mvn -q package -DskipTests)

WAR_SRC=$(ls "$ROOT"/target/CSCI201_Project-*.war | head -1)
if [ ! -f "$WAR_SRC" ]; then
    echo "WAR not found after Maven build" >&2
    exit 1
fi

if [ ! -d "$TOMCAT_DIR" ]; then
    echo "== Downloading Tomcat $TOMCAT_VER"
    TMP_TGZ="$(mktemp).tgz"
    trap 'rm -f "$TMP_TGZ"' EXIT
    for URL in \
        "https://dlcdn.apache.org/tomcat/tomcat-10/v${TOMCAT_VER}/bin/apache-tomcat-${TOMCAT_VER}.tar.gz" \
        "https://archive.apache.org/dist/tomcat/tomcat-10/v${TOMCAT_VER}/bin/apache-tomcat-${TOMCAT_VER}.tar.gz"
    do
        if curl -fsSL "$URL" -o "$TMP_TGZ"; then
            break
        fi
    done
    if [ ! -s "$TMP_TGZ" ]; then
        echo "Failed to download Tomcat" >&2
        exit 1
    fi
    mkdir -p "$TOMCAT_DIR"
    tar -xzf "$TMP_TGZ" -C "$TOMCAT_DIR" --strip-components=1
    chmod +x "$TOMCAT_DIR"/bin/*.sh
fi

echo "== Deploying WAR"
rm -rf "$TOMCAT_DIR/webapps/CSCI201_Project" "$TOMCAT_DIR/webapps/CSCI201_Project.war"
cp "$WAR_SRC" "$TOMCAT_DIR/webapps/CSCI201_Project.war"

export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/postgres}"
export DB_USER="${DB_USER:-postgres}"
export DB_PASSWORD="${DB_PASSWORD:-pg}"
export PHOTO_UPLOAD_DIR="$UPLOAD_DIR"
export CATALINA_HOME="$TOMCAT_DIR"
export CATALINA_BASE="$TOMCAT_DIR"

echo "== Starting Tomcat on http://localhost:8080/CSCI201_Project/"
echo "   DB_URL=$DB_URL"
echo "   PHOTO_UPLOAD_DIR=$PHOTO_UPLOAD_DIR"
echo "   (Ctrl-C to stop)"
exec "$TOMCAT_DIR/bin/catalina.sh" run
