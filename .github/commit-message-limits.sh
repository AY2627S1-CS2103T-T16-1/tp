#!/bin/sh
# Checks commit messages against the 72-character limits that
# .githooks/commit-msg enforces locally: a subject of at most 72 characters,
# and a body wrapped at 72. A body line with no spaces is an unbreakable token
# such as a URL, and is allowed as in the hook.
#
# Usage:  commit-message-limits.sh <revision range>    e.g. master..my-branch
#         commit-message-limits.sh --proposed           reads a PR description
#
# With --proposed, the message checked is the one after "Proposed merge commit
# message": the `quoted` text on that line, or else the next ``` block.
#
# A merge commit that keeps GitHub's default message is skipped, as the course
# ignores those merges when checking compliance with the Git conventions. That
# is "Merge pull request #N from <branch>" from the merge button, whose body is
# the unwrapped PR title, and "Merge branch '<base>' into <branch>" from the
# Update branch button.
#
# Not named check-*, since run-checks.sh runs those without arguments.

check_message() {
    awk -v label="$1" '
        NR == 1 && length($0) > 72 {
            printf "%s: subject is %d chars: %s\n", label, length($0), $0
            failed = 1
        }
        NR > 1 && length($0) > 72 && / / {
            printf "%s: line %d is %d chars: %s\n", label, NR, length($0), $0
            failed = 1
        }
        END { exit failed }
    '
}

is_default_merge() {
    git rev-parse -q --verify "$1^2" > /dev/null &&
        git log -1 --format=%s "$1" |
        grep -Eq "^Merge (pull request #[0-9]+ from|branch '[^']+' into) [^ ]+$"
}

proposed_message() {
    tr -d '\r' | awk '
        !found && tolower($0) ~ /proposed merge commit message/ {
            found = 1
            if (match($0, /`[^`]+`/)) {
                print substr($0, RSTART + 1, RLENGTH - 2)
                exit
            }
            next
        }
        found && !fenced && /^```/ { fenced = 1; next }
        fenced && /^```/ { exit }
        fenced { print }
    '
}

if [ "$1" = --proposed ]; then
    message=$(proposed_message)
    if [ -z "$message" ]; then
        echo "No proposed merge commit message found; nothing to check."
        exit 0
    fi
    printf '%s\n' "$message" | check_message "proposed merge commit message"
    exit $?
fi

ret=0
for commit in $(git rev-list "$1"); do
    short=$(git rev-parse --short "$commit")
    if is_default_merge "$commit"; then
        echo "$short: GitHub's default merge message; skipped."
        continue
    fi
    if ! git log -1 --format=%B "$commit" | check_message "$short"; then
        ret=1
    fi
done

[ "$ret" -ne 0 ] && echo "Wrap or shorten these messages; see .githooks/README.md."
exit $ret
