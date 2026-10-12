# PE rules that shape a bug hunt

Condensed from the course's PE page
(<https://nus-cs2103-ay2627-s1.github.io/website/admin/tp-pe.html>). Re-check
the page before a real PE; it is the authority, this file is a summary.

## Scope

- **In scope:** the JAR's behaviour, and the UG and DG as rendered HTML pages.
- **Out of scope:** README and other `.md` files outside the UG/DG, the rest of
  the website, code quality, data/config files *unless they affect behaviour*,
  and terminal output *unless it would alarm a user*.
- Examining source code to find product, UG or DG bugs is explicitly allowed,
  and so are automated tools and AI. Bugs must still come from your own testing.
- Bugs that need **deliberate sabotage** are not bugs. Plausible mistakes
  (including a careless hand-edit of the data file) must not crash the app or
  corrupt data.
- Features the UG marks "coming soon" or as future work are not bugs.
- Minor grammar that does not hinder the reader may be `NotInScope`; do not
  spend time on many of them. A minor typo is a `VeryLow` DocumentationBug.

## Types

| Label | Meaning |
| --- | --- |
| `type.FunctionalityBug` | Does not work as specified, or as a user would reasonably expect |
| `type.FeatureFlaw` | Works as designed, but the design makes a v1.6 feature less useful: an arbitrary restriction, a realistic value that cannot be entered, an unhelpful error, duplicate detection that misses obvious near-matches. **Not** a suggestion for a new feature. |
| `type.DocumentationBug` | UG/DG misleads: missing step, wrong instruction or example, typo, broken link, wrong UML notation |

## Severity

| Label | Meaning |
| --- | --- |
| `severity.VeryLow` | Purely cosmetic, does not affect usage |
| `severity.Low` | Unlikely to affect normal operation |
| `severity.Medium` | Occasional inconvenience to some users, who can continue |
| `severity.High` | Affects most users and causes major problems |

- **When unsure, choose the lower severity.** Severity accuracy is worth about
  15–20% more marks per bug; over-claiming costs more than under-claiming.
- Weigh **credibility**: an obvious, visible bug hurts more than a subtle one.
- For documentation, "user" means **reader**.
- Broken links: Low or Medium. UML notation errors: Low or higher. Purely
  cosmetic layout: VeryLow.
- Crashes and data loss reachable by a plausible mistake are the main route to
  High; a crash needing a 20,000-character paste is usually Low ("rare
  situations"), unless the input is something a user would plausibly paste.

## Reporting

- **One bug per issue.** A multi-bug report gets only one bug counted.
- Full description in the **issue body**; comments are ignored.
- Exactly one `type.*` and one `severity.*` label.
- Enough detail to **reproduce**: exact input, starting state, expected vs
  actual, screenshot.
- No references to other bug numbers, no sub-issues.
- Escape `<` and `>` (backticks or `\`), or GitHub strips them.
- Duplicates are not penalised; report separately if unsure.

## Budget

- PE Phase 1: Part I product testing ~60 min, Part II docs ~30 min, Part III
  overall evaluation ~15 min (TEAMMATES; missing it is penalised), Part IV
  trimming ~15 min.
- **At most 6 bugs sent; the best 5 count.** Rank by severity × confidence;
  type does not matter for the choice.
- In Part IV, justify each kept bug's severity **in the description**: why it
  meets its level, and why it is above the next lower one.
- `NotApplicable`, `CannotReproduce` and `IssueUnclear` earn nothing, so
  reproducibility and clarity matter more than volume.

## Running the JAR

- Java 25 (`java -version` on the day). JAR in an empty folder the app can
  write to. Launch with `java -jar "<name>.jar"`, never by double-clicking.
- Check the UG for extra setup steps and the team's release page for extra
  downloads.
- Linux on Wayland with `Gdk-CRITICAL`: `GDK_BACKEND=x11 java -jar ...`.
