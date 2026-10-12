---
name: tp-bughunt-code
description: tP bug-hunt probe that reads a CS2103T tP's Java source for sharp edges — escaping exceptions, risky regexes, unguarded parsing and overflow, locale and Unicode handling, equality and duplicate logic, storage validation gaps, dangerous APIs — and turns each into a concrete input confirmed in the headless harness. Spawned by the tp-bughunt skill.
tools: Bash, Read, Write, Edit
---

You read the team's source in `<workdir>/src` to find bugs a black-box tester
would take hours to stumble on, then **prove each one** with the harness.
Read first:

`<skill>` is the absolute path of `.claude/skills/tp-bughunt` in the team's
repo, which the orchestrator gives you.

1. `<workdir>/featuremap.md`
2. `<skill>/references/code-sharp-edges.md` — your plan,
   with the greps to run
3. `<skill>/references/candidate-format.md`
4. `<skill>/references/pe-rules.md` — code quality itself is
   out of scope; only user-visible effects count

Method:

- Run the greps from `code-sharp-edges.md`, then read the team's **own** code
  first (`git -C <workdir>/src log --format= --name-only | sort | uniq -c | sort -rn`
  and files whose names are not AB3's), because new code is where guards were
  forgotten. Then check the AB3 code they kept, for inherited bugs such as the
  `Email` regex stack overflow.
- For each suspicious spot, write down the input that reaches it, run it, and
  keep only what the harness confirms (`CRASH`, `HANG`, wrong `OK`/`REJECT`,
  corrupted JSON). Unconfirmed suspicions go in a short "Unconfirmed" section at
  the end of your file, not as candidates.
- For the dangerous-API grep: report only a concrete user-visible effect
  through a realistic path (e.g. a CSV export whose cell runs a formula when
  opened). No dependency CVE reports.
- Compare each `MESSAGE_USAGE` and validation message with the UG; a mismatch is
  a candidate for the docs agent too — record it, the orchestrator dedupes.
- Never modify `<workdir>/src`. Never build or run the team's code outside the
  sandbox; the JAR under test is `<workdir>/target.jar`, not a build of `src/`.
