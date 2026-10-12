#!/bin/sh
# Resolve the tool relative to this launcher, preserving the caller's directory.
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd) || exit 1
if ! command -v uv >/dev/null 2>&1; then
    printf '%s\n' 'uv is required. Install it from https://docs.astral.sh/uv/getting-started/installation/' >&2
    exit 127
fi
exec uv run "$script_dir/scripts/signing/signing.py" "$@"
