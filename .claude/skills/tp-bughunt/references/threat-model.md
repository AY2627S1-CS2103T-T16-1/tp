# Threat model for a tP sweep

What a finding must show to count. `pe-rules.md` holds the course's types and
severities; this file decides **whether** something is a finding at all, and
every bucket agent and triager applies it the same way.

A tP is a single-user desktop app with a local JSON file, no network and no
other accounts. So the "attacker" here is not a hacker. It is the target user
on a bad day, plus the machine they run on. A finding is a way that this
person, acting plausibly, gets a crash, lost or corrupted data, a wrong result,
or is blocked from doing what the UG promises.

## Assets: what must not be harmed

| Asset | Harm that counts |
| --- | --- |
| The data file (`data/*.json`) | Records lost, silently altered, or the file overwritten or emptied on launch, on a command or on exit |
| Correctness of what is shown | Wrong record edited or deleted, wrong search results, wrong counts or summaries, stale display after a command |
| Availability of the app | Crash, freeze, a command that silently does nothing, an app that will not start, a window that cannot be used |
| The user's ability to enter real data | Realistic values that are refused, or accepted but stored or shown wrongly |
| The user's understanding | Error messages that blame the wrong field or give the wrong reason; UG/DG statements the product contradicts |

## Actors: whose actions count

1. **The target user typing commands**, including the plausible mistakes:
   typos, wrong or missing prefixes, extra spaces, wrong case, repeated
   prefixes, values pasted from email or a spreadsheet (tabs, trailing
   newlines, smart quotes, non-ASCII names), indexes out of range, a command
   repeated twice, a command run against an empty or filtered list.
2. **The same user hand-editing the data file** as the UG invites: a field
   deleted, a value in the wrong type, a stray comma, a duplicate record, an
   empty file, the file saved by an editor that adds a BOM or CRLF endings.
3. **The environment**: a different working directory, a read-only or full
   folder, a missing or corrupt `preferences.json`, window moved off-screen by
   the saved preferences, small screens and display scaling, another OS's line
   endings or path separators, a non-English default locale.

**Not actors** (findings that need them are invalid): someone deliberately
crafting a malicious data file, a process with write access racing the app,
an attacker on the network, a modified JAR, or a user who pastes 100,000
characters for no reason. Long inputs count only at lengths a user could
plausibly produce, such as a long address or a paragraph of notes.

## Interaction modes: every way in

The orchestrator lists the concrete entries for the target in
`<workdir>/endpoints.md`, mapping each to the files that implement it. The
modes to cover are these:

| Mode | Entry | Typical sources |
| --- | --- | --- |
| Command line | Each command word and alias, each prefix/flag, the index and preamble | `*Parser.java`, tokenizer, `ParserUtil`, model value classes |
| Command execution | The effect on the model, the list shown and the file saved | `*Command.java`, `ModelManager`, `UniqueStudentList` |
| Data file load | Launch with an existing, edited, malformed or missing file | `Json*` storage classes, `MainApp.initModelManager` |
| Data file save | Every mutating command, and exit | `StorageManager`, `JsonUtil`, `FileUtil` |
| Preferences | Window size and position, file paths | `UserPrefs`, `GuiSettings`, `JsonUserPrefsStorage` |
| GUI | Command box keys (history, Enter on empty), result display, list cards, help window, resizing, long values | `ui/*`, `.fxml`, `.css` |
| Launch | Working directory, Java version, display, arguments | `Main`, `MainApp` |

## Valid finding: all of these must hold

1. **Reachable** through one of the modes above, by an actor above.
2. **User-visible effect** on an asset above. "The code is fragile" or "this
   would break if someone changed X" is not an effect.
3. **Reproduced**: `poc.py` exits 0, or exits 3 and a triager confirms the
   GUI effect. A code reading alone is a hypothesis.
4. **Not documented as intended.** If the UG states the behaviour, it is not a
   functionality bug. It may still be a feature flaw when the design is poor
   for the target user (see `pe-rules.md`).
5. **Not a duplicate** of an existing report's root cause. Same cause, other
   input: add the input to that report instead.

## Invalid: do not write a report

- Code quality, naming, missing tests, dead code, logging, Javadoc.
- Dependency CVEs, Java/JavaFX internals, behaviour of the OS or the JVM.
- "Security" issues with no plausible actor: path traversal through a crafted
  data file, deserialisation gadgets, denial of service through megabyte inputs.
- Suggestions for new features, or features the UG marks coming soon.
- Output only in the terminal or the log file, unless it would alarm a user
  running `java -jar` (such as a stack trace on every launch).
- Anything that needs a modified JAR, a debugger, or reflection to trigger.
- Behaviour that only test code or the harness can reach.

## Borderline: report, and lower the severity

- A crash or wrong result that needs an unusual but possible input, such as a
  name with an emoji or a 300-character address: valid, usually Low.
- Data-file edits that are plausible but careless, such as a missing field or
  a trailing comma: valid when the app then loses the rest of the data or
  crashes; the UG's own warnings about editing lower the severity, not the
  validity.
- A misleading error message on a common mistake: a valid feature flaw.
