# Candidate format and probing conventions

Shared by every `tp-bughunt-*` probe agent.

## Running the harness

```sh
R=<workdir>/runs/<agent>/<probe-name>
mkdir -p "$R" && cp <workdir>/target.jar "$R/"
<skill>/scripts/sandbox-run.sh "$R" -- java -cp target.jar <skill>/harness/TpHarness.java \
    < probes.txt > "$R/out.txt" 2> "$R/log.txt"
```

- One run directory per independent scenario, so state does not leak between
  probes. Seed state with commands at the top of the probe file, or by writing
  `data/*.json` before launching.
- Write probe files in your run directory, not in `src/`.
- `CRASH` and `HANG` are always worth a candidate. `OK` where the UG says the
  input is invalid, and `REJECT` where a reasonable user's value is refused, are
  candidates too. Read the message text: a misleading or vague message is a
  feature flaw.
- After modifying commands, inspect `"$R"/data/*.json` for what was persisted.
- A `HANG` exits the harness; put suspected hangs in their own probe file.

## Candidate entry

Append to `<workdir>/candidates/<agent>.md`, one entry per distinct suspected
bug:

```markdown
## <agent>-NN: <one-line title, as it would appear in the report>

- **Area:** <command / field / file / UG section>
- **Reproduce:** <run directory>; exact probe lines (or the data-file edit), in order
- **Observed:** <harness verdict line(s), trimmed; data-file effect>
- **Expected:** <UG quote with section heading, or the reasonable expectation and why>
- **Proposed:** type.<…>, severity.<…>
- **Confidence:** high | medium | low — <why>
- **Realistic trigger?** <would a normal user plausibly do this? which user?>
```

Group inputs that fail for the same reason into one entry and list the extra
triggers under **Reproduce**. Do not write candidates that are suggestions,
sabotage, or out of scope per `pe-rules.md`.

## Returning

Your final message to the orchestrator: the number of candidates, the three
most promising by severity × confidence (one line each), and what you did not
get to. The candidates file holds the detail.
