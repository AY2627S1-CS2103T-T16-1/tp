---
name: tp-review
description: Review a teammate's pull request in this CS2103T tP repo, or respond to review comments on your own. Covers the se-edu review rubric — what to check for completeness, and how to phrase feedback, which is graded as much as the code. Use when asked to review a PR, leave PR comments, or reply to a reviewer.
---

# Reviewing tP pull requests

Source: [se-edu PR review guidelines](https://se-education.org/guides/guidelines/PRs-reviewing.html).
Week 9 onward the course expects reviews with thoughtful comments rather than
superficial approvals. **How a comment is phrased matters as much as what it
says.**

For finding actual defects, run the built-in `/code-review` first — it hunts
correctness bugs. This skill covers the rubric `/code-review` knows nothing
about: completeness, process, and tone.

## As a reviewer

### Where to comment

- Leave **specific comments at the relevant lines**, not one broad summary for
  the whole PR.
- Also leave an **overall review** covering the PR as a whole.

```sh
gh pr diff <N> -R AY2627S1-CS2103T-T16-1/tp
gh pr review <N> -R AY2627S1-CS2103T-T16-1/tp --comment --body-file <path>
```

### How to phrase it

- **Ask, don't demand.** "Should this be extracted out?" rather than "Extract
  this." Questions invite dialogue; demands close it.
- **Avoid judgmental labels** — no "good", "bad", "wrong". Frame observations
  around preference or impact: "I like how you …", "Not sure I like …", "This
  might surprise a user who …".
- **Plain English only.** No slang, no colloquialisms, no culture-specific
  references — the team and the graders do not share one background.

### What to check

- [ ] Single, standalone, **complete** change — functional code, comments,
      tests, UG and DG together
- [ ] No unrelated changes, no reformatting of untouched files (it pollutes the
      diff and misattributes RepoSense authorship)
- [ ] Title matches the issue; description has `Fixes #N` or `Fixes part of #N`
- [ ] Assigned to the right milestone; before/after screenshots for UI changes
- [ ] All checks passing
- [ ] Tests cover the plausible mistakes, not only the happy path
- [ ] UG **command summary table** and DG **manual-testing appendix** updated
      if user-visible behaviour changed
- [ ] Commit messages follow the se-edu conventions (see the `tp-git` skill)
- [ ] Error messages name the offending field and value
- [ ] **Every earlier review comment is resolved** before approving

### What not to do

Do not approve to be agreeable, and do not block on taste alone — say it is a
preference when it is one.

## As an author responding

- Never defensive or confrontational. Explain reasoning calmly and objectively;
  do not try to prove superiority.
- Acknowledge and thank reviewers for their time and input.
- If you disagree, state the trade-off and let the reviewer decide whether it
  still matters.
