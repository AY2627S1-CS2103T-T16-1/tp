---
name: tp-feature
description: Add or change a user-facing command, option or field in the TAB app (seedu.tab) — the full set of places one feature has to touch, from parser to User Guide command summary to the Developer Guide manual-testing appendix. Use whenever an issue changes what a user can type or what TAB shows them, so no layer, test kind or document is left behind.
---

# Delivering one TAB feature end to end

A tP PR must be "standalone and complete, including the relevant functional
code, comments, tests, UG and DG changes". The parts most often forgotten are
the **UG command summary table** and the **DG manual-testing appendix**. Walk
this list and check each line off.

## 1. Understand the user before the code

Define the normal use cases, the plausible user mistakes, and the harmful
inputs. A likely mistake must not crash the app, corrupt data, or leave it
unusable. Deliberate sabotage need not be supported.

- Do not impose length, numeric or character restrictions to simplify parsing.
  Any limit needs a product-domain justification broad enough for realistic
  values. Names hold `s/o`, `a/l`, hyphens, apostrophes and any script; phone
  numbers hold spaces, `+`, `ext`.
- Prefer warning over rejection for unusual but harmless values. Block input
  only when accepting it would make the data ambiguous or risk harm.
- Match case sensitivity to the real-world concept — names and search terms are
  normally case-insensitive.
- Error messages name the specific offending field and value and give the
  actual reason. Distinguish malformed input from well-formed-but-invalid.

## 2. Code

- `src/main/java/seedu/tab/logic/parser/` — the `XYZCommandParser`. Read
  `AddCommandParser` first; it is the current idiom:
  `FlagTokenizer.tokenize(args, FLAG_…)`, then `verifyNoDuplicateFlagsFor` for
  single-valued options, then one `ParseProblems` pass so a user who mistyped
  two fields hears about both at once.
- `CliFlags` holds the `Flag` definitions. A command's **preamble** label (the
  value before the first option) belongs on the command class, not in
  `CliFlags` — see `AddCommand.FIELD_NAME`.
- `src/main/java/seedu/tab/logic/commands/` — the command and its
  `MESSAGE_USAGE`, built from `Flag.getLabel()` so the usage text cannot drift
  from the flags.
- `src/main/java/seedu/tab/model/` — new or changed fields; mind
  `isSameStudent` (identity) versus `equals`.
- `src/main/java/seedu/tab/storage/` — any `JsonAdapted*` change.

Code quality is graded: methods under ~30 lines, one level of abstraction per
method, guard clauses with a prominent happy path, named constants instead of
magic numbers, no dead code, never an empty `catch`, always a `default` in a
`switch`.

## 3. Tests

JUnit 5. Reuse the builders in `src/test/java/seedu/tab/testutil/`
(`StudentBuilder`, `EditStudentDescriptorBuilder`, `StudentUtil`,
`TypicalStudents`) rather than constructing model objects by hand.

- `logic/parser/XYZCommandParserTest` — happy path plus: an option in the value
  position, an unknown option, an option without a value, an unclosed quote, an
  escaped `\"` and `\\`, a pasted non-breaking space, empty and
  whitespace-only variants, case variants, realistic symbols, boundary
  lengths and numbers, duplicate-like values.
- `logic/commands/XYZCommandTest` and the integration test — assert the exact
  user-facing message, the model state after a failure, and the absence of
  unintended mutation.
- `storage/` — compatibility and malformed-data tests whenever a change touches
  storage. A correctly hand-edited file must still load; a malformed one is
  rejected with a clear diagnostic. Never silently discard existing data.
- `logic/parser/UserGuideExamplesTest` — runs the commands printed in the User
  Guide through the real parser. **Extend it to cover your command**, so the
  guide cannot drift from the code.

## 4. User Guide — `docs/UserGuide.md`

All four of these, or the guide contradicts itself:

- [ ] The command's **Format:** line
- [ ] The notes and tip boxes around it, including any **Known limitation** box
      your change removes or adds
- [ ] Every worked **example** under the command
- [ ] The **command summary table** at the bottom of the file
- [ ] The "Notes about the command format" box at the top, if your change
      alters how commands read in general

Mark anything not yet shipped `Coming soon` or leave it out. A mismatch between
code and UG is a bug regardless of which side is wrong.

## 5. Developer Guide — `docs/DeveloperGuide.md`

- [ ] The **Implementation** section for the design decision, saying *why*, not
      just what. Keep code snippets sparse so they do not go stale.
- [ ] The **glossary**, if you introduce or retire a term
- [ ] UML under `docs/diagrams/*.puml` if class relationships changed
- [ ] The **Appendix: Instructions for manual testing** — required for every
      user-testable feature changed from AB3. It complements the UG rather than
      repeating it, and its commands and expected results must be accurate.
- [ ] **Acknowledgements**, if anything was reused

Keep your own documentation in your own commits so grading can attribute it.

## 6. Verify

```sh
./gradlew check coverage     # what CI runs
.github/run-checks.sh        # whitespace, CRLF, final newline, FIXME
./gradlew run                # then type the commands yourself
```

Exercise the happy path, the plausible typo, the empty value and a
save/exit/relaunch round trip. Do not report the task complete until
`./gradlew check` passes; report failures honestly.
