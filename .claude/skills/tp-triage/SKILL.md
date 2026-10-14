---
name: tp-triage
description: Label, size or triage an issue in this CS2103T tP tracker — type.*, priority.*, severity.* and milestone, using the course's deliberately narrow readings of severity. Use when creating an issue, assigning a severity to a bug (including PE bugs received or disputed), or deciding what goes in a milestone.
---

# Issue tracker conventions

Every issue carries a `type.*` label. Bugs also carry a `severity.*` label.
Anything scheduled carries a `priority.*` label and a milestone (`v1.1`–`v1.6`).

## Type

| Label | Meaning |
| --- | --- |
| `type.Epic` | A big feature that breaks into smaller stories, e.g. search |
| `type.Story` | A user story |
| `type.Enhancement` | An enhancement to an existing story |
| `type.Task` | Something that is not a story, bug or epic, e.g. moving test code |
| `type.Bug` | A bug |

## Priority

| Label | Meaning |
| --- | --- |
| `priority.High` | Must do |
| `priority.Medium` | Nice to have |
| `priority.Low` | Unlikely to do |

## Severity — where teams drift

| Label | Meaning |
| --- | --- |
| `severity.VeryLow` | Purely **cosmetic only**: typos, spacing, layout, colour, font. Nothing that affects usage. |
| `severity.Low` | Unlikely to affect normal operation. Rare situations, minor inconvenience. |
| `severity.Medium` | Occasional inconvenience to some users, who can keep using the product. |
| `severity.High` | Affects most users and causes major problems — **almost unusable for most users**. |

`severity.High` and `severity.VeryLow` are both much narrower than they look.
**Most real bugs land in Low or Medium.**

Two rules of thumb from the course:

- Weigh damage to the product's **credibility**, not just to the user. An
  obvious, visible bug hurts credibility more than a subtle one, even when the
  functional impact is similar.
- For documentation bugs, read "user" as **reader**. Judge a DG bug by its
  impact on a developer reading the DG, not on an end user of the app.

## Writing the issue

- Descriptive and **standalone** — readable without the conversation that
  produced it. Small enough for one person to finish in a few hours.
- Assign the responsible member, the labels and the target `v1.x` milestone.
- Note the deadline in the description as a `due:` line, e.g.
  `due: 14 October 2026, 23:59 SGT`. State dependencies explicitly
  ("Depends on #105").
- Prefer tasks that move the whole product between **working states**. Avoid
  depth-first tasks such as adding an entire layer with no usable behaviour.
- Issue titles and descriptions are **not** governed by the se-edu commit
  conventions — write them to read clearly.

```sh
gh issue create -R AY2627S1-CS2103T-T16-1/tp \
  --title "<clear standalone title>" --body-file <path> \
  --label type.Enhancement --label priority.High \
  --milestone v1.3 --assignee <user>
```

## Judging a reported bug

Distinguish the three the course separates:

- **Functionality bug** — behaves other than documented or reasonably expected
- **Feature flaw** — behaves as documented, but the design fails a reasonable
  user: an arbitrary restriction, a command that cannot express a realistic
  value, an unhelpful error message, duplicate detection that misses obvious
  near-matches
- **Documentation bug** — the UG or DG misleads its reader

A mismatch between implementation, tests and the User Guide is a bug regardless
of which side is wrong.
