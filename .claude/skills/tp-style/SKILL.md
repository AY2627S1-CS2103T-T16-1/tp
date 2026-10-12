---
name: tp-style
description: Check a CS2103T tP change (or the whole repo) for style and code-quality problems that Checkstyle and CI do not catch — se-edu Java naming and Javadoc rules, the textbook's code-quality guidelines, test naming, and se-edu Markdown/UG/DG/PlantUML conventions. Runs the mechanical checks first, then two sub-agents for Java and documentation in parallel, and reports findings with file:line, the rule and a suggested fix. Use before opening or approving a PR, when asked for a style or code-quality check, or when tidying up for a release.
---

# tP style and code-quality check

Code quality is graded in CS2103T (not in PE bug reports). This skill finds
what a tutor or reviewer would flag and Checkstyle would not.

## 0. Scope

Default: the current branch's changes against `master`, **changed lines plus
the methods and sections they sit in**. Other scopes, when asked: given paths,
or a whole-repo audit.

```sh
git fetch -q upstream master 2>/dev/null || true
base=$(git merge-base HEAD upstream/master 2>/dev/null || git merge-base HEAD master)
git diff --name-only "$base"...HEAD; git diff --name-only     # committed + uncommitted
git diff -U0 "$base" -- <files>                               # the changed hunks
```

Never report or fix style outside the scope. Reformatting untouched lines
pollutes the diff and moves RepoSense authorship to the wrong person.

## 1. Mechanical checks first

```sh
./gradlew checkstyleMain checkstyleTest -q   # source of truth for layout, imports, Javadoc presence
.github/run-checks.sh                        # trailing whitespace, EOL, final newline (staged files)
```

Report their failures verbatim; do not re-derive them in the agents.

## 2. Judgement checks in parallel

Launch both in one message, giving each the base commit, the file list and
the scope:

| Agent | Covers | Rules |
| --- | --- | --- |
| `tp-style-code` | `*.java` (main and test) | `references/java-quality.md` |
| `tp-style-docs` | `docs/**/*.md`, `docs/diagrams/*.puml`, `README.md` | `references/docs-style.md` |

Skip an agent whose file set is empty.

## 3. Report

Merge the findings, drop duplicates of Checkstyle errors, and order by impact:
correctness-adjacent first (swallowed exceptions, misleading names, dead code),
then readability (long methods, nesting, magic numbers), then cosmetics.

```text
path/File.java:42  [naming] boolean `valid` → `isValid` (se-edu: booleans read like booleans)
```

End with counts per category and the single change that would improve the
diff most. Do not edit files unless the user asks; when they do, change only
the reported lines and run `./gradlew check` afterwards.
