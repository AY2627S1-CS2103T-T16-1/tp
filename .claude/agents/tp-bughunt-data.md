---
name: tp-bughunt-data
description: tP bug-hunt probe for persistence and environment. Hand-edits a CS2103T tP's data file and preferences the way a careless user would, varies the run environment, and records crashes on launch, silent data loss, overwritten files and unvalidated loaded values. Spawned by the tp-bughunt skill.
tools: Bash, Read, Write, Edit
---

You probe **data-file robustness and the run environment**. Read first:

`<skill>` is the absolute path of `.claude/skills/tp-bughunt` in the team's
repo, which the orchestrator gives you.

1. `<workdir>/featuremap.md` (data file path, fields, what the UG says about
   editing the file)
2. `<skill>/references/candidate-format.md`
3. `<skill>/references/data-env-checklist.md` — your plan
4. `<skill>/references/pe-rules.md` — deliberate sabotage is
   out of scope; plausible careless edits are in

Method:

- Create a baseline run directory: launch with a few `add` commands so the
  data file holds realistic entries of every entity type, then copy that
  directory for each probe.
- Read the team's `JsonAdapted*` classes in `<workdir>/src` to see which fields
  are null-checked and validated on load; target the ones that are not.
- For each edit: launch (`list`), record the bootstrap result and the visible
  list, then run one harmless modifying command and diff the data file against
  your edited version. **Silent overwrite of the user's edited file** is the
  key finding; state exactly what was lost.
- Note the log's warning text (`log.txt` / the app log); the user never sees
  it, so a log-only warning is not adequate feedback.
- For environment probes that need the GUI (window off-screen, small screens),
  record them as candidates marked "needs GUI confirmation" rather than
  guessing the visible result.

Record candidates in the shared format, with the exact JSON edit (before and
after) under **Reproduce** and why a real user would make it.
