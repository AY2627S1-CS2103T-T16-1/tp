---
name: tp-bughunt
description: Orchestrate a CS2103T tP bug hunt — another team's product in PE / PE-D, or our own tP before a release. Runs the JAR in a no-network sandbox through a headless harness, fans out sub-agents for inputs, feature flaws, data-file robustness, source-code sharp edges and UG/DG checks, verifies every candidate adversarially, confirms the keepers in the GUI, and drafts PE-format reports with type, severity and severity justification. A sweep mode instead buckets every source file (at most five per bucket), runs one agent per bucket that writes reports/<id>/report.md with a poc.py, then a second wave of agents that re-runs and triages every report. Use when asked to bughunt, test a tP, do PE/PE-D testing, find bugs before a release, or sweep/audit every file of a tP.
---

# tP bug hunt

You are the **orchestrator**. Sub-agents do the probing; you own set-up, the
shared feature map, deduplication, ranking, GUI confirmation and the final
reports. Read `references/pe-rules.md` first — it decides what counts.

Never file issues yourself. The output is report drafts the user submits
(through CATcher in PE). Untrusted JARs run **only** through
`scripts/sandbox-run.sh`.

`<skill>` below means this skill's directory, `.claude/skills/tp-bughunt` in this
repo. Sub-agents work in a workdir outside the repo, so give every one of them
the absolute path of `<skill>`.

## 0. Inputs

Establish, asking only for what you cannot find:

- **Mode** — `pe` (6-bug cap, time-boxed, Part IV justifications), `ped`
  (dry run: report everything worthwhile), or `self` (our own tP: drafts become
  issues for our tracker, labelled per the `tp-triage` skill).
- **Target** — the JAR (path or release URL) and the team's repo and UG/DG URLs.
  In `self` mode, build it: `./gradlew shadowJar`.
- **Strategy**: `probes` (default; steps 3–6, agents split by concern) or
  `sweep` (the "Sweep" section; agents split by file, every finding with a
  `poc.py`). Use `sweep` when asked to cover every file, to audit the code,
  or to produce PoCs. It costs one agent per five source files plus a triage
  wave, so it suits `self` and `ped` better than a time-boxed `pe`.
- **Workdir** — default `~/tp-bughunt/<team>-<mode>/`, outside every repo.

Layout to create:

```
<workdir>/
  target.jar          the exact JAR under test
  src/                shallow clone of the team's repo (read-only use)
  docs/               saved UG.html / DG.html (and .md sources from src/)
  featuremap.md       written by you in step 2; every agent reads it
  runs/<agent>/...    each agent's own sandbox run directories
  candidates/<agent>.md
  verified.md
  screens/  reports/
  endpoints.md        sweep only: every way in, mapped to source files
  buckets.json        sweep only: written by scripts/bucket.py
  unconfirmed/        sweep only: hypotheses that did not reproduce
```

## 1. Set up and smoke-test

```sh
java -version                                       # must be 25
gh repo clone <org/repo> <workdir>/src -- --depth 1 # or point at our repo in self mode
curl -sL <UG url> -o <workdir>/docs/UG.html; curl -sL <DG url> -o <workdir>/docs/DG.html
mkdir -p <workdir>/runs/smoke && cp <workdir>/target.jar <workdir>/runs/smoke/
printf 'list\nhelp\n' | <skill>/scripts/sandbox-run.sh <workdir>/runs/smoke -- \
    java -cp target.jar <skill>/harness/TpHarness.java 2>/dev/null
```

`READY` then `OK` lines mean the harness drives the team's Logic. If it prints
`BOOTSTRAP-FAILED`, find the `javafx.application.Application` subclass in
`src/` and pass `--main-app <fqcn>`. If it still fails, the team has diverged
from AB3's `MainApp`/`Logic` shape: tell the agents to fall back to GUI driving
(`references/gui-confirm.md`) and expect a slower hunt.

Read the harness doc comment in `<skill>/harness/TpHarness.java` once: it lists
the input syntax (`#!` escapes, `#repeat N UNIT template`) and the verdicts
(`OK`, `REJECT`, `CRASH`, `HANG`).

## 2. Write the feature map

From the UG (rendered HTML is authoritative; `.md` is easier to grep), the DG's
manual-testing appendix and the parsers in `src/`, write `featuremap.md`:

- each command: word, aliases, exact format, every prefix with required or
  optional and repeatable or not, the UG's constraints per field, every UG
  example, and the code's `MESSAGE_USAGE`
- data model: the entities, their fields, what the UG says counts as a
  duplicate, case sensitivity of names and search
- data file path and format; preferences file
- features marked coming soon (out of scope)
- anything the UG says about limits, dates, sorting, undo, confirmation

Keep it factual: no candidate bugs here. Every agent's quality depends on it.

## 3. Probe in parallel

Launch these sub-agents **in one message** so they run concurrently. Give each
the workdir, the mode, its run-directory root `runs/<agent>/`, and its output
file `candidates/<agent>.md`.

| Agent | Covers |
| --- | --- |
| `tp-bughunt-inputs` | every command × field × `references/input-checklist.md`; crashes, hangs, wrong accept/reject |
| `tp-bughunt-flaws` | user-perspective design flaws: restrictions, error messages, duplicates, search, workflow friction |
| `tp-bughunt-data` | hand-edited data file, preferences, environment (`references/data-env-checklist.md`) |
| `tp-bughunt-code` | source-code sharp edges → concrete inputs, confirmed in the harness (`references/code-sharp-edges.md`) |
| `tp-bughunt-docs` | UG/DG versus product, examples, diagrams, links (`references/docs-checklist.md`) |

