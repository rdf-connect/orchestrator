#!/usr/bin/env bash

TRACKED_FILES="$(git ls-files --cached --others --exclude-standard)"

function find_tracked_files() {
    echo "$TRACKED_FILES" | grep "$1$" | tee /dev/tty
}

echo "Formatting: Kotlin"
find_tracked_files ".kt" | xargs ktfmt 2>/dev/null
find_tracked_files ".kts" | xargs ktfmt 2>/dev/null
echo ""

echo "Formatting: Java"
find_tracked_files ".java" | xargs -I "{}" google-java-format -r "{}"
echo ""

echo "Formatting: YAML"
find_tracked_files ".yml" | xargs -I "{}" yamlfmt "{}"
find_tracked_files ".yaml" | xargs -I "{}" yamlfmt "{}"
echo ""

echo "Formatting: Markdown"
find_tracked_files ".md" | xargs -I "{}" mdformat "{}"
echo ""

echo "Formatting: Python"
find_tracked_files ".py" | xargs black
echo ""

echo "Formatting: Protobuf"
find_tracked_files ".proto" | xargs -I "{}" buf format -w "{}"
