# AGENTS.md

Guidance for AI coding agents and human contributors working in this repository.

## Project

CS2103T AY26/27 S1 team project (T16-1), forked from
[se-edu/addressbook-level3](https://github.com/se-edu/addressbook-level3). A
JavaFX desktop address-book application, CLI-first with a GUI shell.

- Java 25, Gradle, JUnit 5, Jackson, JavaFX 17
- Main class: `seedu.address.Main`
- Source: `src/main/java/seedu/address/{commons,logic,model,storage,ui}`
- Tests: `src/test/java/seedu/address/...`, test fixtures in `src/test/data`
- Architecture is documented in `docs/DeveloperGuide.md` — read it before
  making structural changes.

## Commands

```sh
./gradlew check coverage   # what CI runs: tests + checkstyle + JaCoCo report
./gradlew test             # tests only
./gradlew checkstyleMain checkstyleTest
./gradlew run              # launch the app
./gradlew shadowJar        # build addressbook.jar
.github/run-checks.sh      # repo-wide text hygiene checks (staged files)
```

CI (`.github/workflows/gradle.yml`) runs on every push and pull request across
ubuntu, macos and windows. Coverage is uploaded to Codecov from the Linux job
only. **Do not claim a change is done until `./gradlew check` passes.**

## Git hooks

The conventions below are also enforced locally by hooks in `.githooks/`.
Enable them once per clone:

```sh
git config core.hooksPath .githooks
```

- `pre-commit` runs `.github/run-checks.sh` against the staged index, then
  Checkstyle when Java files are staged.
- `commit-msg` validates the commit message against the se-edu Git
  conventions.

Errors abort the commit; warnings are advisory. `SKIP_CHECKSTYLE=1 git commit`
defers the slow Checkstyle run to CI, and `git commit --no-verify` bypasses
the hooks entirely — use that sparingly, since CI applies the same rules.

## Git conventions

Commit messages follow the
[se-edu Git conventions](https://se-education.org/guides/conventions/git.html).

**Subject line**
- Imperative mood: `Add README.md`, never `Added` or `Adding`
- Capitalise the first letter
- No trailing period
- Aim for <=50 characters, hard limit 72
- Optional scope prefix: `Person class: Remove static imports`

**Body**
- Blank line between subject and body; wrap at 72 characters
- Explain WHAT and WHY, never HOW — the diff already shows how
- Describe the current situation in **present tense**, say why it needs
  changing, then introduce the change with **"Let's ..."**
- Do not use the words "currently" or "originally"

```
Point CI and Codecov badges at the team fork

The CI and Codecov badges in README.md and docs/index.md point to
se-edu/addressbook-level3, the upstream repo this project is forked
from. They therefore report the build status and coverage of upstream
rather than of our own fork, which misleads anyone reading our
documentation.

Let's point both badges at AY2627S1-CS2103T-T16-1/tp, and add the
missing Codecov badge to README.md.
```

**Authorship** — commits are authored by the human contributor alone. Do not
add `Co-Authored-By:` trailers for AI tools, and do not add tool session
links. CS2103T assesses individual contributions via RepoSense authorship, so
extra co-authors distort the record being graded.

**Never rewrite published history** on `master` without checking with the team
first. `git push --force` on a shared branch breaks every teammate's clone.

## Java code style

Enforced by `config/checkstyle/checkstyle.xml`, which implements the
[se-edu Java coding standard](https://se-education.org/guides/conventions/java/intermediate.html).
Checkstyle is the source of truth — when in doubt, run it.

**Naming**
- Classes/enums: nouns in `PascalCase`
- Methods: verbs in `camelCase` (`getName()`, `computeTotalWidth()`)
- Variables: `camelCase`; constants: `ALL_CAPS_WITH_UNDERSCORES`
- Packages: all lowercase
- Booleans take an `is`/`has`/`was`/`can` prefix
- Collections take a plural name (`Collection<Point> points`)
- Abbreviations are not shouted: `exportHtmlSource()`, not `exportHTMLSource()`
- Long scope, long name; short scope, short name
- Test methods: `featureUnderTest_testScenario_expectedBehavior()`, e.g.
  `parse_emptyArg_throwsParseException`

**Layout**
- 4 spaces, never tabs; wrapped lines indent 8
- Line length: 110 soft, 120 hard
- K&R braces — `{` on the same line
- Braces always, even for a single-statement `if` or loop
- One statement and one variable declaration per line
- Break after commas, before operators; assignment operators stay at EOL
- Member order: static fields, instance fields, constructors, methods
- Import order: static, `java`/`javax`, `org`, `com` — alphabetical within
  each group. No wildcard imports, no unused imports.

**Javadoc**
- Required on every public class and on public methods over one line long
  (getters, setters and `main` are exempt; `*Test.java` is suppressed)
- Method summaries are third-person: "Returns ...", not "Return ..."
- Block tag order: `@param`, `@return`, `@throws`
- English, American spelling

**Text hygiene** (enforced by `.github/check-*.sh`, so CI fails on these)
- No trailing whitespace — error in code, warning in `.md`
- LF line endings only, never CRLF
- Every text file ends with a newline
- Use `TODO`, never `FIXME` — checkstyle rejects `FIXME`

## Code quality

From the
[CS2103T textbook chapter on code quality](https://nus-cs2103-ay2223s2.github.io/website/se-book-adapted/chapters/codeQuality.html).
These are graded, not merely suggested.

**Maximise readability**
- Avoid long methods — roughly 30 lines is the ceiling
- Avoid deep nesting and arrowhead code
- Avoid complicated expressions; compute intermediate values with names
- Avoid magic numbers — use named constants
- Make the code obvious; structure it logically
- Do not trip up the reader (unused parameters, inconsistent naming)
- Practise KISS; avoid premature optimisation
- SLAP hard — one level of abstraction per method
- Make the happy path prominent; use guard clauses

**Name well** — nouns for things, verbs for actions; standard words; names
that explain; neither too long nor too short; nothing misleading.

**Avoid unsafe shortcuts** — always use the default branch in a `switch`;
don't recycle variables; never leave an empty `catch` (checkstyle rejects it);
delete dead code rather than commenting it out; minimise variable scope;
minimise duplication.

**Comment minimally, but sufficiently** — don't repeat the obvious, write for
the reader, and explain WHAT and WHY, not HOW.

## Documentation

`docs/` is a Jekyll site. Follow the
[se-edu Markdown standard](https://se-education.org/guides/conventions/markdown.html).
User-facing behaviour changes belong in `docs/UserGuide.md`; design and
implementation changes belong in `docs/DeveloperGuide.md`. UML diagrams are
PlantUML sources under `docs/diagrams/`.

## Testing

- JUnit 5. New functional code needs tests — coverage is reported to Codecov
  and tracked over the project.
- Reuse the builders in `src/test/java/seedu/address/testutil` rather than
  constructing model objects by hand.
- See `docs/Testing.md` for how to run the different test kinds.

## Working agreements for agents

- Match the surrounding code; this is a teaching codebase where consistency
  counts more than cleverness.
- Do not reformat or "tidy" files you were not asked to change — it pollutes
  the diff and misattributes authorship in RepoSense.
- Run `./gradlew check` before reporting a task complete, and report failures
  honestly rather than describing intended behaviour.
- Prefer small, reviewable commits that each do one thing.
