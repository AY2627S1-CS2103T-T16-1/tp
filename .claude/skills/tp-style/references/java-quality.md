# Java style and quality rules beyond Checkstyle

Sources: the se-edu Java coding standard
(<https://se-education.org/guides/conventions/java/intermediate.html>), the
CS2103T textbook chapter on code quality, and the repo's `AGENTS.md`.
Checkstyle (`config/checkstyle/checkstyle.xml`) already enforces layout,
import order, braces, line length and Javadoc *presence*; do not duplicate it.

Tag each finding with its category in brackets.

## [naming]

- Classes and enums are nouns; methods are verbs (`getName`, `computeTotal`,
  not `name()` for an action or `processing()`).
- Booleans (fields, locals, parameters, methods) read as booleans: `is`, `has`,
  `was`, `can`, `should` — `isArchived`, `hasTag()`. A boolean setter is
  `setArchived(boolean isArchived)`.
- Collections and arrays are plural: `students`, `tags`, not `studentList`
  when a plural reads better, never a singular.
- Abbreviations are not shouted: `parseJsonFile`, `HtmlExporter`, `UgLink`.
- Long scope, long name; short scope, short name. Single letters only for
  loop indices (`i`, `j`, `k`, with `j`/`k` only when nested) and tiny lambdas.
- Related constants share a prefix: `MESSAGE_SUCCESS`, `MESSAGE_DUPLICATE_…`;
  `PREFIX_NAME`, `PREFIX_PHONE`.
- Names do not mislead: a method named `check…` that also mutates, a `get…`
  that does I/O, a `list` that is a `Set`, a name left over from AB3
  (`person`, `addressBook`) in code that handles students.
- Names are English and spelled in American English (`color`, `initialize`).
- Test methods: `featureUnderTest_testScenario_expectedBehavior`, e.g.
  `parse_emptyArg_throwsParseException`; scenario and behaviour may be omitted
  only when obvious.

## [javadoc]

- The summary of a method is third-person, present tense, starting with a verb:
  "Returns …", "Adds …" — not "Return", "This method returns", or "Function
  to …".
- The first sentence is a standalone summary. A blank line separates the
  description from the `@param` block; tag order is `@param`, `@return`,
  `@throws`.
- Either every parameter has `@param`, or none does. `@param` descriptions end
  with punctuation.
- The Javadoc describes **what and why** for the caller, not how the body works,
  and is still true after the change (a renamed parameter, a new exception, a
  changed return value).
- Overrides whose parent doc applies need none; slightly different behaviour
  uses `{@inheritDoc}` plus the difference.

## [comments]

- Comments explain what and why, not how; no comment that restates the code.
- No commented-out code; delete it (git has history).
- `TODO`, never `FIXME`. A `TODO` names what is missing.
- Comments in English, American spelling, no slang. Indented with the code
  they describe.
- Reused or adapted code is cited in a nearby comment (course reuse policy),
  and listed in the DG's Acknowledgements.

## [readability]

- Methods over about 30 lines: suggest the extraction, named for its purpose.
- Nesting deeper than about three levels, or arrowhead `if`s: suggest guard
  clauses that make the happy path prominent.
- SLAP: a method mixes high-level steps with low-level detail (string
  slicing, index arithmetic beside `model.addStudent(...)`).
- Complicated expressions: suggest named intermediate variables.
- Magic numbers and strings: a named constant, unless the literal is
  self-explanatory (`0`, `1`, `""`, an index into a pair).
- Duplicated logic across commands or parsers: point to the existing helper
  (`ParserUtil`, `StringUtil`, a `testutil` builder) to reuse.
- Booleans compared to literals (`== true`), `if (x) return true; else return
  false;`, negated names (`isNotEmpty` used as `!isNotEmpty`).

## [unsafe]

- Empty `catch`, or a `catch` that logs and continues where the user needs to
  know.
- Catching `Exception`/`Throwable` broadly in logic code.
- `switch` without a `default`; fallthrough without `// Fallthrough`.
- A variable reused for a different purpose; scope wider than needed;
  placeholder initialisation (`String name = null;` then assigned in every
  branch).
- Public mutable fields (constants and plain data holders excepted); returning
  an internal mutable collection without a defensive copy or unmodifiable view
  where callers could mutate model state.
- `System.out`/`printStackTrace` instead of `LogsCenter` loggers.
- Missing `assert` for an internal invariant the code relies on (AB3 enables
  assertions in `build.gradle`); `assert` used to validate user input instead
  (that must be an exception with a message).
- `equals` without `hashCode`, or `equals` that ignores a field the class
  treats as identity.

## [tests]

- New behaviour without tests for the happy path, likely mistakes, empty and
  whitespace input, case variants and boundaries (`AGENTS.md`, "Test the
  user-visible boundaries").
- Model objects built by hand instead of the `testutil` builders.
- A test that asserts only "no exception" where the message or resulting state
  is the point; tests that pass for the wrong reason (asserting against the
  same constant the code uses for an unexpected path).
- Test names not in `feature_scenario_behavior` form.

## Do not report

Anything Checkstyle reports; personal taste with no rule behind it (say
"preference" if you mention it at all); lines outside the scope.
