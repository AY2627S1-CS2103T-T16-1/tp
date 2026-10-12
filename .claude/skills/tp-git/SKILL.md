---
name: tp-git
description: Write a commit message, name a branch, or open/merge a PR in this CS2103T tP repo following the se-edu Git conventions. Use before every commit, branch creation, PR open, or merge in this repository — the conventions are enforced by .githooks and by graders, and getting them wrong after the fact means rewriting history, which is forbidden here.
---

# Git, branches and PRs for the tP

Authoritative sources: [se-edu Git conventions](https://se-education.org/guides/conventions/git.html),
[PR guidelines](https://se-education.org/guides/guidelines/PRs.html),
[Admin Appendix E](https://nus-cs2103-ay2627-s1.github.io/website/admin/appendixE-gitHub.html).
`.githooks/commit-msg` and `.githooks/pre-commit` enforce much of this locally.

## Hard rules — violating these destroys graded evidence

- **Never `git commit --amend` and never `git push --force`.** Branches are
  append-only once pushed. Fix a mistake with a new commit.
- **Never delete a merged branch.** The course scripts inspect branches to
  confirm work went through a PR.
- **Sync with `master` by merging, never rebasing.** A rebase rewrites commit
  timestamps and skews how grading tracks when work was done.
- **No `Co-Authored-By:` trailers for AI tools, and no tool session links.**
  RepoSense attributes individual contributions; extra authors distort it.
  Commits are authored by the human contributor alone.

## Commit messages

**Subject**
- Imperative mood: `Add README.md`, never `Added` or `Adding`
- Capitalise the first letter; no trailing period
- Aim for ≤50 characters, hard limit 72
- Optional scope prefix: `Person class: Remove static imports`

A subject over 50 characters gets an advisory `WARN` from the `commit-msg`
hook and commits anyway, so it is easy to leave at 51–72 and then be unable to
fix it: the commit is pushed, and shortening it would mean rewriting published
history. **Count the subject before committing, not after.** A `scope:` prefix
eats into the 50, so keep what follows it short.

```sh
printf '%s' "<subject>" | wc -c     # keep this at 50 or below
```

**Body**
- Blank line after the subject; wrap at 72 columns
- Explain WHAT and WHY, never HOW — the diff shows how
- Structure: describe the situation in **present tense**, say why it needs
  changing, then introduce the change with **"Let's …"**
- Never use the words "currently" or "originally"

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

**Organisation**
- One complete logical change per commit, building bottom-up on earlier ones
- Aim under 100 changed lines. A mechanical change (a rename, a move) may be
  larger, but isolate it from behavioural changes and describe it so it can be
  reproduced — name the substitution you applied.
- Every commit kept in an unsquashed PR history must pass CI. Run
  `./gradlew check` before committing, or `SKIP_CHECKSTYLE=1 git commit` to
  defer only the slow Checkstyle run.

## Branches

- Kebab case from meaningful keywords: `refactor-ui-tests`
- For an issue, prefix its number: `13-add-dillion-photo`,
  `105-take-edits-fields-as-options`
- Branch off the **team repo's** `master` (`upstream/master`); send the PR from
  your own fork (`origin`)
- Never send a feature PR from `master`

## Opening a PR

1. Confirm `upstream/master` is current and your fork's `master` matches it,
   then branch.
2. Keep the PR small, single-purpose, standalone and complete — functional
   code, comments, tests, UG and DG changes all together.
3. Title: match the issue title where practical; otherwise write it as a commit
   subject describing the whole PR.
4. Description: `Fixes #123` for a complete fix, `Fixes part of #123` for a
   partial one. Include before/after screenshots for UI changes. Propose a
   merge commit message for a non-trivial multi-commit PR. Note any PR this one
   branches off.
5. Assign the issue's milestone. Open as a **draft**, inspect the final diff
   for unintended changes, then mark ready once all checks pass.

```sh
git push -u origin <branch>
gh pr create -R AY2627S1-CS2103T-T16-1/tp --draft \
  --base master --head dillionlim:<branch> \
  --title "<issue title>" --body-file <path> --milestone v1.3
```

## Merging a PR

- Never merge while CI is failing.
- Use a **merge commit** — no squash, no rebase.
- Merge subject, used consistently: `Issue title #IssueNumber (#PrNumber)`
- Afterwards: close the issue, pull `upstream/master` into local `master`, push
  to `origin/master`, then merge `master` into every unmerged branch that needs
  it. Keep all merged branches.

## Scope

These conventions govern commit messages and branch names only. Issue titles
and descriptions are not governed by them — write those to read clearly.

## Reuse

Per the [reuse policy](https://nus-cs2103-ay2627-s1.github.io/website/admin/appendixB-policies.html#policy-on-reuse),
cite reused or adapted code in a nearby comment and record it in the
Developer Guide's Acknowledgements section with the source and extent of reuse.
Credit reused media where it first appears.