With more than about six commands, split `tp-bughunt-inputs` across several
instances by command so each stays thorough. In `pe` mode, tell every agent
the time budget (about 20 minutes each) and to stop at it.

## 4. Merge, deduplicate, verify

1. Read all `candidates/*.md`. Merge duplicates that share a root cause; keep
   the most realistic trigger as the headline.
2. Drop what `references/pe-rules.md` puts out of scope: sabotage, coming-soon
   features, code quality, logs, suggestions.
3. Send the rest to `tp-bughunt-verifier` instances (about five candidates
   each, in one message). They reproduce from a fresh run directory, argue the
   receiving team's side, and return verdicts with type and severity.
4. Write `verified.md`: a table of `CONFIRMED` candidates ranked by
   severity × confidence, then credibility (obvious and visible first).

## 5. Confirm in the GUI

For each candidate you intend to keep (in `pe`, the top eight or so, to choose
six), follow `references/gui-confirm.md`: private, **verified** Xvfb, sandboxed
launch, seeded data, one command, screenshot, and read the screenshot. The
"Actual" in the report is what the screenshot shows, not what the harness said.

## 6. Draft the reports

One file per bug in `reports/`, using `references/report-template.md`,
including the "Why this severity" section. Then report to the user:

- a ranked table: title, type, severity, confidence, screenshot path
- in `pe`, the six you recommend sending and why the rest were cut
- what was not covered (commands skipped, harness fallbacks, time cuts)

In `self` mode, offer to open them as issues with the `tp-triage` labels; do so
only after the user approves.

## Sweep: file buckets

Steps 0–2 are the same. Then, instead of steps 3–6:

### S1. Threat model and endpoints

`references/threat-model.md` is the threat model: assets, the actors whose
actions count, the interaction modes, and what is and is not a valid finding.
Read it, and add anything specific to this product (for example an export
command that writes outside `data/`) to a short "Product notes" section at the
top of `endpoints.md`.

Write `<workdir>/endpoints.md`: one row per concrete entry point, grouped by
the modes in the threat model, with the source files it runs through.

- every command word and alias, with its parser and command class
- each data file and preferences file, with load and save paths through
  `MainApp`, storage and the `JsonAdapted*` classes
- each GUI entry: command box keys, list cards, help window, panels
- launch: `Main`, `MainApp`, working directory and arguments

Find them from the code, not only the UG: grep the parser's `switch`, the
`COMMAND_WORD` constants, the `@FXML` handlers and the key handlers. A command
that is in the code but not the UG is an endpoint too.

### S2. Bucket the files

```sh
python3 -I <skill>/scripts/bucket.py <workdir>/src --max 5 --out <workdir>/buckets.json
```

It prints the buckets. Check that every main source file is in one (the count
on the first line), and that no bucket is over five files. Tell the user the
bucket count before the fan-out, since it is the number of agents.

### S3. Fan out: one agent per bucket

Launch one `tp-bughunt-bucket` agent per bucket, **all in one message**. Give
each the workdir, the mode, its bucket id and the bucket's file list copied
from `buckets.json`. Each writes `reports/<bucket>-NN-<slug>/` with
`report.md` and `poc.py`, and `unconfirmed/<bucket>.md`.

If the harness bootstrap failed in step 1, say so in every prompt: the agents
must then use `poc.needs_gui` and the triage wave does all confirmation in the
GUI.

### S4. Fan in

When every bucket agent has returned:

1. `python3 -I <skill>/scripts/report-index.py <workdir>` writes
   `reports/INDEX.md`.
2. Run every PoC once (`python3 -I <workdir>/reports/<id>/poc.py`) and list
   the ones that do not exit 0 or 3. Do not fix them; triage will.
3. Read the titles and root-cause lines. Note obvious duplicate pairs for the
   triagers, but do not delete reports.

### S5. Second wave: triage

Split the report ids into groups of about five, keeping reports from the same
area (and suspected duplicates) in the same group. Launch one
`tp-bughunt-triage` agent per group, **all in one message**. Each writes
`reports/<id>/triage.md` and confirms GUI-only findings with screenshots.

Then rerun `report-index.py`; `INDEX.md` is now ranked by verdict and severity.

### S6. Report to the user

- the `INDEX.md` table of `CONFIRMED` and `WEAK` reports
- counts: buckets, reports written, confirmed, weak, rejected, duplicates
- buckets that produced nothing, and files no entry point reaches
- in `pe`, the six to send, chosen as in step 6; in `self`, offer to open the
  confirmed ones as issues with `tp-triage` labels, only after approval

## Ground rules

- A bug must be reproduced, not inferred. A code reading or an LLM opinion is a
  hypothesis until the harness or the GUI shows it.
- When unsure of severity, pick the lower one. Over-claiming loses marks.
- Prefer the realistic trigger: an apostrophe in a name beats a 40,000-character
  paste, even if both hit the same bug.
- Keep everything inside the workdir. Never run the JAR outside the sandbox and
  never touch the user's real display.
