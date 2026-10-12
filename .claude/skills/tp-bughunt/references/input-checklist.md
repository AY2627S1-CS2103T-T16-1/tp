# Input probes, per command and per field

Apply to **every** command and **every** field the UG lists. Record each probe's
harness verdict; interesting results go to the candidates file. Prefixes below
use AB3's `n/`; substitute the team's real prefixes (some use `-n`, `--name`).

## Command shape

- Command word: wrong case (`ADD`, `Add`), leading/trailing spaces, a tab
  instead of a space, a documented alias, an undocumented abbreviation.
- Extra text after a no-argument command (`list abc`, `clear 1`, `exit now`).
- Missing every argument; only whitespace as the argument.
- Each required prefix missing in turn; each optional prefix present but empty
  (`t/`), present twice (`n/A n/B`), and in an unusual order.
- Prefix text inside a value (`a/Blk 1 n/2`, `n/Ravi s/o Kumar` where `s/` may
  be a prefix; `e/a/b@x.com`). The UG's delimiters must not make common
  real-world values impossible to enter.
- Prefix without the separating space (`n/Ann p/1` vs `n/Annp/1`).
- Preamble text where none is expected (`add junk n/Ann ...`).

## Indices and numbers

- `0`, `-1`, `+1`, `01`, `1.0`, `1e3`, ` 1 `, `１` (full-width digit), `٣`
  (Arabic-Indic digit — `Character.isDigit` accepts it, `\d` with
  UNICODE_CHARACTER_CLASS too).
- One past the list size, the list size exactly, after `find` filtered the
  list (index refers to the *displayed* list).
- `2147483647`, `2147483648`, `-2147483649`, `9223372036854775808`,
  `99999999999999999999` — the team's own numeric fields often lack AB3's
  overflow guard. Watch for `NumberFormatException` CRASH and for wrong
  messages ("must be a positive integer" shown for a positive number that is
  merely too large is a misleading message — feature flaw, usually Low).
- Arithmetic on stored numbers: sums, durations, counts, percentages — can it
  overflow, divide by zero, or show `NaN`/`Infinity`/`-0.0`/`1.0E10`?
- Money/scores with decimals: `0.1 + 0.2`, rounding, `1,000`, `$5`.

## Text values

- Empty, single char, whitespace only, leading/trailing/internal multiple
  spaces (are `John  Doe` and `John Doe` duplicates?).
- Case variants of an existing value (duplicate detection, search).
- Realistic symbols the UG may forbid without reason: `O'Brien`,
  `Jean-Luc`, `Ravi s/o Kumar`, `Anne-Marie d'Arc`, `X Æ A-12`, `Dr. Lee Jr.`,
  `李小龙`, `Nguyễn`, `José`, `Ångström`, `Zoë`, names with `/`, `@`, `,`.
- Phone: `+65 9123 4567`, `(65) 9123-4567`, `9123 4567 (HP)`, extension
  `x123`, very short `999`, very long. Rejecting harmless annotations is a
  feature flaw; accepting nonsense that later breaks something is a bug.
- Email: `a+tag@x.com`, `a.b@sub.domain.sg`, `UPPER@X.COM`, `a@localhost`,
  internationalised `é@x.com`, trailing dot, duplicate emails differing in
  case.
- Unicode traps:
  - zero-width space `​`, zero-width joiner `‍`, BOM `﻿`
  - non-breaking space ` ` and ideographic space `　` — `String.trim()`
    does **not** remove them, `strip()` does; near-duplicate entries slip through
  - combining characters: `é` vs `é` (looks identical, compares unequal)
  - right-to-left override `‮`, emoji `👍🏽` (surrogate pairs; substring
    and length logic can split them)
  - newline/tab inside a value (`#! add n/A\nB ...`) — breaks the list cell
    layout or the data file
- Turkish-I: if the app lowercases with the default locale, `TITLE` vs `title`
  behaves differently under `-Duser.language=tr`. Low severity; report only if
  the app is plausibly used in such a locale.

## Length

- At the documented limit, one over, and far over (e.g. 1,000 and 20,000
  characters). Use `#repeat N UNIT template`.
- **Regex stack overflow:** for each field validated by a regex with a repeated
  group — e.g. `(x|y)*`, `(\w+\s?)*`, `([.-]\w+)*` — feed 5,000–40,000 repeats
  of the group's unit (`a.`, `a-`, `a `). Java's regex engine recurses per
  repetition, so this throws `StackOverflowError`, which `catch (ParseException)`
  does not catch. Upstream AB3's `Email` regex does this, so every team that kept
  it inherits the bug. In the GUI the command silently does nothing or the app
  freezes; confirm what the user sees before rating it.
- **Catastrophic backtracking:** nested quantifiers like `(a+)+$`, `(\w|\d)*x`
  against a long almost-matching string. The harness reports `HANG`.
- Long values that are accepted: check the GUI (truncation, overflowing
  cells, hidden buttons, horizontal scroll that hides other columns).

## Dates and times (if the app has any)

- Invalid calendar dates: `2026-02-30`, `2026-02-29` (not a leap year),
  `2028-02-29` (leap year), `2026-13-01`, `2026-00-10`, `31/04/2026`.
  `DateTimeFormatter` with the default `ResolverStyle.SMART` silently turns
  `2026-02-30` into `2026-02-28` — that is a functionality bug.
- Ambiguous formats: `01/02/2026` (D/M or M/D?), two-digit years, `24:00`,
  `12:00am`, missing leading zeros, the year `0000` and `10000`.
- Ranges: end before start, zero-length, spanning midnight or a year end,
  overlap with an existing entry.
- Past dates: should warn, not reject, unless the domain forbids them.

## State and sequence

- Repeat the same command twice (idempotence, duplicates).
- Edit an item into a duplicate of another; edit with no changes.
- Delete, then `undo`/`redo` (if present) across `clear`, `find`, `edit`.
- Commands while the list is filtered; after `clear`; on an empty list.
- Confirmation prompts: answer with `Y`, ` yes `, `no`, an unrelated command,
  empty input.
- Commands that open windows (`help`, `view`, export dialogs): run them twice,
  minimise the window and run again (AB3's help window does not refocus on some
  OSes).

## Search and filter

- Partial word, different case, multiple keywords (AND vs OR — does the UG say
  which?), keywords with symbols, a keyword that is the empty string after
  trimming, keywords in fields the UG says are searched.
- Search for a value containing the prefix text.
