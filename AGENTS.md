# AGENTS.md

Guidance for AI coding agents and human contributors working in this repository.

## Project

CS2103T AY26/27 S1 team project (T16-1), forked from
[se-edu/addressbook-level3](https://github.com/se-edu/addressbook-level3). A
JavaFX desktop application for managing a teaching assistant's students,
CLI-first with a GUI shell.

- Java 25, Gradle, JUnit 5, Jackson, JavaFX 17
- Main class: `seedu.tab.Main`
- Source: `src/main/java/seedu/tab/{commons,logic,model,storage,ui}`
- Tests: `src/test/java/seedu/tab/...`, test fixtures in `src/test/data`
- Architecture is documented in `docs/DeveloperGuide.md` — read it before
  making structural changes.

## Commands

```sh
./gradlew check coverage   # what CI runs: tests + checkstyle + JaCoCo report
./gradlew test             # tests only
./gradlew checkstyleMain checkstyleTest
./gradlew run              # launch the app
./gradlew shadowJar        # build tab.jar
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

Commit messages and branch names follow the
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

**Commit organization**
- Each commit should contain one complete logical change and build on earlier
  commits in a bottom-up order.
- Aim to keep a commit below 100 changed lines. A mechanical change may be
  larger, but should be isolated from behavioral changes and described so that
  it can be reproduced.
- Every commit retained in an unsquashed PR history should pass CI.

**Branch names**
- Kebab case, made of meaningful keywords: `refactor-ui-tests`
- For a branch that addresses an issue, prefix the issue number:
  `issueNumber-some-keywords-from-issue-title`, e.g. `13-add-dillion-photo`
- Branch off the team repo's `master`, and send the PR from your own fork

**Branch lifecycle** — required by
[Admin Appendix E](https://nus-cs2103-ay2627-s1.github.io/website/admin/appendixE-gitHub.html),
not by se-edu:
- Keep every branch after its PR merges. The course scripts inspect branches
  to confirm that changes went through one, so deleting them destroys the
  evidence being graded.
- Sync a branch with `master` by merging, never by rebasing. A rebase
  rewrites commit timestamps, which skews how the grading scripts track when
  the work was done.

**Scope** — these conventions cover commit messages and branch names only.
Issue titles and issue descriptions are not governed by them; write those so
they read clearly.

**Authorship** — commits are authored by the human contributor alone. Do not
add `Co-Authored-By:` trailers for AI tools, and do not add tool session
links. CS2103T assesses individual contributions via RepoSense authorship, so
extra co-authors distort the record being graded.

**Reuse** — comply with the course
[reuse policy](https://nus-cs2103-ay2627-s1.github.io/website/admin/appendixB-policies.html#policy-on-reuse):
- Cite reused, adapted or externally inspired code immediately in a nearby
  comment, and comply with its license.
- Record reused ideas, code, documentation and third-party libraries in the
  Developer Guide's Acknowledgements section, with the source and extent of
  reuse. Credit reused media where it first appears as well.

**Never rewrite published history** on `master` without checking with the team
first. `git push --force` on a shared branch breaks every teammate's clone.

## Issues and pull requests

Follow the
[tP GitHub workflow](https://nus-cs2103-ay2627-s1.github.io/website/admin/appendixE-gitHub.html#tp-project-schedule-tracking),
[PR guidelines](https://se-education.org/guides/guidelines/PRs.html) and
[GitHub conventions](https://se-education.org/guides/conventions/github.html).

**Issues and milestones**
- Track project work with descriptive, standalone issues small enough for one
  person to finish in a few hours. Assign the responsible member, appropriate
  labels and the target `v1.x` milestone.
- Prefer tasks that move the whole product between working states. Do not use
  depth-first tasks such as adding an entire layer without usable behavior.

**Creating pull requests**
- Keep each PR small, single-purpose, standalone and complete, including the
  relevant functional code, comments, tests, UG and DG changes.
- Use a separate branch for each PR. Use a branch in the
  author's fork. Never send a feature PR from `master`.
- Match the PR title to its issue title where practical; otherwise write it as
  a commit subject describing the whole PR.
- In the description, use `Fixes #123` for a complete fix or `Fixes part of
  #123` for a partial fix. Include before-and-after screenshots for UI changes
  and propose a merge commit message for a non-trivial multi-commit PR.
- Assign the PR to the issue's milestone. Open it as a draft, inspect the final
  diff for unintended changes, and ensure all checks pass before marking it
  ready for review.

**Merging pull requests**
- Do not merge a PR while CI is failing. Use a merge commit; do not squash or
  rebase, because rewriting commit timestamps interferes with course progress
  tracking.
- Give the merge commit a traceable subject, using this format
  consistently: `Issue title #IssueNumber (#PrNumber)`.
- After merging, close the issue, pull `upstream/master` into local `master`,
  push it to `origin/master`, then merge `master` into every unmerged branch
  that needs the update. Keep all merged PR branches.

## Reviewing pull requests

Follow the
[se-edu PR review guidelines](https://se-education.org/guides/guidelines/PRs-reviewing.html).
The way comments are phrased matters as much as the code itself.

**Reviewers**
- Add specific comments at relevant places in the code rather than broad
  summary comments for the entire PR.
- Also leave an overall review, and verify that the PR is a single, standalone,
  complete change with tests and documentation, no unrelated changes, a
  suitable title/description and passing checks.
- Before approving, confirm that all earlier review comments are resolved.
- Phrase feedback as questions (e.g. "Should this be extracted out?")
  rather than demands to encourage collaborative dialogue.
- Avoid judgmental labels like "good", "bad", or "wrong"; frame observations
  around preference or impact (e.g. "I like how you...", "Not sure I like...").
- Keep language accessible and universal using plain English; avoid slang,
  colloquialisms, or culture-specific references.

**Authors**
- Avoid defensive or confrontational responses; explain reasoning calmly
  and objectively without trying to prove superiority.
- Acknowledge and thank reviewers for their time and input.

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

## Prevent functionality bugs and feature flaws

Apply the course guidance on
[functionality bugs](https://nus-cs2103-ay2627-s1.github.io/website/schedule/week8/project.html#functionality-bugs)
and
[feature flaws](https://nus-cs2103-ay2627-s1.github.io/website/schedule/week8/project.html#feature-flaws)
when designing, implementing and reviewing user-facing behaviour. Correctness
includes usability and matching reasonable user expectations, not merely
accepting values that fit a convenient data type.

**Design from the user's point of view**
- Before coding, define the normal use cases, plausible user mistakes and
  harmful inputs. Deliberate sabotage need not be supported, but a likely
  mistake must not crash the app, corrupt data or leave it unusable.
- Prefer the simplest useful feature, but do not omit behaviour essential for
  the app to be reasonably useful. If a clearly better user experience needs
  little extra effort, implement it rather than declaring it out of scope.
- Keep implementation, tests and the User Guide aligned. A mismatch is still
  a bug regardless of whether the code or documentation is wrong.

**Accept realistic data safely**
- Do not impose arbitrary length, numeric or character restrictions merely to
  simplify parsing or validation. Any limit must be justified by the product
  domain and broad enough for realistic values.
- Accept symbols and formats that legitimate real-world values need. In
  particular, command delimiters must not make common names or other expected
  values impossible to enter; improve parsing where practical.
- Prefer warning to rejection for unusual but harmless values, such as a past
  date or a richly annotated phone number. Block input only when accepting it
  would hinder the software, make the data ambiguous or risk harm.
- Handle boundary values and arithmetic overflow where they can arise from a
  plausible mistake. Very long values must not break the layout or hide so much
  content that the feature becomes unusable.

**Make interaction forgiving and clear**
- Error messages must identify the specific offending field/value and explain
  the actual reason. Distinguish malformed input from a well-formed but invalid
  value when practical; otherwise use an accurate combined message.
- Keep commands quick to type and easy to remember. Avoid needless case
  sensitivity, long mandatory keywords and awkward special characters; where
  useful, support both a short alias and a descriptive form.
- Match case sensitivity to the real-world concept. Names and search terms are
  normally case-insensitive.
- Make search useful when users remember only part of the target. Consider
  whether OR semantics, partial matching or other forgiving behaviour better
  serves the documented use case than exact or AND-only matching.
- Keep terminal output presentable and non-alarming even though the GUI is the
  primary interface; do not print misleading errors or stack traces during
  normal operation.

**Treat duplicate detection as uncertain**
- Do not rely only on exact string equality when case or insignificant
  whitespace can differ. Normalize values consistently and test likely
  near-matches.
- Prefer warning the user about a possible duplicate and letting them decide
  when identity is ambiguous. State the detector's limitations clearly; never
  imply that all duplicates are found when they are not.

**Preserve the data contract**
- Retain at least AB3's support for human-editable data files: correctly edited
  files must load, malformed edits may be rejected with clear diagnostics, and
  the format must remain reasonably editable by hand.
- Do not silently discard or corrupt existing data after schema, parser or
  validation changes. Add compatibility and malformed-data tests whenever a
  change touches storage.

**Test the user-visible boundaries**
- For every new or changed feature, add tests for the happy path, plausible
  typing mistakes, empty and whitespace variants, case variants, realistic
  symbols, boundary lengths/numbers, duplicate-like values and malformed
  persisted data where applicable.
- Verify not only the returned result but also the exact user-facing error,
  state after failure, persistence outcome and absence of unintended mutation.
- Review restrictions and defaults explicitly: each one needs a user-centered
  rationale and must be documented if users can observe it.

## Issue tracker

Every issue carries a `type.*` label. Bugs also carry a `severity.*` label,
and anything scheduled carries a `priority.*` label and a milestone.

**Type**

| Label | Meaning |
| --- | --- |
| `type.Epic` | A big feature that breaks down into smaller stories, e.g. search |
| `type.Story` | A user story |
| `type.Enhancement` | An enhancement to an existing story |
| `type.Task` | Something to be done that is not a story, bug or epic, e.g. moving test code into a new folder |
| `type.Bug` | A bug |

**Priority**

| Label | Meaning |
| --- | --- |
| `priority.High` | Must do |
| `priority.Medium` | Nice to have |
| `priority.Low` | Unlikely to do |

**Severity** (bugs only)

| Label | Meaning |
| --- | --- |
| `severity.VeryLow` | Purely cosmetic, does not affect usage: typos, spacing, layout, colour, font. Cosmetic problems *only* |
| `severity.Low` | Unlikely to affect normal operation. Appears only in rare situations and causes minor inconvenience |
| `severity.Medium` | Causes occasional inconvenience to some users, but they can keep using the product |
| `severity.High` | Affects most users and causes major problems. Reserve this for flaws that make the product almost unusable for most users |

Severity is where teams drift, so two rules of thumb from the course:

- Weigh the damage to the product's **credibility**, not just to the user. An
  obvious, visible bug hurts credibility more than a subtle one, even when the
  functional impact is similar.
- For documentation bugs, read "user" as **reader**. Judge a DG bug by its
  impact on a developer reading the DG, not on an end user of the app.

`severity.High` and `severity.VeryLow` are both narrower than they look:
High means *almost unusable for most users*, and VeryLow means *cosmetic only*.
Most real bugs land in Low or Medium.

**Milestones** — `v1.1` through `v1.6`.

## Documentation

`docs/` is a MarkBind site. Follow the
[se-edu Markdown standard](https://se-education.org/guides/conventions/markdown.html).
User-facing behaviour changes belong in `docs/UserGuide.md`; design and
implementation changes belong in `docs/DeveloperGuide.md`. UML diagrams are
PlantUML sources under `docs/diagrams/`.

- Keep the UG, DG, product website and executable aligned with the latest
  release. The UG must cover every current feature precisely; mark future
  features `Coming soon` or leave them out.
- Keep the main UG and DG content in their existing files so grading scripts
  can attribute it. Each contributor should commit their own documentation.
- Keep UML diagrams simple, purpose-focused and integrated with the nearby
  explanation. Use code snippets sparingly so they do not become stale.
- Maintain the DG's `Instructions for Manual Testing` appendix for every
  user-testable feature changed from AB3. It should complement rather than
  repeat the UG, and its commands and expected results must be accurate.

## Testing

- JUnit 5. New functional code needs tests — coverage is reported to Codecov
  and tracked over the project.
- Reuse the builders in `src/test/java/seedu/tab/testutil` rather than
  constructing model objects by hand.
- See `docs/Testing.md` for how to run the different test kinds.
- Before a release, test the packaged JAR from an empty writable directory with
  `java -jar` under Java 25. Exercise the documented manual-test path and check
  relevant resolutions, display scaling and supported operating systems.

## Releases and iterations

- Plan each iteration with issues, assignees and a milestone deadline. Keep
  incoming PRs on that milestone and move incomplete work forward rather than
  claiming it in the release.
- A release must contain the executable JAR and release notes describing what
  changed since the previous release. Verify that source, JAR, UG, DG and
  website all describe the same version before closing the milestone.
- During the `v1.6` feature freeze, avoid functional-code changes unless they
  are necessary. Per contributor, changes under `src/main` are limited to 15%
  of their functional code or 100 lines, whichever is higher; tests,
  documentation and non-code files are outside that limit.

## Working agreements for agents

- Match the surrounding code; this is a teaching codebase where consistency
  counts more than cleverness.
- Do not reformat or "tidy" files you were not asked to change — it pollutes
  the diff and misattributes authorship in RepoSense.
- Run `./gradlew check` before reporting a task complete, and report failures
  honestly rather than describing intended behaviour.
- Prefer small, reviewable commits that each do one thing.
