# Bug report template

One file per bug in `<workdir>/reports/NN-short-slug.md`. The title line and
labels go into the PE issue form; everything under `---` is the issue body.

```markdown
Title: <What breaks, where, under which input — under 80 chars>
Labels: type.FunctionalityBug | type.FeatureFlaw | type.DocumentationBug ; severity.<Level>
Confidence: high | medium            <- for ranking only, not part of the issue

---

**Steps to reproduce**

1. Start from an empty folder with only the JAR. Launch with `java -jar "<jar>"`.
2. <state set-up, e.g. "Run `clear`, then add one student with …">
3. Enter: `<exact command, with < and > in backticks>`

**Expected**

<What the UG says, quoted with its section name, or what a reasonable user
expects and why.>

**Actual**

<What happens: exact message, crash, frozen window, lost data.>

**Screenshot**

<attach the GUI screenshot; for long inputs, also attach the input as a
.txt file>

**Why this severity**

<Which part of the severity definition it meets, and why it is above the next
lower level. Required for kept bugs in Part IV; write it now.>
```

## Writing rules

- Neutral, factual tone. The receiving team reads it; no "obviously",
  no blame.
- One bug per report. If two inputs fail for the same root cause, report the
  most realistic input and mention the other in one line.
- Quote the UG when claiming a mismatch, with the section heading, so the
  report survives a `response.Rejected` objection.
- For long inputs, give a way to generate them (e.g. "the name is the letter
  `a` repeated 5,000 times; a file with the command is attached").
- For data-file bugs, give the exact edit (before/after JSON snippet) and say
  why a user plausibly makes it (the UG invites hand-editing).
