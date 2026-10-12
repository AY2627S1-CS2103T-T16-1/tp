#!/usr/bin/env bash
# Runs a command inside a bubblewrap sandbox for testing another team's JAR.
#
#   sandbox-run.sh <rundir> [--display :143] -- <command...>
#
# - The whole filesystem is read-only; only <rundir> is writable.
# - $HOME is replaced by <rundir>/home, so the JAR cannot read ~/.ssh,
#   tokens or your own projects (JavaFX caches its natives there too).
# - No network. /tmp is private, except the X11 socket directory when
#   --display is given, so the GUI can reach a (verified) Xvfb.
# - Starts in <rundir>, which plays the "empty folder" the PE rules ask for.
set -euo pipefail

if [[ $# -lt 3 ]]; then
    echo "usage: $0 <rundir> [--display :N] -- <command...>" >&2
    exit 64
fi
rundir=$(realpath "$1")
shift
display=""
if [[ "$1" == "--display" ]]; then
    display="$2"
    shift 2
fi
[[ "$1" == "--" ]] && shift

skill_dir=$(cd "$(dirname "$0")/.." && pwd)
mkdir -p "$rundir/home"

args=(--ro-bind / / --dev /dev --proc /proc --tmpfs /tmp
      --tmpfs "$HOME"
      --bind "$rundir" "$rundir"
      --ro-bind "$skill_dir" "$skill_dir"
      --setenv HOME "$rundir/home"
      --unshare-net --unshare-pid --die-with-parent
      --chdir "$rundir")
if [[ -n "$display" ]]; then
    args+=(--ro-bind /tmp/.X11-unix /tmp/.X11-unix --setenv DISPLAY "$display" --setenv GDK_BACKEND x11)
else
    args+=(--unsetenv DISPLAY --unsetenv WAYLAND_DISPLAY)
fi

exec bwrap "${args[@]}" "$@"
