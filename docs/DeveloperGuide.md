---
  layout: default.md
  title: "Developer Guide"
  pageNav: 3
---

# TAB Developer Guide

TAB (Teaching Assistant's Book) is an offline desktop application for teaching assistants to manage student details, groups, and pending follow-ups through typed commands. This guide explains its architecture and the implementation decisions a developer needs to understand before changing it. Command syntax and field constraints are documented in the [User Guide](UserGuide.md).

<page-nav-print />

## Acknowledgements

* TAB is built on [AddressBook-Level3 (AB3)](https://github.com/se-edu/addressbook-level3) by the [SE-EDU initiative](https://se-education.org). Its layered architecture, command/parser structure, JavaFX UI framework, JSON storage infrastructure, test utilities, and documentation form the starting point for TAB. This guide adapts the organization and explanations of the [AB3 Developer Guide](https://se-education.org/addressbook-level3/DeveloperGuide.html) to TAB's student model and behavior.
* [JavaFX](https://openjfx.io/) supplies the controls, FXML loader, observable collections, and graphics used by the desktop interface. [Jackson](https://github.com/FasterXML/jackson) handles JSON conversion through `JsonUtil`, and [JUnit 5](https://junit.org/junit5/) supports automated testing.
* Email local-part validation in `Email` uses the dot-atom character and dot-placement rules from [RFC 5322, section 3.2.3](https://www.rfc-editor.org/rfc/rfc5322.html#section-3.2.3). No implementation code was copied.
* `StringUtil.computeEditDistance` implements the recurrence for [optimal string alignment distance](https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance) to suggest mistyped command words. The algorithmic idea is reused in that method; the implementation was written for TAB.
* The help pager's navigation keys and progress prompt are inspired by [OpenBSD more(1)](https://man.openbsd.org/more.1). The reuse is limited to interaction conventions; the JavaFX pager was written for TAB.
* [MarkBind](https://markbind.org/) builds the documentation site, and [PlantUML](https://plantuml.com/) generates its UML diagrams from the sources in `docs/diagrams/`.

## Setting up and getting started

Use [Setting up and getting started](SettingUp.md) for IDE setup, and the [User Guide quick start](UserGuide.md#quick-start) for the Java and JavaFX runtime combinations supported by the packaged app. The source targets Java 25. `seedu.tab.Main` launches the JavaFX application, `MainApp`.

From the repository root:

```sh
./gradlew run
./gradlew check
./gradlew shadowJar
```

These commands launch TAB, run the tests and Checkstyle checks, and build `build/libs/tab.jar`, respectively. See [Testing](Testing.md) for test selection and [Documentation](Documentation.md) for the MarkBind preview workflow.

When packaged, TAB resolves `data/tab.json` and `preferences.json` relative to the JAR's directory through `AppUtil.getApplicationDirectory()`. Development runs use the working directory. Use a separate writable folder for manual testing so that test commands cannot affect real student records.

## Design

### Architecture

TAB retains AB3's four main components and the shared `Commons` utilities. The UML package diagram below shows their dependencies; an arrow points from the component that uses an API or type to the component that provides it. `Main` represents application startup and shutdown. `Commons` utilities are used across components and omitted from the arrows for focus.

<puml src="diagrams/ArchitectureDiagram.puml" alt="Dependencies between TAB's architectural components" />

| Component | Entry point | Responsibility |
| --- | --- | --- |
| UI | [`Ui`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/ui/Ui.java), implemented by `UiManager` | Displays students, accepts input, and presents results and startup errors. |
| Logic | [`Logic`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/logic/Logic.java), implemented by `LogicManager` | Parses and executes commands, manages clear confirmation, and coordinates saving. |
| Model | [`Model`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/model/Model.java), implemented by `ModelManager` | Owns the student book, user preferences, and active filtered view. |
| Storage | [`Storage`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/storage/Storage.java), implemented by `StorageManager` | Loads and saves student records and preferences through separate JSON storage classes. |
| Commons | `seedu.tab.commons` | Supplies indexing, logging, value normalization, file access, and error formatting. |

The sequence diagram shows how these components cooperate for a successful `delete 1`. Lifelines represent components; messages summarize interactions rather than every internal call. Logic parses the command, snapshots the model, and executes the deletion, then saves the changed book before returning feedback. Parser details and failure paths are expanded in the Logic and saving sections.

<puml src="diagrams/ArchitectureSequenceDiagram.puml" alt="Successful deletion flowing through UI, Logic, Model, and Storage" />

`MainApp.init()` reads preferences, constructs storage, loads the student book into `ModelManager`, and connects `LogicManager` and `UiManager`. A missing student file produces sample students. An unreadable student file produces a startup error that prevents the normal window from opening. This distinction protects existing records from being replaced by an empty or sample book.

On exit, `MainWindow` passes the window bounds to Logic, and `MainApp.stop()` saves preferences. Student changes are saved during command execution, as described in [Saving changes safely](#saving-changes-safely).

### UI component

The UI uses JavaFX controls and FXML layouts in `src/main/resources/view/`. `UiPart<T>` loads a layout and connects its controller. The partial class diagram below shows the main window's UI parts; helper controls and the common `UiPart` superclass are omitted.

<puml src="diagrams/UiClassDiagram.puml" alt="Main window and its principal UI parts" />

`MainWindow` connects the command box to `Logic.execute()`. A successful `CommandResult` carries feedback, help/exit requests, and optionally a student to reveal. `ParseException` and `CommandException` messages appear in `ResultDisplay`; the command box retains rejected input for correction.

`StudentListPanel` and `SummaryPanel` receive the same observable filtered list through Logic. A model change therefore updates both the cards and the summary. UI classes depend on model types to display student data, but student mutations go through Logic.

`HelpPage` replaces the student view inside the window. Closing it restores the previous result and unfinished command. `StatusBarFooter` displays the data file's absolute path. The keyboard and layout decisions behind these parts are described in [Supporting keyboard work](#supporting-keyboard-work).

### Logic component

`StudentBookParser` dispatches a command word to its command-specific parser. Each parser implementing `Parser<T>` validates arguments and constructs a `Command`; commands execute against the `Model` interface. `CommandResult` keeps presentation requests separate from JavaFX controls, so commands can be tested without a window.

The partial class diagram shows this structure using `delete` as the representative command. Other command/parser pairs follow the same pattern. Model and Storage dependencies appear in the architecture view above.

<puml src="diagrams/LogicClassDiagram.puml" alt="Logic API, command and parser hierarchies, and command results" />

The sequence diagram traces a successful `delete 1` through Logic. Target resolution is summarized as a lookup in the displayed list, and the shared save procedure is shown as a reference to [Saving changes safely](#saving-changes-safely). Tokenization and value validation remain inside the command parser. Descriptive messages summarize list access and snapshot creation.

<puml src="diagrams/DeleteSequenceDiagram.puml" alt="Parsing and executing delete 1 through Logic" />

Commands check their execution preconditions before changing the model. For example, a target outside the displayed list raises `CommandException` before deletion. Logic snapshots the book and filter before executing an ordinary command, then saves only if the resulting student book differs from the snapshot.

### Model component

`ModelManager` owns a `StudentBook`, `UserPrefs`, and a JavaFX `FilteredList<Student>` backed by the book's observable list. `StudentBook` delegates list operations and ID uniqueness checks to `UniqueStudentList`. Consumers receive read-only book/preferences interfaces and an observable filtered view.

The partial class diagram separates ownership of the book from references to immutable student values. Value objects may be shared by replacement records, so their relationships use associations rather than composition.

<puml src="diagrams/ModelClassDiagram.puml" alt="Student book ownership, filtered view, and immutable student values" />

Only a student's name is mandatory. Phone, email, NUS Student ID, and follow-up flag are represented with `Optional`; general tags form a set. Updating a record constructs a replacement `Student` and replaces the old entry, allowing observable-list listeners to refresh the UI.

`Student.isSameStudent()` compares two present NUS Student IDs. A missing ID cannot establish identity. `Student.equals()` compares every field, including tags and follow-up state; it is used to detect unchanged records and books. Keep these two meanings separate when adding commands or storage validation.

### Storage component

`StorageManager` delegates student data to `JsonStudentBookStorage` and preferences to `JsonUserPrefsStorage`. `JsonSerializableStudentBook`, `JsonAdaptedStudent`, and `JsonAdaptedTag` translate between JSON-friendly data and validated model values. Storage therefore depends on Model, while Model has no dependency on Storage, Logic, or UI.

The class diagram shows the API implementation, its two delegates, and the student-data adapters. Model classes and file-access helpers are omitted to focus on Storage's structure.

<puml src="diagrams/StorageClassDiagram.puml" alt="Storage API implementation, JSON delegates, and student-data adapters" />

There are two layers of validation. Custom deserializers check JSON types, such as a string for `email` and a Boolean for `flag`. Adapter conversion then applies the model's field rules and checks repeated student IDs. The entire book must load successfully before it is exposed to the normal UI.

The [data contract](#loading-and-preserving-the-data-contract) and [save procedure](#saving-changes-safely) below explain how this separation supports hand-edited files without silently discarding malformed data.

## Implementation

### Parsing commands and reporting mistakes

TAB uses a positional name or target followed by options. For example, `add Ravi s/o Kumaran -p "9123 4567" -t "Lab 3"` keeps the multi-word name in the preamble and gives each option one value. This avoids treating the slash in a legitimate name as an AB3 field prefix.

| Stage | Classes | Decision |
| --- | --- | --- |
| Identify the command | `StudentBookParser`, `CommandWords` | Strip invisible format/control characters as defined by `StringUtil`, split on Unicode whitespace, and fold only the command word with `Locale.ROOT`. |
| Split arguments | `CommandTokenizer` | Preserve quoted token boundaries and explicit empty values; support double quotes and pasted curly/full-width variants. Apostrophes are literal. Backslashes escape quotes and backslashes. |
| Assign options | `FlagTokenizer`, `CliFlag`, `CliFlags`, `FlagArgumentMap` | Map aliases to the same option, accept separate or `=`-attached values, and distinguish value-taking options from presence-only selectors. |
| Validate meaning | Command-specific parsers, `ParserUtil`, `ParseProblems` | Validate targets and fields, reject inappropriate repetition, and construct a command only after validation succeeds. |

The sequence diagram follows successful argument parsing for `add`. `FlagTokenizer` calls `CommandTokenizer` before assigning options; the parser then validates values and constructs the command. The tokenizer lifelines denote classes with static helpers. Notes summarize field validation and student construction; command execution and saving are outside this diagram.

<puml src="diagrams/CommandArgumentParsing.puml" alt="Add argument parsing, tokenizer delegation, validation, and command construction" />

The command-specific parser determines which options exist and which may repeat. For example, `add` and `edit` allow repeated `-t`, while `find` allows repeated field selectors because each adds a condition. A no-argument command such as `list` rejects extra text. Option spellings remain case-sensitive, with a lowercase suggestion when the command supports that spelling.

Structural errors are reported first: an unclosed quote or unknown option makes field boundaries uncertain. Once the structure is valid, `add` and `edit` collect independent field errors through `ParseProblems`, allowing the user to correct several invalid values together. `ValueMessages` and `Messages` format field-specific reasons and shorten long values for readable feedback.

Unknown command words are never executed as guesses. `CommandWords` first looks for command prefixes, then uses edit distance to find likely corrections. Suggestions help recover from a typo without risking an unintended destructive command. Add a new command to both dispatch and `COMMAND_WORDS`; `CommandWordsTest` checks that they agree.

The activity diagram shows the selection policy in `CommandWords.suggestFor()`. Prefix matches take precedence; otherwise only the nearest candidates within the edit allowance survive. More than three suggestions are suppressed. The edit distance supports adjacent transpositions, so a swapped pair of letters counts as one edit.

<puml src="diagrams/CommandWordSuggestion.puml" alt="Selecting command-prefix or nearest-edit suggestions and limiting their number" />

### Student values and duplicate detection

`StringUtil.normalizeFieldValue()` removes invisible format characters except the joiner and non-joiner needed by some scripts, removes non-whitespace control characters, collapses Unicode whitespace, trims the ends, and composes text to Unicode NFC. Field constructors use this shared representation so command input and loaded data follow the same storage rules.

The activity diagram shows the normalization order. The joiner exceptions preserve meaningful script characters while direction marks and zero-width spaces are removed.

<puml src="diagrams/NormalizeFieldValue.puml" alt="Field normalization preserving joiners and removing other format and control characters" />

| Value | Model rule and comparison behavior |
| --- | --- |
| `Name` | Requires a Unicode letter or number; preserves punctuation and script. Search and possible-duplicate warnings use a separate comparison key that ignores case, Latin accents/modifiers, curly apostrophe variants, and selected hyphen/space differences. Stored spelling is preserved. |
| `Phone` | Requires at least three decimal digits and preserves formatting. `getDigits()` removes other characters and converts Unicode decimal digits to ASCII for numeric search and possible-duplicate warnings. |
| `Email` | Checks TAB's supported `local-part@domain` form. Commands and storage enforce a 254-character limit. A valid address with a dotless domain is accepted with a warning when added or changed. |
| `NusStudentId` | Accepts 1–32 ASCII letters/digits and stores uppercase using `Locale.ROOT`. It is the sole identity key. |
| `Tag` | Requires a Unicode letter or number. Commands, storage loading, and summary grouping merge case-insensitive matches, keeping an appropriate display spelling. `Tag.equals()` itself remains case-sensitive. |

ID collisions and possible duplicates have different consequences. `StudentIdValidation` blocks `add` or `edit` when the requested ID belongs to another record, and `UniqueStudentList` enforces the same invariant at the model boundary. Without a shared present ID, students may coexist even when all their other details match.

`LikelyDuplicateWarning` appends advice when another record has a matching normalized name, digit sequence for a phone, or case-insensitive email. An edit checks only changed values and excludes the record being edited. These warnings allow the operation to succeed because matching contact details do not prove identity. The detector does not find every spelling variation or infer that two different ID values belong to one person.

The activity diagram isolates the duplicate checks in `AddCommand.execute()`: a shared present ID blocks addition, while matching contact details only add advice. Other advisories, filter handling, and saving are omitted; passing these checks continues through the rest of the add operation.

<puml src="diagrams/AddDuplicateCheckActivityDiagram.puml" alt="Blocking ID collisions while allowing additions with possible-duplicate warnings" />

### Selecting and editing students

`StudentTarget` represents either a displayed index or a NUS Student ID. The parser builds it; the command resolves it against the model at execution time. Indices are one-based in user input and converted through `Index` for list access. An ID lookup searches the entire book, including students hidden by the active filter.

| Target form | `edit` | `delete` / `tag` |
| --- | --- | --- |
| Displayed position | `edit 1 -p 81234567` | `delete 1` / `tag 1 -a T1` |
| Letter-leading ID | `edit A0123456X -p 81234567` | `delete A0123456X` / `tag A0123456X -a T1` |
| Explicit ID, including a digit-leading ID | `edit --target-id 001234 -p 81234567` | `delete --id 001234` / `tag --id 001234 -a T1` |

`edit` needs a separate `--target-id` because its `--id` option changes the ID field. A bare digit-leading target is read as an index. Reusing `StudentTarget` keeps target scope and diagnostics consistent across the three commands.

`EditStudentDescriptor` records changes without mutating the selected student. For optional details, its internal `OptionalFieldEdit<T>` stores both intent and value:

| Input for a phone | Descriptor state | Effect |
| --- | --- | --- |
| No phone option | Not edited | Keep the saved phone. |
| `-p 81234567` | Edited, with a value | Replace the phone. |
| `-p ""` | Edited, without a value | Clear the phone. |

Email and NUS Student ID use the same three states. This avoids confusing an absent option with a request to erase data. An option without its required value is a parsing error. Names cannot be cleared; `-t ""` clears all general tags, while nonempty `-t` options replace the entire tag set. An edit that produces an equal student returns an unchanged message and causes no data-file write.

### Filtering students and updating the view

`FindCommandParser` groups terms by field and passes them, together with an optional follow-up selector, to `DetailsContainsKeywordsPredicate`. Every supplied condition must match. A field term is a case-insensitive substring; repeated selectors require all their terms. For tags, each term may match any one tag, but a quoted phrase must occur within one tag.

For example, `find -n Jason -t Lab -t Tutorial -f` requires a name containing Jason, tags satisfying both group terms, and a pending follow-up. A missing optional detail cannot match a term for that field. Name matching uses the comparison key described above; phone searches additionally compare digits when the query contains only digits and common phone punctuation/spacing.

`FindCommand` replaces the model's predicate; it searches the whole book rather than narrowing the previous result. `ListCommand` restores the show-all predicate. Neither changes student records or writes the data file. Edits and tag changes keep the active predicate, so a changed student can disappear if it no longer matches.

Adding has one additional usability rule. If the new student fails the current predicate, `AddCommand` clears the search and explains why. Its `CommandResult` identifies the new student, and `MainWindow` asks `StudentListPanel` to scroll that record into view after a successful save. If the student already matches, the filter remains active.

`SummaryPanel` observes the displayed list and delegates counting to `StudentSummary`. It counts follow-ups separately from tags and counts each student once per case-insensitive tag group. Groups appear by descending count, then alphabetically for ties. A group's most-used spelling is displayed, with a deterministic tie-break, so results remain stable across list changes and restarts.

### Tags and follow-up status

`TagCommand` copies the selected student's tag set, adds or removes one case-insensitive match, and constructs a replacement student with all other details preserved. Adding an existing tag or removing an absent one reports that no change was made. This provides incremental grouping without rebuilding the entire tag set through `edit`.

The sequence diagram traces execution of `tag 1 -a T1` when the displayed student does not already have that tag. `StudentTarget` resolves the displayed index before the command replaces the record. Parsing has already constructed the command; Logic saves the changed book after execution, following [Saving changes safely](#saving-changes-safely).

<puml src="diagrams/TagCommandSequence.puml" alt="Resolving a displayed student and replacing its record to add tag T1" />

Follow-up is a separate status. `Student` holds an optional `FollowUpFlag`, a specialized `Tag` used for its display label, outside the general tag set. The `-f` option on `add` starts a flagged record; on `edit`, `-f` sets the status and `-nf` clears it. The shared selector parser rejects repeated or conflicting status options. `find -f` and `find -nf` use the same status as a filter.

The exact label `Needs follow-up` is reserved, ignoring case and normalized spacing. Commands reject it as a new general tag, and storage asks users to represent it with `"flag": true`. Related labels remain valid tags; `FollowUpTagWarning` explains that labels resembling the marker do not set follow-up status. Keeping status separate prevents a student from displaying and counting the same marker twice.

### Confirming a clear

`LogicManager` owns an optional pending `ClearCommand`. Executing `clear` only returns a prompt counting the full book, including hidden students. A nonempty book leaves confirmation pending; an empty book reports that there is nothing to clear.

The state diagram shows how subsequent submissions are handled. Replies are trimmed with the command whitespace rules and compared case-insensitively after invisible characters are removed. While confirmation is pending, input is interpreted as a reply rather than dispatched as another command.

<puml src="diagrams/ClearConfirmation.puml" alt="Clear confirmation states and accepted replies" />

On confirmation, Logic drops the pending request, snapshots the book and filter, and calls `executeConfirmed()`. Saving then follows the same procedure as another mutation. A failed save restores the records and filter, but the user must issue `clear` again to request another confirmation. The pending request is session state and is never persisted.

Keeping the pending request in Logic makes the confirmation protocol testable without JavaFX. `CommandBox` queries `Logic.isAwaitingConfirmation()` so an empty submission cancels a request and confirmation replies stay out of reusable command history.

### Saving changes safely

Logic coordinates in-memory changes and disk persistence. It copies the student book and remembers the active predicate before a command executes. Because student values are immutable, the copy can retain their references. After execution, equality with the snapshot determines whether saving is necessary.

The sequence diagram shows the shared execution path. Clear confirmation enters the equivalent path after the affirmative reply. The referenced storage procedure is expanded in the activity diagram that follows.

<puml src="diagrams/SaveRollbackSequenceDiagram.puml" alt="Saving changed student data and restoring the model after an I/O failure" />

If saving throws `IOException`, Logic restores the book and predicate before reporting `CommandException`. A command's success result reaches the UI only after a required save succeeds. Read-only commands, unchanged edits, clear prompts, cancellations, and invalid replies do not write student data.

`JsonStudentBookStorage` remembers the last book loaded or successfully saved for each normalized path. During saving it acquires a nonblocking file lock, reloads the disk book, and compares student records against that baseline. The following activity diagram focuses on this disk-side procedure.

<puml src="diagrams/StaleWindowSave.puml" alt="Locking, checking for stale student data, and replacing the saved file" />

The lock lives beside the resolved data file, normally as `data/.tab.json.lock`; resolving the target also coordinates windows using symbolic links. A busy lock, changed records, or a file that cannot be validated causes the save to fail with recovery guidance. The lock file remains after release; its existence does not mean a window holds the lock.

The comparison uses validated student-book content. Formatting-only edits and changes to the top-level `_comment` do not count as changed student records. The latest disk comment is carried into the replacement file. This check protects cooperating TAB windows from overwriting each other's records; external text editors do not participate in its locking protocol, so users must close TAB before hand-editing.

After validation, `JsonUtil` serializes the book and delegates replacement to `FileUtil.writeToFile()`. It writes UTF-8 to a temporary sibling, preserves existing POSIX permissions where supported, flushes the new file, and uses an atomic move to replace the target. On an I/O failure, it attempts to remove the temporary file and propagates the error. If atomic replacement is unsupported, saving fails rather than falling back to truncating the existing file.

These safeguards support local single-user work; they do not merge concurrent edits.

### Loading and preserving the data contract

The top-level JSON object contains `students` and an optional `_comment` string. Each student has a required `name`, optional `phone`, `email`, `nusStudentId`, `tags`, and `flag`. The [User Guide's file-editing section](UserGuide.md#editing-the-data-file) specifies the complete format and recovery instructions.

| Saved value | Loading behavior |
| --- | --- |
| Absent or `null` phone/email/ID | Load an absent detail. Blank normalized phone/email also mean absent; an explicit blank ID is invalid. |
| Absent or `null` tags | Load an empty tag set. A supplied array must contain valid strings; normalized case variants merge, retaining the first spelling. |
| Absent, `null`, or `false` flag | Load an unflagged student. Only a Boolean is accepted when a non-null value is supplied. |
| Legacy `address` key | Accept and ignore it for compatibility with earlier TAB files. |
| Unknown key, wrong type, invalid field, or duplicate present ID | Refuse the whole book and identify the problem instead of dropping the value. |

Saving omits absent optional details and a false flag, preserving a compact, editable file. Files predating these optional fields continue to load. The former `Needs follow-up` general tag requires migration to the separate `flag` field; TAB reports this explicitly rather than silently reinterpreting it.

`FileUtil` reads strict UTF-8 and accepts a leading byte order mark. `JsonUtil` rejects repeated object keys and trailing JSON values as well as malformed syntax. `JsonParseErrorFormatter` turns failures into `DataLoadingErrorMessage`, which separates the explanation from an optional line-numbered source excerpt. Adapter errors add the affected student record and name when available.

`MainApp` passes a student-data loading error to `UiManager`, which shows a repair dialog instead of the main window. The unreadable file remains untouched. A genuinely missing file is treated as first use and supplies sample students; these are written only after a command changes the book.

Preferences have a different recovery policy because they contain window settings rather than student records. Missing or unreadable preferences fall back to defaults and are rewritten when possible. A preference-save failure logs the file and reason while allowing the session or shutdown to continue.

### Supporting keyboard work

`CommandBox` owns a real JavaFX text field for editing and a `HighlightedTextOverlay` for colored text, caret, and selection. `CommandHighlighter` shares quote/option recognition helpers with the parser and marks syntactic roles rather than validating field contents. This preserves normal text editing while making command structure visible. `CommandTextField` converts pasted whitespace separators to spaces so copied multi-line text remains one inspectable command.

`CommandHistory` stores nonblank submissions for the session, including rejected commands, and skips consecutive identical submissions. Up/Down navigation preserves the unsent draft and restores it after the newest entry. Successful submissions clear the field; failed ones remain editable. Clear-confirmation replies are excluded from history because they depend on a pending request.

`MainWindow` defines a cyclic Tab order through commands, student list, tag summary, and visible result. `HelpPage` has keyboard paging and returns focus to the draft. `ResultDisplay` shows the latest reply and collapses when empty. These choices keep regular student-management tasks available without a pointing device.

Card layout adapts to the available width. `CardHeadline` shares room between the name and tags, `CardName` shortens names through `UiText` without splitting user-perceived characters, and `TagRow` keeps fitting chips on one row and summarizes hidden tags in an overflow chip. `ContactRow` wraps optional contact details, while absent labels are unmanaged so they leave no empty gaps. Tooltips retain full values when a card shortens them.

`WindowBounds` restores saved geometry onto an available screen and clamps oversized windows to its usable area. `UiFonts` selects fonts suitable for the platform, and `UiText` supplies safe display shortening. Keep layout-only helpers in UI; normalization that affects stored or searched values belongs in Commons or Model.

## Documentation, logging, testing, and dev-ops

* [Documentation](Documentation.md) describes the MarkBind workflow. Diagram sources belong in `docs/diagrams/` and are rendered by the `<puml>` elements beside their explanations.
* [Logging](Logging.md) describes `LogsCenter` and the Java logging configuration.
* [Testing](Testing.md) explains JUnit and JavaFX test execution. Reuse `StudentBuilder` and the other fixtures in `src/test/java/seedu/tab/testutil/` when adding tests.
* [DevOps](DevOps.md) describes the build and CI setup. Run `./gradlew check` before submitting a change.

Useful regression entry points include `CommandDispatchAndPersistenceTest` for command-to-file behavior, `ClearConfirmationTest` for the confirmation protocol, `LogicManagerTest` for save rollback, and `JsonStudentBookStorageConcurrencyTest` for stale saves and locks. Parser/model tests cover normalization and validation; UI tests exercise focus, layout, and list updates. The manual paths below complement these automated checks.

## Appendix: Requirements

### Product scope

**Target users:** Teaching assistants who manage tutorial or lab students on a desktop, type comfortably, and prefer short keyboard interactions during teaching work.

**Value proposition:** Keep student contact details, class tags, and pending follow-ups together in a local student book that can be searched and updated through typed commands.

TAB manages records and follow-up status locally. Messaging students, recording attendance, and recording grades are outside its scope. General tags can describe groups, but TAB does not model courses or classes as separate entities.

### User stories

Priorities are `***` (must have), `**` (nice to have), and `*` (lower priority). **Implemented** describes the present product; **Future** records an unimplemented requirement and does not promise a command in this release.

| Priority | As a… | I want to… | So that I can… | Status |
| --- | --- | --- | --- | --- |
| `***` | teaching assistant | start and exit TAB through terminal/keyboard commands | fit student management into a keyboard workflow | Implemented |
| `***` | teaching assistant | view all students and their recorded details | see who I am responsible for and how to contact them | Implemented |
| `***` | teaching assistant | add a student with only the details I know | record new students without inventing missing information | Implemented |
| `***` | teaching assistant | record names with local punctuation and non-Latin scripts | keep the names used on the official roster | Implemented |
| `***` | teaching assistant | keep different students with the same name | avoid treating names as unique identities | Implemented |
| `***` | teaching assistant | delete an individual student | remove records I no longer need | Implemented |
| `***` | teaching assistant | save record changes automatically | retain my work between sessions | Implemented |
| `***` | teaching assistant | mark and clear pending follow-ups | track which students still need a response | Implemented |
| `***` | teaching assistant | search a chosen field using part of a detail, ignoring case | locate a student from incomplete information | Implemented |
| `***` | teaching assistant | perform student-management tasks offline with local storage | keep student data on my machine | Implemented |
| `**` | new or forgetful teaching assistant | read command help inside TAB | learn or recall commands without leaving the app | Implemented |
| `**` | teaching assistant | edit or clear individual details | keep student information up to date | Implemented |
| `**` | teaching assistant | add or remove one tag without replacing the others | maintain class groups incrementally | Implemented |
| `**` | teaching assistant | find students by tag or follow-up state | focus on a group or outstanding work | Implemented |
| `**` | teaching assistant who edits JSON | receive a precise startup error for unreadable data | repair the file while preserving its records | Implemented |
| `*` | teaching assistant | record an optional NUS Student ID | match students to institutional records | Implemented |
| `*` | teaching assistant | identify a target by NUS Student ID when editing, tagging, or deleting | act on a record independently of its displayed position | Implemented |
| `*` | teaching assistant | record formatted international phone numbers | retain useful contact details for exchange students | Implemented |
| `*` | teaching assistant | see a newly added student even during a search | check the new record immediately | Implemented |
| `*` | teaching assistant | confirm before clearing the whole book | avoid losing records through one mistyped command | Implemented |
| `*` | teaching assistant | receive possible-duplicate warnings on name, phone, or email matches | review records that might describe the same person | Implemented |
| `*` | teaching assistant | see totals and tag/follow-up counts for the displayed students | understand the size and composition of the current result | Implemented |
| `*` | teaching assistant | recall recent commands and keep rejected input editable | reuse or correct input with fewer keystrokes | Implemented |
| `*` | teaching assistant | receive useful input errors and likely command corrections | recover from typing mistakes quickly | Implemented |
| `*` | teaching assistant using a laptop | open TAB within the available display area | begin work without moving an off-screen window | Implemented |
| `**` | teaching assistant | know how long a follow-up has been pending | identify stale interactions | Future |
| `**` | teaching assistant | undo my last change | recover from an accidental mutation | Future |
| `*` | teaching assistant | redo an undone change | restore a change reverted by accident | Future |
| `*` | teaching assistant with many students | import a roster and apply a shared tag | set up a class without adding every record separately | Future |
| `*` | teaching assistant with many students | edit a group of records together | avoid repeating the same change | Future |
| `*` | teaching assistant | delete an entire class | remove a group I no longer teach | Future |
| `*` | teaching assistant | define and reset tag aliases | reduce typing for frequently used group names | Future |
| `*` | teaching assistant | add and view student remarks | retain context such as requests or special needs | Future |
| `*` | teaching assistant | merge duplicate records after comparing them | combine their information without losing details | Future |
| `*` | teaching assistant | find students with no tags | identify records still needing a group | Future |
| `*` | teaching assistant | sort and group the displayed list | read records in an order suited to my task | Future |
| `*` | teaching assistant | export student records | share or print a roster outside TAB | Future |
| `*` | teaching assistant | reset the book to sample data | explore TAB before entering real records | Future |
| `*` | teaching assistant | choose separate data files per course | avoid mixing course rosters in one session | Future |
| `*` | long-time teaching assistant | archive past students and reset the active list for a new semester | retain historical records without cluttering ongoing work | Future |
| `*` | teaching assistant | auto-complete commands | reduce typing for common operations | Future |
| `*` | teaching assistant | revisit previous output | read a result after another command replaces it | Future |
| `*` | teaching assistant who edits JSON | reload saved data without restarting | continue after repairing a file | Future |
| `*` | teaching assistant | find students by the date of their last follow-up | identify students needing attention over time | Future |

### Use cases

For these use cases, the **system** is TAB and the **actor** is a teaching assistant. The main success scenario (MSS) describes successful interaction; extensions refer to the step where an alternative begins. Editing, tagging, and deletion share student-target resolution, so UC02 describes that common interaction once.

#### UC01: Add a student

**MSS**

1. The user requests to add a student, supplying a name and any known details.
1. TAB validates the details and checks the NUS Student ID, if supplied.
1. TAB adds and saves the student.
1. TAB confirms the addition and reveals the new student.

    Use case ends.

**Extensions**

* 2a. The command or a field is invalid.
  * 2a1. TAB explains the error without changing records.
  * Use case resumes at step 1.
* 2b. Another student holds the supplied NUS Student ID.
  * 2b1. TAB identifies the conflict without adding a student.
  * Use case resumes at step 1.
* 3a. Saving fails.
  * 3a1. TAB restores the records and filter and reports the reason.
  * Use case resumes at step 1.
* 4a. Name, phone, or email matches suggest a possible duplicate.
  * 4a1. TAB includes a warning in the successful result for the user to review.
  * Use case ends.

#### UC02: Find a student and update their details

**MSS**

1. The user requests students matching selected details or follow-up status.
1. TAB displays the matching students and summary.
1. The user selects a displayed student and supplies changes.
1. TAB validates the request, replaces the student's record, and saves it.
1. TAB confirms the change and updates the filtered view and summary.

    Use case ends.

**Extensions**

* 1a. The search is invalid.
  * 1a1. TAB explains the error and keeps the previous results.
  * Use case resumes at step 1.
* 2a. No students match.
  * 2a1. The user requests a different search or all students.
  * Use case resumes at step 2.
* 3a. The user supplies an ID target, including one outside the displayed results.
  * 3a1. TAB resolves the ID against the whole book.
  * Use case resumes at step 4.
* 4a. The target does not exist, a value is invalid, or the new ID conflicts.
  * 4a1. TAB explains the problem without changing records or the filter.
  * Use case resumes at step 3.
* 4b. The requested details equal the saved details.
  * 4b1. TAB reports that no change is needed without writing student data.
  * Use case ends.
* 4c. Saving fails.
  * 4c1. TAB restores the records and filter and explains recovery.
  * Use case resumes at step 3.
* 5a. The changed student no longer matches the filter.
  * 5a1. TAB removes the student from the displayed results while retaining the updated record.
  * Use case ends.

Setting/clearing follow-up uses the same edit flow. For deletion, step 3 requests removal and step 4 removes the target; field replacement and ID-conflict checks do not apply. UC03 covers the tag-specific alternatives.

#### UC03: Add or remove one tag

**MSS**

1. The user identifies a student and requests one tag addition or removal.
1. TAB resolves the target and validates the requested tag action.
1. TAB updates that tag, preserves the other details, and saves the record.
1. TAB confirms the action and refreshes the filtered view and summary.

    Use case ends.

**Extensions**

* 2a. The target or tag action is invalid.
  * 2a1. TAB explains the problem without changing records.
  * Use case resumes at step 1.
* 2b. The student already has a tag to add, or lacks a tag to remove.
  * 2b1. TAB reports that no change was made.
  * Use case ends.
* 3a. Saving fails.
  * 3a1. TAB restores the records and filter and reports the reason.
  * Use case resumes at step 1.

#### UC04: Clear the student book

**MSS**

1. The user requests to clear all students.
1. TAB asks for confirmation, stating the total number of students and the irreversible effect.
1. The user confirms.
1. TAB clears and saves the book, then confirms completion.

    Use case ends.

**Extensions**

* 2a. The book is empty.
  * 2a1. TAB reports that there is nothing to clear.
  * Use case ends.
* 3a. The user declines or submits an empty reply.
  * 3a1. TAB cancels without changing records or the filter.
  * Use case ends.
* 3b. The reply is unrecognized, including another command.
  * 3b1. TAB explains the accepted replies and repeats the prompt.
  * Use case resumes at step 3.
* 3c. The user closes TAB before replying.
  * 3c1. TAB leaves student data unchanged and discards the pending request.
  * Use case ends.
* 4a. Saving fails.
  * 4a1. TAB restores the records and filter and reports the failure; no confirmation remains pending.
  * Use case ends.

#### UC05: Recover from unreadable student data

**MSS**

1. The user starts TAB with an unreadable student data file.
1. TAB identifies the file and explains the loading problem instead of opening the student view.
1. The user closes TAB and repairs or replaces the file from a backup.
1. The user restarts TAB.
1. TAB loads the corrected records and opens the student view.

    Use case ends.

**Extensions**

* 2a. A syntax location is available.
  * 2a1. TAB includes the line, column, and a short source excerpt.
  * Use case resumes at step 3.
* 2b. A record or field can be identified.
  * 2b1. TAB includes that context in the explanation.
  * Use case resumes at step 3.
* 5a. The repair still leaves invalid data.
  * 5a1. TAB leaves the file unchanged and reports the remaining problem.
  * Use case resumes at step 3.

### Non-functional requirements

These are product acceptance targets. Timing and capacity targets require measurement on the test machine; passing unit tests alone does not establish them.

1. **Runtime:** Run on Windows, Linux, and macOS with Java 25 and the matching JavaFX runtime described in the UG. Plain-JDK Apple Silicon setups using the bundled Intel JavaFX natives require an x64 JDK under Rosetta; the documented native JDK+FX setup is an alternative.
1. **Portability:** Distribute one executable JAR of at most 100 MB, usable from a writable folder without an installer or administrator privileges.
1. **Keyboard access:** Make every student-management operation and in-app help available through the keyboard.
1. **Typing efficiency:** For a practiced typist, the median completion time for the task sequence below must be lower in TAB than in the defined mouse-driven LibreOffice Calc baseline. Both workflows must produce the same final student data without unresolved errors.
1. **Capacity and responsiveness:** Support 1,000 student records, with typical add, edit, delete, tag, list, and find commands updating the GUI within 100 ms and startup ready for input within 2 seconds. Measure on a local writable disk and record hardware, runtime, dataset, and repeated timings.
1. **Display:** Keep the interface readable at 1920×1080 with 100% and 125% scaling, and usable at 1280×720 with scaling up to 150%, using scrolling where necessary.
1. **Data locality:** Perform student-management operations offline and store student data locally without transmitting it to a network service.
1. **Single-user integrity:** Preserve existing saved records when loading fails or a command cannot save, and reject a stale cooperating TAB window's write rather than overwrite newer records. Concurrent edits are not merged.
1. **Editable data:** Accept correctly edited UTF-8 JSON, retain supported optional fields and the file description, and report malformed data without silently dropping invalid values.

#### Typing efficiency evaluation

Use a disposable book containing 100 students named `Student 0001` through `Student 0100`, with IDs `S0001` through `S0100`, phones `81230001` through `81230100`, and tag `T1`. Prepare a Calc sheet with the same data in Name, ID, Phone, Email, and Tags columns (A–E). Enable column filters over `A1:E102`, including the header and the empty row for the added student. Keep email empty, store IDs and phones as text, and separate tags with semicolons in the Tags column. Record the Calc version, TAB version, operating system, hardware, and display settings.

| Task | TAB workflow | Calc baseline |
| --- | --- | --- |
| Add a student | `add Student 0101 --id S0101 -p 81230101 -t T1` | Click the next empty row and enter the same values. |
| Find one student | `find -i S0050` | Use the ID column's filter dropdown to display only `S0050`. |
| Change that student's phone | `edit --target-id S0050 -p 91234567` | Click the matching phone cell and replace its value. |
| Add a group tag | `tag --id S0050 -a "Lab 3"` | Edit the matching Tags cell to contain `T1; Lab 3`. |
| Delete the added student | `list`, then `delete --id S0101` | Clear the filter using its dropdown, select the `S0101` row, and delete it through the row menu. |

Use the same participant for both workflows, after three untimed practice runs of each. In Calc, use the mouse for navigation, filters, and menus; the keyboard is available for entering cell values. In TAB, use its command box. Time five runs of each complete sequence, alternating the starting application between pairs and resetting the data before each run. Start with the application open and ready for input; stop after the deletion result is visible. Include correction time and verify the final records. Compare the two medians; this defines the acceptance test, not a measured performance claim.

### Glossary

| Term | Meaning in this guide |
| --- | --- |
| Student book | The complete collection of student records, including those hidden by a search. |
| Displayed / filtered list | The observable view selected by the current predicate; user-facing indices refer to this view. |
| Index | A one-based position in the displayed list, rather than a permanent student identifier. |
| NUS Student ID | The optional institutional identifier used as TAB's sole identity key when present. |
| Tag | A user-defined grouping label, such as a tutorial or lab group. |
| Follow-up | A pending action or response owed to a student, represented separately from general tags. |
| Preamble | Positional text before the first option, such as an add name or an edit target. |
| Option / selector | A hyphen-prefixed command token identifying a value or a presence-only condition. |
| NFC | Unicode Normalization Form C, used for the stored representation of field values. |
| Snapshot | A copy of the student book retained before a command so a failed save can restore it. |
| Save baseline | The last loaded or successfully saved book remembered by a storage instance for stale-write checks. |
| Stale window | A TAB session whose remembered book differs from the student records now saved on disk. |

## Appendix: Effort

TAB builds on AB3's layered JavaFX application, command/parser pattern, observable model, JSON storage, and test utilities. The architectural skeleton is reused; the main implementation work adapts that skeleton to a teaching assistant's student-management workflow. TAB still has one primary record type, so its added difficulty lies in behavior across layers rather than a larger entity model.

| Area | Work beyond AB3 and its main challenge |
| --- | --- |
| Student records and identity | Optional contact details and NUS Student IDs, ID-based targeting, follow-up status, and incremental tags require parsers, model invariants, persistence, and displays to agree. Hard ID conflicts must stay distinct from advisory contact matches. |
| Input and search | Positional names, quoted values, option aliases, Unicode normalization, combined field filters, and typo suggestions broaden accepted input. The challenge is preserving legitimate names and values while giving precise diagnostics for plausible mistakes. |
| Persistence and recovery | Save snapshots, rollback, disk baselines, file locks, and atomic replacement protect records when writes fail or another TAB window saves first. Validation must also preserve AB3's support for correctly hand-edited data files. |
| Keyboard workflow and presentation | Command history, in-window help, student reveal, and a summary of the filtered list connect command results to observable JavaFX views. UI state must remain coherent after searches, record replacement, and failed saves. |
| Verification and documentation | Parser, command, model, storage, and UI-helper tests cover these behaviors; the manual-testing appendix covers interactions and platform checks. The UG and this guide document the same input rules, recovery paths, and implementation boundaries. |

The work spans implementation, automated tests, manual verification, and documentation rather than a package rename alone. The implementation sections explain the resulting mechanisms and their limits: possible-duplicate detection is advisory, concurrent changes are rejected rather than merged, and timing targets still require measurement. Individual contribution evidence belongs in the team members' project portfolio pages and the repository history.

## Appendix: Instructions for Manual Testing

These paths provide representative inputs and observable results. Extend them with exploratory tests; the [UG](UserGuide.md) remains the reference for full command syntax. Run destructive and file-editing tests only on a disposable copy of TAB and its data. Run the command sections in order; restart the shared setup if you skip sections or explore changes that affect later prerequisites.

### Launch, help, and shutdown

1. Put `tab.jar` in an empty writable folder and run `java -jar tab.jar` with the documented Java setup.
   * Expected: Six sample students appear, none flagged, the command box has focus, and the summary shows six students. The status bar names `data/tab.json` in the JAR's folder.
1. Enter `help`, use Space/PgDn and `b`/PgUp to page, then `g`/`G` to reach the ends and `q` to return. Repeat with F1 and Esc. In help, `c` copies the UG URL.
   * Expected: Help stays inside the window, its progress changes with scrolling, and closing help returns to commands without changing students or the previous result.
1. Type an unfinished command, open help with F1, and close it. Use Tab and Shift+Tab through commands, students, summary, and the result when visible.
   * Expected: The draft is restored with the caret at its end, and each reading pane can be reached and scrolled from the keyboard.
1. Resize and move the window, enter `exit`, then relaunch. Also try the title-bar close action.
   * Expected: The window closes and its geometry is retained when it fits the available screen. Student data remains unchanged.
1. Move the window to a second monitor, exit, disconnect that monitor, and relaunch. Repeat at the display sizes/scalings in the NFRs.
   * Expected: TAB opens within an available screen. Cards, help, and summary remain usable through resizing and scrolling.

### Shared test records

Use this setup once before the following command paths. Each line is one command; respond to `clear` on the next submission. If the book is already empty, skip `yes`.

```text
clear
yes
add José O'Brien -p "+65 9123 4567" -e jose@example.com --id A61401 -t "Lab 3" -t Tutorial -f
add Alice Tan --id 001234 -t "Lab 3"
add Jason Lim -p 81234567 -e jason@example.com --id A61403 -t Tutorial
```

Expected: Three students appear in this order. Only José needs follow-up. The summary shows `Lab 3` and `Tutorial` with two students each and a total of three. Alice's card has no phone/email placeholders.

### Adding students and reviewing duplicates

1. Enter `find -n Nobody`, then `add Mei 林 -t "Lab 4"`.
   * Expected: The search clears, feedback explains why, and the new student is revealed. Missing phone, email, and ID are accepted.
1. Enter `add Different Name --id a61401`.
   * Expected: An ID-conflict error; no new student, filter change, or saved-data change.
1. Enter `add Jose O'Brien -p "+65 (9123) 4567" -e JOSE@example.com --id A61404`.
   * Expected: The record is saved with warnings for matching name, phone, and email. Its different ID permits it to coexist with José.
1. Enter `add Incomplete Probe -p 12 -e missing-at`, then `add Dotless Probe -e probe@nus`.
   * Expected: The first reports both invalid fields and adds nothing. The second succeeds with a dotless-domain warning.
1. Enter `add Siti s/o Kumaran -t 组别`, then `add Test -t t1 -t T1`.
   * Expected: The slash and Unicode text are accepted. The second record has one `t1` tag, retaining its first spelling.

### Searching and checking the summary

1. Enter `find -n jose`, then `find -p 9123-4567`.
   * Expected: Each finds José and the added Jose record; name accent comparison and phone digit comparison both work. Each result reports two students.
1. Enter `find -t "Lab 3" -t Tutorial -f`.
   * Expected: Only José appears, with a total of one and one pending follow-up. Both tags appear with count one.
1. Enter `find -nf -t "Lab 3"`, then `find -n "Jason Lim"`.
   * Expected: The first finds Alice; the second finds Jason, demonstrating that a new search starts from the whole book.
1. Enter `find --id a6140`, then `find -i 0012`.
   * Expected: The first finds José, Jason, and the added Jose record; the second finds only Alice. ID search is partial and ignores letter case.
1. Enter `find -e example.com -n Alice`.
   * Expected: No students match because Alice has no email. The displayed total and follow-up count are zero, with no tag rows.
1. Try `find Alice`, `find -t Lab 3`, `find -f -nf`, and `find -n ""`.
   * Expected: Each explains the invalid query and preserves the previous results and saved records.
1. Enter `list`.
   * Expected: Every added student returns. Tag groups are ordered by count, alphabetically on ties, and the follow-up count is separate from tag groups.

### Editing, clearing details, and resolving targets

1. Enter `find -n Jason`, then `edit --target-id 001234 -p 82345678 -e alice@example.com`.
   * Expected: Alice changes even though hidden; Jason remains the displayed result. `--target-id` treats the leading zeros as part of the ID.
1. Enter `edit 1 -p 83334444`, then `list`.
   * Expected: Jason's phone changes because index 1 refers to the filtered result at execution time; other students keep their details.
1. Enter `edit A61403 -p "" -e "" --id "" -t ""`, then `find -n Jason`.
   * Expected: Jason retains his name but loses phone, email, ID, and general tags. Restart TAB, run `find --id A61403` (no matches), then `find -n Jason` to confirm persistence and resume the path.
1. Enter `edit 1 -n "Jason Lim"`.
   * Expected: An unchanged message. The data file is not rewritten.
1. Try `edit 1 -n ""`, `edit 1 -p`, and `edit 1 --id A61401`.
   * Expected: Respectively, invalid name, missing phone value, and ID-conflict errors. The selected record and current filter stay unchanged.
1. Enter `edit --target-id 001234 --id 1ABC`, then `edit --target-id 1ABC -nf`.
   * Expected: Alice's ID changes to `1ABC`; the second command reaches that same record. `edit 1 -nf` would instead select the displayed position.

### Incremental tags and follow-up

1. Enter `list`, then `tag A61401 -a "Project Group"` and `tag A61401 -r "LAB 3"`.
   * Expected: José gains the new tag and loses `Lab 3` ignoring case, retaining Tutorial and his other details. Summary counts update.
1. Try `tag A61401 -a tutorial`, `tag A61401 -r Missing`, and `tag A61401 -a X -r Y`.
   * Expected: Existing-tag, absent-tag, and conflicting-action errors; records and file stay unchanged.
1. Enter `edit A61401 -nf`, then `find -f`.
   * Expected: José is no longer flagged and no student matches. General tags remain intact.
1. Enter `edit --target-id 1ABC -f`, then `find -f`.
   * Expected: Alice appears with the follow-up label. Restarting preserves her status.
1. Try `tag --id 1ABC -a "Needs follow-up"`, then `tag --id 1ABC -r "Needs follow-up"`.
   * Expected: Addition is rejected as a reserved marker. Removal explains that follow-up is separate and suggests the edit command that clears it.
1. Enter `tag --id 1ABC -a "Needs follow-up soon"`.
   * Expected: A normal tag is added and the existing flag remains set. Then try `tag --id 1ABC -a "Needs_follow_up"`: it is stored as a general tag with an advisory note that this spelling does not set follow-up status. Alice's existing follow-up remains set.

### Deleting with displayed and ID targets

1. Enter `find -n Mei`, then `delete 1`.
   * Expected: Only Mei is deleted; the filtered result becomes empty and the saved file excludes her.
1. Enter `delete --id A61404` while that result is empty.
   * Expected: The additional Jose record is deleted from the full book without clearing the active filter.
1. Try `delete 0`, `delete 999`, `delete --id Missing614`, and `delete 1 extra`.
   * Expected: Each gives a target/format error and preserves records, filter, and saved data. Enter `list` and restart to confirm only the intended students were deleted.

### Clear confirmation

1. Enter `find -n Nobody`, then `clear`.
   * Expected: The prompt counts the whole book despite the empty displayed result and warns that clearing cannot be undone. No data is written.
1. Enter `maybe`, then `exit`.
   * Expected: Both are rejected as replies; neither executes a command. The prompt remains pending and the data is unchanged.
1. Reply ` NO `, then enter `clear` and submit an empty line.
   * Expected: Each cancels, retaining the records and filter.
1. Enter `clear` and reply ` YeS `.
   * Expected: All students are cleared and the empty book survives a restart. Another `clear` reports that the book is already empty without starting a confirmation.
1. Add `add Pending Probe`, enter `clear`, close the window without replying, and restart.
   * Expected: Pending Probe remains and no confirmation is pending.

### Command entry, history, and long values

1. Enter `LiSt`, then `lsit`, then `list extra`.
   * Expected: The mixed-case command works; the typo suggests `list` without executing it; the extra argument is rejected. Failed input remains editable and its error marking clears after editing.
1. Enter `add Quote Probe --phone="9123 4567" --tag="Lab 5"`, then try `edit 1 -P 81234567` and `add Broken -t "Lab`.
   * Expected: Attached values are accepted; the capital option suggests `-p`; the unclosed quote is reported without changing records.
1. Paste `add “Paste Probe” –p “8123 4567” –t “Lab 6”`, then paste a command containing tab/newline separators before pressing Enter.
   * Expected: Curly quotes and a pasted option dash are recognized. Separators become spaces in one command, and the line's color follows its structure.
1. Enter `list` twice, then `clear` and reply `no`. Type an unsent draft, press Up repeatedly through accepted and rejected submissions, then Down to the newest position.
   * Expected: Recall changes the input without executing it; the draft returns after the newest command. The two consecutive `list` submissions occupy one history entry, and the `no` reply is absent. Restarting clears history.
1. Add a student with a long accented name and a long tag, then narrow the window. For a reproducible value, use `add José José José José José José José José José José José José José José José José -t "A very long tutorial group name used to check wrapping" -p "+65 9123 4567 extension 1234567890"`.
   * Expected: Card contents stay inside their bounds. Shortened values have full-value tooltips, contact details wrap, and summary tag names remain readable beside their counts.

### Loading hand-edited data

First rerun the shared test-record setup so the file contains three students with IDs and one flag. Close all TAB windows before each edit, back up this valid file, and restore that backup between malformed cases. Change one thing at a time so the reported cause is unambiguous.

1. Add `"_comment": "Manual testing copy"` to the top-level object. Remove Jason's phone/email/ID keys and José's `flag`; set José's `tags` to `[" t1 ", "T1", "Lab 3"]`. Restart, enter `tag 1 -a Probe`, and close TAB.
   * Expected: Missing details load as absent, the missing flag means unflagged, tags merge to `t1` and `Lab 3`, and the next save retains the description and omits absent details.
1. Try each malformed edit separately: `"phone": "12"`; `"email": ["a@b.com"]`; `"flag": "true"`; `"tags": [null]`; `"nusStudentId": ""`; or rename `nusStudentId` to `nusStudentID`.
   * Expected: Startup identifies the record and offending field/type or value. The main window does not open and the file is unchanged.
1. Give two students the same NUS Student ID with different letter case. Separately, put `Needs follow-up` into a general tag array.
   * Expected: The first identifies the duplicate ID and records. The second asks for the separate `flag` representation. Neither file is partially loaded or rewritten.
1. Remove a comma from valid JSON. Then try a repeated `name` key, a second JSON object after the book, and an empty file, restoring the backup each time.
   * Expected: Syntax errors identify a line/column and show a short numbered excerpt when a location is available. The empty file reports that it contains no data. Files remain unchanged.
1. Save a file containing `José` in Windows-1252, launch TAB, then resave it correctly as UTF-8 and relaunch. Also try UTF-8 with a leading BOM.
   * Expected: Invalid UTF-8 is rejected with encoding guidance; both supported UTF-8 forms preserve the accent.
1. Move the valid file elsewhere and start TAB, then close TAB and restore it. Separately, make the file/folder unreadable through the OS permissions UI and start TAB.
   * Expected: A genuinely missing file shows sample students. An unreadable file gives a loading error instead of sample data. Restore permissions after the test.
1. On macOS/Linux, close TAB, move `data/tab.json` to `tab-real.json` beside the JAR, and create a relative symbolic link with `ln -s ../tab-real.json data/tab.json`. Start TAB and enter `add Link Probe`, then close it.
   * Expected: `tab-real.json` contains the new student and `data/tab.json` remains a symbolic link. Replace the link with one pointing into a nonexistent folder and relaunch: startup reports the missing target folder without opening the normal window. Restore the original regular data file before further tests.
1. With TAB closed, replace `preferences.json` with `null`, then restart.
   * Expected: Default window settings are used and preferences are rewritten when writable. Student data is unaffected.

### Save failures and stale windows

1. With a saved student book, open two TAB windows from the same disposable JAR folder. In A enter `add Window A --id A61490`. In B enter `find -n Nobody`, then `add Window B --id A61491`.
   * Expected: A saves. B reports that disk data changed, restores its own earlier records and empty filter, and leaves A's file unchanged. Restart B to see Window A; adding Window B then succeeds.
1. Close every window. In the OS file manager, create a folder at `data/.tab.json.lock` after moving any existing lock file aside. Launch TAB, enter `find -n Nobody`, then `add Blocked Probe`.
   * Expected: Saving reports the lock-path problem; the added student is rolled back and the empty filter returns. Saved records are unchanged. `list`, `find`, and `help` still work. Close TAB and restore the lock path before continuing.
1. Repeat with the data folder made non-writable through the OS permissions UI. On macOS/Linux, `chmod 555 data` can be used for this disposable folder; restore with `chmod 755 data` afterward.
   * Expected: A mutation reports insufficient write access and restores both records and filter. Read-only commands still work. Check the original file against the backup.
1. In a separate empty test folder, put an ordinary file named `data` beside the JAR and attempt an add after launch.
   * Expected: TAB reports that a file blocks the required data folder, leaves that file intact, and does not retain the added student. Close TAB and move the blocking file before retrying.
1. Compare `data/tab.json` before and after `list`, `find -n Nobody`, a canceled `clear`, and an unchanged edit. Run `list` before selecting the student for that unchanged edit.
   * Expected: These operations do not rewrite student data. A successful mutation persists after restart; a rejected mutation leaves the saved book as it was.
