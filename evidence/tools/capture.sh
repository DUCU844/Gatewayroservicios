#!/usr/bin/env bash
# Runs a command and stores "$ command" + its real output in evidence/<name>.txt
# Usage: evidence/tools/capture.sh <name> "<title>" <command...>
set -uo pipefail
export AWS_PAGER=""   # set MSYS_NO_PATHCONV=1 per command when an argument starts with "/"
name="$1"; title="$2"; shift 2
out="evidence/$name.txt"
{
  echo "# $title"
  echo "# $(date '+%Y-%m-%d %H:%M:%S %z')"
  echo "\$ $*"
  bash -c "$*" 2>&1
} | sed 's/\x1b\[[0-9;]*m//g' > "$out"
cat "$out"
