# Data file and environment probes

The UG of every AB3 derivative invites users to hand-edit the data file, so a
**plausible careless edit** must not crash the app or silently destroy data.
Deliberate sabotage (random binary, 1 GB file) is not a bug.

Work on a fresh copy of the run directory for each probe. For each, record:
launch outcome (window/crash/hang), what the GUI shows, the log's warning,
and the data file **after** a launch → `exit` cycle (was it overwritten?).

With the harness, one launch = `init()` = load; send `list` then nothing else,
then inspect `data/*.json`. Saving happens on each modifying command, so to test
"does it overwrite on save" send one harmless modifying command too.

## Plausible edits (in scope)

1. Change one value to something the parser rejects (phone `abc`, email with no
   `@`, a date `2026-02-30`). Expect: a clear diagnostic and the original file
   left intact, or the bad entry skipped with a warning. Bug: silent empty list
   followed by the file being overwritten on the next command (data loss —
   usually Medium; High only if most users would plausibly hit it).
2. Delete a field from one entry (remove the `"email": "..."` line).
3. Set a field to `null`, `""`, `" "`.
4. Add a trailing comma, or delete the last closing `}` (common JSON slip).
5. Duplicate an entry by copy-paste, then change one field; also leave it an
   exact duplicate. Does load accept duplicates that `add` would refuse?
6. Change case or spacing of a duplicate (`john doe` vs `John Doe`).
7. A value longer than any the parser allows, or with a newline `\n` in it.
8. Unicode values written directly in the file (`"name": "José"`), and the file
   saved as UTF-8 **with BOM** (what Notepad on Windows writes).
9. Reorder entries; remove all entries (`"persons": []`); delete the file;
   empty file (0 bytes).
10. A field from an older or newer version of the app (unknown property) — does
    load fail on unknown properties?
11. Values for fields the team added (tags, sessions, grades, dates): out of
    range numbers, `-1`, `1e309`, unknown enum values, a nested list with a
    `null` element.

Hand-edit with the harness off; then run:

```sh
printf 'list\n' | sandbox-run.sh <rundir> -- java -cp target.jar <skill>/harness/TpHarness.java
```

A `BOOTSTRAP-FAILED` with a `NullPointerException` or `IllegalArgumentException`
from `toModelType` is a crash-on-launch candidate. Confirm in the GUI.

## preferences.json and config

- Delete it, empty it, set window size to `0`, negative, or larger than the
  screen; set a position off-screen (`x: -5000`). A window that opens
  off-screen and cannot be recovered without editing files is a real bug
  (Medium when a common cause exists, e.g. unplugging a second monitor).
- A `preferences.json` path to the data file that points to a missing folder or
  a read-only location.

## Environment

- Run directory read-only (`chmod a-w <rundir>`): does the app warn, or crash,
  or pretend to save?
- Run directory path containing spaces and non-ASCII (`Jérôme's tests/`).
- Two instances at once on the same data file: last write wins? (Low.)
- Launch, close via the window's X button vs the `exit` command: is data saved
  either way?
- Kill the app mid-session (`kill -9`) after a modifying command: was the
  change persisted? (Only report if the UG promises autosave.)
- Small screen: 1280×720 at 100% and 1920×1080 at 125%/150% scaling
  (`Xvfb :143 -screen 0 1280x720x24`): are all panels usable?

## Terminal output

Out of scope unless it would alarm a user. A stack trace printed during normal
operation (e.g. on every launch) can be reported as Low/VeryLow; logs written
to files are out of scope.
