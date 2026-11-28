#!/bin/sh
set -eu
umask 077
state_dir=${1:?state directory required}
office_source=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
test -f "$state_dir/office.sqlite3"
exec /usr/bin/python3 "$office_source/office.py" --db "$state_dir/office.sqlite3" backup \
    "$state_dir/backups/office-$timestamp.sqlite3"
