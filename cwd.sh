#!/usr/bin/env bash

##############################################################
## Simple script to execute a command in another directory. ##
##############################################################

ORIGINAL_DIRECTORY=$PWD
TARGET_DIRECTORY="$1"
TARGET_COMMAND="$2"

if [[ -z "$TARGET_DIRECTORY" ]]; then
  echo "ERROR: no working directory specified."
  exit 1
fi

if [[ ! -d "$TARGET_DIRECTORY" ]]; then
  echo "ERROR: $TARGET_DIRECTORY is not a valid directory."
  exit 2
fi

if [[ -z "$TARGET_COMMAND" ]]; then
  echo "ERROR: no command was specified"
  exit 3
fi

if ! cd "$TARGET_DIRECTORY"; then
  echo "ERROR: could not open $TARGET_DIRECTORY"
  exit 5
fi

if ! eval "$TARGET_COMMAND"; then
  echo "ERROR: $TARGET_COMMAND exited with exit code $?"
  exit 6
fi

if ! cd "$ORIGINAL_DIRECTORY"; then
  echo "ERROR: could not navigate back to $ORIGINAL_DIRECTORY"
  exit 7
fi
