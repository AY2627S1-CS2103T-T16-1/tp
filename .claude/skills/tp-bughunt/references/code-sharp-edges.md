# Source-code sharp edges in AB3-derived Java tPs

Reading the code is allowed in PE. Use it only to **generate hypotheses** with a
concrete triggering input; a finding is a bug only once the harness or the GUI
reproduces it. Code quality itself is out of scope.

Each grep below runs from the target repo root.

## Exceptions that escape the GUI's catch

AB3's `MainWindow.executeCommand` catches only `CommandException` and
`ParseException`. Anything else — `NumberFormatException`,
`IndexOutOfBoundsException`, `NullPointerException`, `DateTimeException`,
`IllegalArgumentException`, `StackOverflowError` — escapes to the JavaFX thread:
the command silently does nothing, or the UI is left half-updated.

```sh
grep -rn 'Integer.parseInt\|Long.parseLong\|Double.parseDouble\|new BigDecimal\|valueOf(' src/main --include=*.java
grep -rn 'LocalDate.parse\|LocalDateTime.parse\|LocalTime.parse\|DateTimeFormatter' src/main --include=*.java
grep -rn '\.get(\|\.substring(\|\.charAt(\|split(' src/main/java/*/*/logic --include=*.java
grep -rn 'catch (' src/main/java --include=*.java | grep -v 'ParseException\|CommandException'
```

For each parse call, find whether a guard (regex, try/catch converting to
`ParseException`) runs first. For each `split`, ask what happens with zero or
one part.

## Regexes

```sh
grep -rn 'Pattern.compile\|matches(\|VALIDATION_REGEX\|replaceAll(\|split("' src/main --include=*.java
```

- Repeated groups `(...)*` / `(...)+` over unbounded input: stack overflow on
  long input (see the input checklist for the payload).
- Nested quantifiers or overlapping alternation: catastrophic backtracking.
- `.` without DOTALL, `^`/`$` without MULTILINE when values may contain newlines.
- `\w`, `\d`, `\s`, `\p{Alnum}` without `UNICODE_CHARACTER_CLASS` reject `é`,
  `李`, and accept or reject things the UG claims otherwise. Compare every
  regex against the UG's stated constraint: the mismatch is the bug.

## Equality and duplicates

```sh
grep -rn 'isSame\|equals(\|equalsIgnoreCase\|hashCode' src/main/java/*/*/model --include=*.java
grep -rn '\.trim()\|toLowerCase()\|toUpperCase()' src/main --include=*.java
```

- Duplicate check by exact `equals` on raw strings: case and whitespace
  variants are not caught (feature flaw).
- `equals` overridden without `hashCode`, or comparing fields the UI does not
  show: duplicates in a `Set`/`HashMap`, or "duplicate" errors the user cannot
  understand.
- `trim()` does not remove ` `/`　`; `toLowerCase()` without
  `Locale.ROOT` depends on the user's locale.
- `edit` paths that skip the duplicate check the `add` path performs.

## Storage

```sh
grep -rn 'class JsonAdapted\|@JsonProperty\|@JsonCreator\|toModelType' src/main/java/*/*/storage --include=*.java
```

- `toModelType()` that does not null-check a field the team added: a missing
  JSON field gives a `NullPointerException` instead of `IllegalValueException`.
  AB3's loader catches `DataLoadingException` only, so this either crashes on
  launch or silently starts empty and **overwrites** the user's file.
- Fields validated on input by the parser but not re-validated on load:
  hand-edited values bypass the rules and later break commands.
- Lists of child objects (`tags`, sessions, grades): a `null` element, a
  duplicate element, an empty object `{}`.
- Data written but never read back (or vice versa) after a schema change: the
  value is lost on the next launch.
- Does the app overwrite a corrupt file on exit? Compare the file before and
  after one launch-and-exit.

## Dangerous APIs (RCE, file access, external launch)

```sh
grep -rnE 'DefaultTyping|JsonTypeInfo|ObjectInputStream|Runtime\.getRuntime\(\)\.exec|ProcessBuilder|ScriptEngine|WebView|WebEngine|Desktop\.getDesktop|getHostServices|showDocument|Files\.(write|delete|copy|move)|new File\(|Paths?\.get\(' src/main build.gradle
```

Offline apps have no remote attacker; these matter only where data from
**another person** reaches them (an import/share feature, a file a classmate
sends, a CSV opened in Excel) or where a user-typed path is used:

- export/import with a user-typed path: overwrite of an arbitrary file, `..`
  traversal, path to a directory, path with no write permission.
- CSV export: values starting with `=`, `+`, `-`, `@` execute as formulas in
  Excel/Sheets.
- `showDocument`/`browse` with a stored string: opens any URL or file.
- `WebView` rendering stored content: stored HTML/JS runs.
- Jackson polymorphic typing on loaded files: the classic deserialisation RCE.

Dependency CVEs alone (e.g. an old Jackson without polymorphic typing) are not
reportable product bugs; skip them unless a concrete user-visible exploit
exists.

## UI code

```sh
grep -rn 'setWrapText\|setMaxWidth\|setMinWidth\|setPrefWidth\|setEllipsis\|ScrollPane' src/main --include=*.java
grep -rn 'wrapText\|maxWidth\|minWidth\|prefWidth\|textOverrun' src/main/resources --include=*.fxml
```

Labels without wrapping or ellipsis plus long accepted values: hidden content
and broken layout; check in the GUI.

## Help, version and messages

- `MESSAGE_USAGE` strings vs the UG's command format: a different prefix,
  order or example is a DocumentationBug or FunctionalityBug.
- The help window URL vs the real UG URL.
- `MainApp.VERSION` vs the release tag and the UG.
