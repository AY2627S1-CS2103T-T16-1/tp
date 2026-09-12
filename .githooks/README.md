# Git hooks

Optional local hooks that check a commit against the same conventions CI and
code review apply, so that problems surface before a push rather than after.

## Enabling

Hooks are not active until you opt in, once per clone:

```sh
git config core.hooksPath .githooks
```

To stop using them:

```sh
git config --unset core.hooksPath
```

## What runs

### `pre-commit`

1. `.github/run-checks.sh` — the repo-wide text checks CI also runs: trailing
   whitespace, CRLF line endings, and a missing newline at end of file. These
   read the staged index, so they see exactly what is being committed.
2. Checkstyle (`checkstyleMain`, `checkstyleTest`), but only when Java files
   are staged.

Checkstyle inspects the working tree rather than the index, so the hook warns
when a staged Java file also has unstaged edits — the result may not match the
commit.

### `commit-msg`

Validates the message against the
[se-edu Git conventions](https://se-education.org/guides/conventions/git.html):

| Rule | Severity |
| --- | --- |
| Subject is non-empty | error |
| Subject is at most 72 characters | error |
| Subject is at most 50 characters | warning |
| Subject starts with a capital letter | error |
| Subject has no trailing period | error |
| Subject is in the imperative mood | error |
| Blank line between subject and body | error |
| Body wrapped at 72 characters | error |
| Body avoids "currently" / "originally" | warning |

A lowercase scope prefix is accepted, so `bug fix: Add space after name`
passes. Merge, revert and autosquash (`fixup!`, `squash!`, `amend!`) messages
are skipped. An over-long line containing no spaces is treated as an
unbreakable token such as a URL and only warned about.

## Escape hatches

```sh
SKIP_CHECKSTYLE=1 git commit    # skip the slow Checkstyle run, leave it to CI
git commit --no-verify          # skip the hooks entirely
```

Use `--no-verify` sparingly: CI applies the same checks, so a commit that
needs it will usually fail the build instead.
