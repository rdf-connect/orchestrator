#!/usr/bin/env bash

TRACKED_FILES="$(git ls-files --cached --others --exclude-standard)"

function grep_tracked_files() {
    echo "$TRACKED_FILES" | grep "$1" | tee /dev/tty
}

echo "Formatting: Kotlin"
grep_tracked_files "\.kt$" | xargs ktfmt 2>/dev/null
grep_tracked_files "\.kts$" | xargs ktfmt 2>/dev/null
echo ""

echo "Formatting: Java"
grep_tracked_files "\.java$" | xargs -I "{}" google-java-format -r "{}"
echo ""

echo "Formatting: YAML"
grep_tracked_files "\.yml$" | xargs -I "{}" yamlfmt "{}"
grep_tracked_files "\.yaml$" | xargs -I "{}" yamlfmt "{}"
echo ""

echo "Formatting: Markdown"
grep_tracked_files "\.md$" | xargs -I "{}" mdformat "{}"
echo ""

echo "Formatting: Python"
grep_tracked_files "\.py$" | xargs black
echo ""

echo "Formatting: Protobuf"
grep_tracked_files "\.proto$" | xargs -I "{}" buf format -w "{}"
echo ""

echo "Formatting: JavaScript/TypeScript"
grep_tracked_files "\.js$" | xargs npx prettier --write >/dev/null
grep_tracked_files "\.ts$" | xargs npx prettier --write >/dev/null
