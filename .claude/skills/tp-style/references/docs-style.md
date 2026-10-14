# Documentation style rules

Sources: the se-edu Markdown standard
(<https://se-education.org/guides/conventions/markdown.html>), the repo's
`AGENTS.md` "Documentation" section, and the course's UG/DG expectations.
The docs are a MarkBind site; check the rendered result for anything layout
related (`npx markbind serve docs` or the GitHub Pages site).

**Consistency with the existing file comes first.** If the UG or DG already
follows a convention throughout, a new section that breaks it is a finding even
when the se-edu standard allows both.

## [markdown] se-edu Markdown standard

- GitHub Flavored Markdown syntax, strictly.
- Do not hard-wrap prose at a fixed width.
- A blank line before every list and every fenced code block.
- A space after the heading marker (`# Heading`), and blank lines around
  headings.
- `>` on every line of a blockquote.
- Ordered lists use `1.` for every item.
- `*` for bullets, `_` for italics.

(Where the repo's existing docs consistently use another marker, follow the
file and mention the divergence once, not per line.)

## [ug] User Guide content

- Every current command is documented in its section **and** in the command
  summary table, with identical formats, prefixes and optional markers.
- Each command section has: what it does, format, parameter constraints,
  examples that work when pasted, and the expected result.
- Formats use the UG's notation consistently (`UPPER_CASE` parameters,
  `[optional]`, `…` for repeatable), explained once near the top.
- No leftover AB3 terms ("AddressBook", "persons", `addressbook.json`) and no
  stale screenshots.
- Future features are marked "Coming soon" or left out.
- Second person, present tense, plain English; American spelling; terms used
  consistently (one word per concept: not "student" in one section and
  "contact" in the next).
- Examples avoid smart quotes and en dashes inside code (MarkBind renders
  `--` outside code as `–`; keep commands in backticks).

## [dg] Developer Guide content

- New design or implementation sections describe the code as it is now; class
  and method names match the source exactly.
- "Instructions for manual testing" covers every user-testable feature changed
  from AB3, complements rather than repeats the UG, and its expected results
  are accurate.
- Acknowledgements list reused code, ideas and libraries with source and
  extent.
- Diagrams sit next to the text that explains them and are referenced from it.

## [uml] PlantUML diagrams

- Sources in `docs/diagrams/*.puml`, using the shared `style.puml` include like
  the existing diagrams.
- Simple and purpose-focused: show only what the nearby text discusses.
- Sequence diagrams: `:ClassName` or `name:ClassName` participants,
  activation bars, dashed return arrows, lifelines destroyed with `X` only when
  the object really is.
- Class diagrams: correct multiplicities, composition vs association matching
  ownership in the code, no fields or methods that do not exist.
- The rendered PNG/SVG is regenerated and committed when the source changes.

## Do not report

Grammar nits that do not hinder the reader (at most a few typos), style in
untouched sections, or Markdown inside generated files.
