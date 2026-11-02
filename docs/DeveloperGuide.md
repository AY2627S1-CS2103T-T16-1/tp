---
  layout: default.md
  title: "Developer Guide"
  pageNav: 3
---

# TAB Developer Guide

TAB (Teaching Assistant's Address Book) is an offline desktop application for teaching assistants to manage student details, groups, and pending follow-ups through typed commands. This guide explains its architecture and the implementation decisions behind it. Command syntax and field constraints are documented in the [User Guide](UserGuide.md).

<page-nav-print />

## Acknowledgements

* TAB builds on the layered architecture, command/parser structure, JavaFX UI, JSON storage, test utilities, and documentation of [AddressBook-Level3 (AB3)](https://github.com/se-edu/addressbook-level3) by the [SE-EDU initiative](https://se-education.org). This guide adapts the organization, explanations, and manual-testing scenario structure of the [AB3 Developer Guide](https://se-education.org/addressbook-level3/DeveloperGuide.html) to TAB's student model and behavior.
* TAB continues to use the following from AB3.
   * [JavaFX](https://openjfx.io/), for the controls, FXML loader, observable collections, and graphics used by the desktop interface.
   * [Jackson](https://github.com/FasterXML/jackson), for JSON conversion through `JsonUtil`.
   * [JUnit 5](https://junit.org/junit5/), for automated testing.
   * [Gradle](https://gradle.org/), for build automation and dependency management.
* TAB re-uses or adapts the following ideas, but did not copy any implementation code.
   * Email local-part validation in `Email` uses the dot-atom character and dot-placement rules from [RFC 5322, section 3.2.3](https://www.rfc-editor.org/rfc/rfc5322.html#section-3.2.3).
   * `StringUtil.computeEditDistance` implements the recurrence for [optimal string alignment distance](https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance) to suggest mistyped command words.
   * The help pager's navigation keys and progress prompt are inspired by [OpenBSD more(1)](https://man.openbsd.org/more.1). The reuse is limited to interaction conventions.
* TAB uses the following technologies for documentation.
   * [MarkBind](https://markbind.org/) builds the documentation site.
   * [PlantUML](https://plantuml.com/) generates its UML diagrams from the sources in `docs/diagrams/`.

### AI usage declaration

OpenAI Codex, Claude, Google Antigravity, GLM 5.3, and Nemotron were
used throughout development to assist with implementation, testing,
debugging, documentation, and preliminary code reviews. These tools
were used to propose and implement code changes, generate and refine
tests, investigate bugs, review pull requests, and draft and revise
documentation.

## Setting up and getting started

Use [Setting up and getting started](SettingUp.md) for IDE setup, and the [User Guide quick start](UserGuide.md#quick-start) for the supported Java/JavaFX runtimes.

From the repository root:

```sh
./gradlew run
./gradlew check
./gradlew shadowJar
```

These commands launch TAB, run the tests and Checkstyle checks, and build `build/libs/tab.jar`, respectively. See [Testing](Testing.md) for test selection and [Documentation](Documentation.md) for the MarkBind preview workflow.

When packaged, TAB resolves `data/tab.json` and `preferences.json` relative to the JAR's directory. Development runs use the working directory. Run manual tests in a separate writable folder so they cannot touch real records.

## Design

### Architecture

<puml src="diagrams/ArchitectureDiagram.puml" alt="Dependencies between TAB's architectural components" />

The architecture diagram shows TAB's components using UML component notation. An arrow points from a component that uses an API or type to the component that provides it. `Storage` is a software component that reads and writes JSON files, rather than a database. Dependencies on shared `Commons` utilities are omitted for clarity.

The following provides a quick overview of the main components and their interactions.

**Main components of the architecture**

**`Main`** (consisting of classes [`Main`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/Main.java) and [`MainApp`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/MainApp.java)) is in charge of app launch and shutdown.

* At app launch, it initializes the other components in the correct sequence, and connects them up with each other.
* At shut down, it shuts down the other components and invokes cleanup methods where necessary.

The bulk of the app's work is done by the following four components:

* [**`UI`**](#ui-component): The UI of the App.
* [**`Logic`**](#logic-component): The command executor.
* [**`Model`**](#model-component): Holds the data of the App in memory.
* [**`Storage`**](#storage-component): Reads data from, and writes data to, the hard disk.

[**`Commons`**](#common-classes) represents a collection of classes used by multiple other components.

**How the architecture components interact with each other**

The *Sequence Diagram* below shows how the components interact with each other for the scenario where the user issues the command `delete 1`.

<puml src="diagrams/ArchitectureSequenceDiagram.puml" alt="Successful deletion flowing through UI, Logic, Model, and Storage" />

Each of the four main components (also shown in the diagram above),

* defines its *API* in an `interface` with the same name as the Component.
* provides its functionality through a concrete `{Component Name}Manager` class that implements the corresponding API interface.

For example, the `Logic` component defines its API in `Logic.java` and implements it in `LogicManager.java`. Other components interact with a component through its interface rather than its concrete class, preventing them from coupling to that component's implementation, as illustrated in the following partial class diagram.

<puml src="diagrams/ComponentInterfaces.puml" alt="UI and Logic implementations depend on component interfaces" />

The sections below give more details of each component.

### UI component

The **API** of this component is specified in [`Ui.java`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/ui/Ui.java).

<puml src="diagrams/UiClassDiagram.puml" alt="Main window's UI parts and selected Logic and Model dependencies" />

The UI consists of a `MainWindow` and its parts, such as `CommandBox`, `ResultDisplay`, `StudentListPanel`, and `StatusBarFooter`. All of these, including `MainWindow`, inherit from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component via `Logic.execute()`.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on classes in the `Model` component because it displays `Student` objects from the model.
* has a `HelpPage` component which is used when the `help` command is called.
* has a `StatusBarFooter` component which displays the data file's absolute path.

**Known issue (Linux):** JavaFX leaks about 50 bytes per keystroke on Linux. Its GTK code creates a character array for each key press and each key release in `WindowContextBase::process_key`, and never frees them. The leak is in JavaFX, not TAB, and JavaFX 25.0.4 has it too. A day of heavy typing leaks less than 1 MB, and closing TAB frees it. If a Linux heap profile shows `char[]` growing steadily, this is why.

**Known issue (long lists):** With around 1,000 students, a command run while the student list is scrolled near its end takes several times longer than one run from the top: about 0.4 s instead of 0.13 s on a fast Linux machine, and about 3 s on a Windows machine that meets NFR 2's minimum hardware, which misses NFR 2's 1-second target. From JavaFX 17, `VirtualFlow` lays out many more cells to estimate the list's height ([openjfx-dev discussion](https://mail.openjdk.org/archives/list/openjfx-dev@openjdk.org/message/2V4EDFSAV7S3NCS5DABSH5QBDWQE6FCJ/)), and `StudentListPanel` builds a new `StudentCard` from FXML for each cell it updates. Scrolling back to the top before running a command avoids the delay.

### Logic component

The **API** of this component is specified in [`Logic.java`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/logic/Logic.java). The `delete` command is used to illustrate the design, and can be replaced with other commands.

<puml src="diagrams/LogicClassDiagram.puml" alt="Logic API, command hierarchy, dispatcher, and command results" />

The sequence diagram below illustrates the interactions within the `Logic` component, taking `execute("delete 1")` API call as an example.

<puml src="diagrams/DeleteExecutionSequence.puml" alt="Executing DeleteCommand c and deleting the displayed student" />

To see how `delete` command is parsed, refer to the sequence diagram below.

<puml src="diagrams/DeleteSequenceDiagram.puml" alt="Parsing delete 1 and returning DeleteCommand c to LogicManager" />

<div markdown="span" class="alert alert-info"><strong>Note</strong>: The lifeline for `DeleteCommandParser` should end at the destroy marker (X), but due to a limitation of PlantUML, it continues to the end of the diagram.
</div>

How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to a `StudentBookParser` object, which in turn creates a parser that matches the command (e.g., `DeleteCommandParser`) and uses it to parse the command. No-argument commands are handled directly by `StudentBookParser`.
1. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `DeleteCommand`) which is executed by the `LogicManager`.
1. The command can communicate with the `Model` when it is executed (e.g. to delete a student).<br>
   Note that although this is shown as a single step in the diagram above for simplicity, the code can require several interactions between the command object and the `Model` to complete the operation.
1. The result of the command execution is encapsulated as a `CommandResult` object which is returned from `Logic` after any changed student data is saved successfully.

The following partial diagram expands the parser helpers, using `AddCommandParser` as a representative example. `FlagTokenizer` delegates token splitting to `CommandTokenizer` and builds a `FlagArgumentMap`. The command parser reads that map, validates fields through `ParserUtil`, and collects independent validation errors through `ParseProblems` before constructing `AddCommand`.

<puml src="diagrams/ParserClasses.puml" alt="Command dispatcher, representative add parser, tokenizers, and validation helpers" />

How the parsing works:

* For commands with a dedicated parser, the `StudentBookParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddCommandParser`). The parser uses the helper classes shown above to parse the user command and create an `XYZCommand` object (e.g., `AddCommand`). The `StudentBookParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddCommandParser` and `DeleteCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.

### Model component

The **API** of this component is specified in [`Model.java`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/model/Model.java).

<puml src="diagrams/ModelClassDiagram.puml" alt="Model API, read-only views, student book ownership, and immutable student values" />

The diagram includes the Model API and the read-only book/preferences interfaces exposed to consumers. Student fields use associations rather than composition because immutable values can be shared between replacement records; they are not exclusively owned by one student object.

The `Model` component,

* stores the student book data i.e., all `Student` objects (which are contained in a `UniqueStudentList` object).
* maintains a `FilteredList<Student>` view of the student book for the current search criteria, such as search results. It exposes this unmodifiable view through the `ObservableList<Student>` API, so the UI can observe changes.
* stores a `UserPrefs` object that represents the user's preferences (just the GUI settings). This is exposed to the outside through the `ReadOnlyUserPrefs` interface.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)

### Storage component

The **API** of this component is specified in [`Storage.java`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/storage/Storage.java).

<puml src="diagrams/StorageClassDiagram.puml" alt="Storage API and separate JSON storage delegates for student data and preferences" />

The `Storage` component,

* can save both student data and user preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonStudentBookStorage` and `JsonUserPrefsStorage` (one class per data file).
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`).

### Common classes

The `seedu.tab.commons` package contains classes shared across components. These include the following:

* `LogsCenter` for logging
* `Index` for displayed-list positions
* `StringUtil` for text normalization and comparison
* `FileUtil` and `JsonUtil` for file access and JSON conversion
* `AppUtil` for resolving the application directory
* `DataLoadingErrorMessage` for formatting startup diagnostics.

## Implementation

This section describes some noteworthy details on how certain features are implemented.

### Parsing commands and reporting mistakes

The following sequence diagram illustrates successful argument parsing for `add Alice -p 81234567`, after command dispatch. Each reference frame expands to a sequence diagram below with the same title.

<puml src="diagrams/CommandArgumentParsing.puml" alt="Add parsing overview with references to argument tokenization and field validation" />

How argument parsing works:

1. `AddCommandParser` tokenizes the arguments in the first referenced interaction.
1. It validates the supplied fields in the second referenced interaction.
1. The parser creates a `Student`, passes it to a new `AddCommand`, and returns that command. Execution and saving occur later.

The first reference expands to **Add argument tokenization**:

<puml src="diagrams/AddArgumentTokenization.puml" alt="FlagTokenizer delegates token splitting and creates the argument map for add" />

How argument tokenization works:

1. `AddCommandParser` calls `FlagTokenizer`, which delegates token splitting to `CommandTokenizer`. The `<<class>>` lifelines receive static method calls.
1. `FlagTokenizer` creates a `FlagArgumentMap`, records the name as the preamble, and stores the phone option and its value.
1. The populated argument map is returned to `AddCommandParser` for validation.

The second reference expands to **Add field validation**. The diagram shows name and phone validation; repetitive calls for absent optional fields are omitted.

<puml src="diagrams/AddFieldValidation.puml" alt="AddCommandParser validates the name and phone through ParseProblems and ParserUtil" />

How field validation works:

1. `AddCommandParser` asks the argument map to reject repeated single-valued options, then creates a `ParseProblems` collector.
1. The parser reads the name and phone text from the argument map. `ParseProblems` invokes the supplied parsing steps, which call `ParserUtil.parseName()` and `ParserUtil.parsePhone()`.
1. `throwIfAny()` returns normally because both values are valid. If validation had recorded errors, it would throw a `ParseException` before the `Student` or `AddCommand` is created in the overview diagram.

The error-handling rules are:

* Structural errors, such as an unclosed quote or unknown option, stop parsing before field validation.
* A standalone value beginning with a hyphen must be quoted to avoid option recognition. A quoted value must be separated from its option by a space or `=`.
* Where possible, invalid option spellings include a supported spelling suggestion, such as `--p` to `-p`. These suggestions do not execute a corrected command.
* Feedback identifies invalid fields and shortens long values. No-argument commands, such as `list`, reject extra text.
* Command words ignore case; option spellings remain case-sensitive. Repeated tags are allowed for `add` and `edit`, while repeated field options are allowed for `find`.

The following activity diagram illustrates how `CommandWords.suggestFor()` suggests corrections for an unknown command word.

<puml src="diagrams/CommandWordSuggestion.puml" alt="Selecting command-prefix or nearest-edit suggestions and limiting their number" />

How command suggestions work:

* Prefix matches take precedence over edit-distance matches.
* If no prefix matches, the method selects the nearest command words within two edits when the first letter matches, or one edit otherwise. Swapping adjacent characters counts as one edit.
* Suggestions are returned in command-list order. More than three matches produce no suggestions.
* Suggestions are displayed for the user to choose; they are never executed automatically.
* New commands must be added to both `StudentBookParser` and `COMMAND_WORDS`. `CommandWordsTest` checks that these remain consistent.

### Selecting and editing students

The `edit`, `delete`, and `tag` commands use `StudentTarget` to identify a student. The parser constructs the target; the command resolves it when executed. The following activity diagram shows the alternative index and ID resolution paths.

<puml src="diagrams/StudentTargetResolution.puml" alt="Resolving an index against the displayed list or a NUS Student ID against the entire student book" />

How target resolution works:

* An index, such as `1`, selects a student from the displayed list. `Index` converts the one-based input into a zero-based list position.
* A letter-leading target, such as `A0123456X`, is parsed as a NUS Student ID. The lookup includes students hidden by the current search criteria.
* An explicit ID option also accepts digit-leading IDs. `edit` uses `--target-id`, while `delete` and `tag` use `--id`. The separate edit option leaves `--id` available for changing the student's ID.
* An unresolved target produces a `CommandException` without changing records. If an out-of-range numeric index matches a stored ID, the feedback suggests the explicit ID option.

`EditStudentDescriptor` stores the requested changes separately from the selected student. The following activity diagram illustrates how `EditCommand` applies each optional field in turn before constructing a replacement record.

<puml src="diagrams/OptionalFieldEditing.puml" alt="Keeping, clearing, or replacing an optional field and applying the completed edit descriptor" />

How optional-field editing works:

* `OptionalFieldEdit<T>` records whether a field was edited and its requested value. Phone, email, and NUS Student ID use this representation.
* An omitted option keeps the saved value. A supplied value replaces it; an explicit empty value, such as `-p ""`, clears it.
* An option without a required value is rejected during parsing. It does not request a clear operation.
* The command makes this choice for each optional field in turn, and `createEditedStudent()` combines all descriptor fields into a replacement `Student`. Names cannot be cleared. `-t ""` clears all general tags, while nonempty `-t` options replace the tag set.
* An equal replacement returns an unchanged result. Otherwise, the command checks ID uniqueness before calling `Model.setStudent()`. Logic saves only when the book changes.

### Tags and the follow-up flag

`TagCommand` adds or removes one general tag without replacing the remaining tags. The following sequence diagram illustrates `tag 1 -a T1` when the selected student does not already have that tag.

<puml src="diagrams/TagCommandSequence.puml" alt="Resolving a displayed student and replacing its record to add tag T1" />

How incremental tag changes work:

1. `TagCommand` resolves its `StudentTarget` against the model.
1. The command checks the student's tags for a case-insensitive match.
1. For a valid change, it copies the tag set, adds or removes the requested tag, and constructs a replacement `Student`. Other details are preserved.
1. The command calls `Model.setStudent()` and returns feedback. Since `Student` is immutable due to defensive programming, the model puts `updated` in the original student's place in the list rather than changing `student`. Logic saves the changed book after command execution.

Adding an existing tag or removing an absent tag throws a `CommandException` without changing the student.

Follow-up is stored separately from general tags. The following activity diagram illustrates how the parser interprets follow-up options for an edit.

<puml src="diagrams/FollowUpStatus.puml" alt="Rejecting repeated or conflicting follow-up selectors and returning the requested edit status" />

How the follow-up flag works:

* `Student` holds an optional `FollowUpFlag`, a subclass of `Tag` used for its display label, outside the general tag set.
* `add -f` creates a flagged student. On `edit`, `-f` sets the flag, `-nf` clears it, and omitting both preserves it. Repeated or conflicting selectors are rejected by `ParserUtil.parseFollowUpSelector()`.
* `find -f` and `find -nf` filter by this status without changing it.
* The label `Needs follow-up` is reserved to prevent duplicate display and counting. Commands reject it as a new general tag; JSON stores the status in the separate `flag` field.
* `FollowUpFlag.isFollowUpName()` ignores case, equivalent dash/space separators, and trailing punctuation. Variants such as `Needs follow up` and `Needs followup` are also reserved.
* Other spellings, such as `Needs_follow_up`, remain general tags. `FollowUpTagWarning` explains that these do not set the follow-up flag.

## Documentation, logging, testing, and dev-ops

* [Documentation](Documentation.md) describes the MarkBind workflow.
* [Logging](Logging.md) describes `LogsCenter` and the Java logging configuration.
* [Testing](Testing.md) explains JUnit and JavaFX test execution.
* [DevOps](DevOps.md) describes the build and CI setup.

## Appendix: Requirements

### Product scope

**Target user profile:** Teaching assistants in National University of Singapore's School of Computing.
* Has a need to keep track of or manages tutorial or lab students
* Prefers desktop apps over other types of applications
* Can type comfortably and fast
* Strongly prefers typing on keyboard to using the mouse
* Familiar with using CLI apps and general CLI conventions

**Value proposition:** Teaching assistants can use TAB through typed commands to manage student contact details much more quickly and seamlessly than mouse-driven GUI applications.

### User stories

* Priorities: High (must have) - `***`, Medium (nice to have) - `**`, Low (unlikely to have) `*`
*  **Implemented** - the feature is present in current product, **Future** - the feature is planned to be released in future

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

For these use cases, the **system** is TAB and the **actor** is a teaching assistant (user).

#### UC01: Add a student

**MSS**

1. The user requests to add a student, supplying a name and any known details.
1. TAB validates the supplied student details.
1. TAB adds and saves the student.
1. TAB confirms the addition and shows the new student.

    Use case ends.

**Extensions**

* 1a. The command format is invalid.
  * 1a1. TAB explains the error without changing records or the current search criteria.
  * Use case resumes from step 1.
* 2a. A supplied field is invalid.
  * 2a1. TAB explains the error without changing records or the current search criteria.
  * Use case resumes from step 1.
* 2b. Another student holds the supplied NUS Student ID.
  * 2b1. TAB identifies the conflict without changing records or the current search criteria.
  * Use case resumes from step 1.
* 3a. Saving fails.
  * 3a1. TAB restores the records and current search criteria and reports the reason.
  * Use case resumes from step 1.
* 4a. The new student does not match the current search criteria.
  * 4a1. TAB shows all students and explains that the search has been cleared.
  * Use case ends.
* 4b. The supplied details warrant a warning, such as a possible duplicate name, phone, or email.
  * 4b1. TAB includes the warning in the successful result for the user to review.
  * Use case ends.

Extensions 4a and 4b may occur together.

#### UC02: Filter students by conditions

**MSS**

1. The user requests students matching selected details or the follow-up flag.
1. TAB displays the students matching all supplied conditions and their summary.

    Use case ends.

**Extensions**

* 1a. The command format is invalid.
  * 1a1. TAB explains the error without changing records or the current search criteria.
  * Use case resumes from step 1.
* 1b. The search conditions are invalid, such as requesting students both with and without the follow-up flag.
  * 1b1. TAB explains the error without changing records or the current search criteria.
  * Use case resumes from step 1.
* 2a. No students match.
  * 2a1. TAB displays an empty result and reports that no students were found.
  * Use case ends.

#### UC03: Edit a student's details

**MSS**

1. The user chooses a student and supplies changes to their details.
1. TAB validates the target student and the supplied student details.
1. TAB updates and saves the student's details, preserving unspecified details.
1. TAB confirms the change and refreshes the current search results and summary.

    Use case ends.

**Extensions**

* 1a. The command format is invalid.
  * 1a1. TAB explains the error without changing records or the current search criteria.
  * Use case resumes from step 1.
* 2a. The target student does not exist.
  * 2a1. TAB explains the problem without changing records or the current search criteria.
  * Use case resumes from step 1.
* 2b. A supplied field is invalid.
  * 2b1. TAB explains the problem without changing records or the current search criteria.
  * Use case resumes from step 1.
* 2c. The new ID conflicts with another student's ID.
  * 2c1. TAB identifies the conflict without changing records or the current search criteria.
  * Use case resumes from step 1.
* 2d. The requested details equal the saved details.
  * 2d1. TAB reports that no change is needed without changing records or the current search criteria.
  * Use case ends.
* 3a. Saving fails.
  * 3a1. TAB restores the records and current search criteria and reports the reason.
  * Use case resumes from step 1.
* 4a. The changed student no longer matches the current search criteria.
  * 4a1. TAB excludes the student from the results while retaining the updated record.
  * Use case ends.
* 4b. The changed details warrant a warning, such as a possible duplicate name, phone, or email.
  * 4b1. TAB includes the warning in the successful result for the user to review.
  * Use case ends.

Extensions 4a and 4b may occur together. Setting or clearing follow-up and replacing or clearing tags use
this same flow. UC04 covers changing one tag while retaining the others; UC06 covers deletion.

#### UC04: Add or remove one tag

**MSS**

1. The user chooses a student and requests one tag addition or removal for the chosen student.
1. TAB validates the target student and the requested tag action.
1. TAB updates that tag, preserves the other details, and saves the record.
1. TAB confirms the action and refreshes the current search results and summary.

    Use case ends.

**Extensions**

* 1a. The command format is invalid.
  * 1a1. TAB explains the error without changing records or the current search criteria.
  * Use case resumes from step 1.
* 2a. The target student does not exist.
  * 2a1. TAB explains the problem without changing records or the current search criteria.
  * Use case resumes from step 1.
* 2b. The tag is invalid.
  * 2b1. TAB explains the problem without changing records or the current search criteria.
  * Use case resumes from step 1.
* 2c. The user requests to add a tag whose name is reserved for the follow-up flag.
  * 2c1. TAB rejects the request without changing records or the current search criteria.
  * 2c2. TAB explains how to set the student's follow-up flag.
  * Use case resumes from step 1.
* 2d. The student already has the tag that the user wants to add (ignoring case and extra spaces).
  * 2d1. TAB reports that no change was made.
  * Use case ends.
* 2e. The student lacks the tag that the user wants to remove (ignoring case and extra spaces) and the tag is not their follow-up flag.
  * 2e1. TAB reports that no change was made.
  * Use case ends.
* 2f. The user requests removal of the student's follow-up flag as a tag.
  * 2f1. TAB reports that no change was made and explains how to clear the student's follow-up flag.
  * Use case ends.
* 3a. Saving fails.
  * 3a1. TAB restores the records and current search criteria and reports the reason.
  * Use case resumes from step 1.
* 4a. The changed student no longer matches the current search criteria.
  * 4a1. TAB excludes the student from the results while retaining the updated record.
  * Use case ends.
* 4b. The added tag has a permitted name that could be confused with the follow-up flag.
  * 4b1. TAB includes the warning in the successful result for the user to review.
  * Use case ends.

Extensions 4a and 4b may occur together.

#### UC05: Clear the student book

**MSS**

1. The user requests to clear all students.
1. TAB asks for confirmation, stating the total number of students and the irreversible effect.
1. The user confirms.
1. TAB clears and saves the book, then confirms completion.

    Use case ends.

**Extensions**

* 1a. The command format is invalid.
  * 1a1. TAB explains the error without changing records or the current search criteria.
  * Use case resumes from step 1.
* 1b. The book is empty.
  * 1b1. TAB reports that there is nothing to clear without asking for confirmation.
  * Use case ends.
* 3a. The user declines.
  * 3a1. TAB cancels without changing records or the current search criteria.
  * Use case ends.
* 3b. The user submits an empty or whitespace-only reply.
  * 3b1. TAB cancels without changing records or the current search criteria.
  * Use case ends.
* 3c. The reply is invalid, including another command.
  * 3c1. TAB explains the accepted replies.
  * Use case resumes from step 2.
* 4a. Saving fails.
  * 4a1. TAB restores the records and current search criteria and reports the reason.
  * Use case ends.

#### UC06: Delete a student

**MSS**

1. The user chooses a student and requests deletion of the chosen student.
1. TAB validates the target student.
1. TAB removes the student and saves the updated book.
1. TAB confirms the deletion and refreshes the current search results and summary.

    Use case ends.

**Extensions**

* 1a. The command format is invalid.
  * 1a1. TAB explains the error without changing records or the current search criteria.
  * Use case resumes from step 1.
* 2a. The target does not exist.
  * 2a1. TAB explains the problem without changing records or the current search criteria.
  * Use case resumes from step 1.
* 3a. Saving fails.
  * 3a1. TAB restores the records and current search criteria and reports the reason.
  * Use case resumes from step 1.

#### UC07: Recover from unreadable student data

**Precondition:** The saved student data file is unreadable or contains invalid data.

**MSS**

1. The user starts TAB.
1. TAB identifies the data file and explains the loading problem and recovery options, including the line,
   column, and a source excerpt for a syntax error, or the student record and field at fault,
   without changing the file or showing any student records.
1. The user closes TAB and repairs the file or replaces it with a valid backup.
1. The user restarts TAB.
1. TAB loads the corrected records and shows them.

    Use case ends.

**Extensions**

* 2a. TAB cannot locate the problem in the data file.
  * 2a1. TAB explains the problem without a location.
  * 2a2. The user closes TAB and replaces the file with a valid backup instead of repairing it.
  * Use case resumes from step 4.
* 4a. TAB still cannot load the file.
  * 4a1. TAB leaves the file unchanged and reports the remaining problem without showing any student records.
  * Use case resumes from step 3.

### Non-functional requirements

1. **Runtime:** Should work on any *mainstream OS* (Windows, Linux, macOS) with Java 25, set up as described in the [UG quick start](UserGuide.md#quick-start).
1. **Portability:** Should be distributed as one executable JAR of at most 100 MB. This JAR should be usable from a writable folder without an installer or administrator privileges.
1. **Keyboard access:** Every student-management operation and in-app help should be available through the keyboard.
1. **Typing efficiency:** A user who types at least 40 words per minute should be able to add a student with a name, phone number and email in under 30 seconds, including correcting one typing mistake.
1. **Capacity and responsiveness:** Should support up to 1,000 student records, with typical add, edit, delete, tag, list, and find commands updating the GUI within 1 second (median of 10 runs) on a machine with at least a 4-core x86-64 or Apple Silicon CPU and 8 GB of RAM. Startup should be ready for input within 2 seconds. This can be measured on a local writable disk by recording hardware, runtime, dataset, and repeated timings.
1. **Display:** The interface should be readable and usable at 1920×1080 with 100% and 125% scaling, and at 1280×720 with scaling up to 150%.
1. **Data locality:** Should work offline and store student data locally without transmitting it to a network service.
1. **Single-user integrity:** Should preserve existing saved records when loading fails or a command cannot save. TAB should also reject a stale cooperating TAB window's write rather than overwrite newer records. Concurrent edits should not be merged.
1. **Editable data:** Should accept correctly edited UTF-8 JSON, retain supported optional fields and the file description, and report malformed data without silently dropping invalid values.

### Glossary

| Term | Meaning in this guide |
| --- | --- |
| Student book | The complete collection of student records, including those hidden by a search. |
| Displayed / filtered list | The students currently shown after applying the current search criteria, or all students when no search criteria apply. |
| Index | A one-based position in the displayed list, rather than a permanent student identifier. |
| NUS Student ID | The optional institutional identifier used as TAB's sole identity key when present. |
| Tag | A user-defined grouping label, such as a tutorial or lab group. |
| Follow-up flag | Marks a student with a pending action or response owed to them. It is stored separately from general tags and shown as the fixed `Needs follow-up` label. |
| Preamble | Positional text before the first option, such as an add name or an edit target. |
| Option / selector | A hyphen-prefixed command token identifying a value or a presence-only condition. |
| Text normalization | Converting text to a consistent form for storage or comparison, such as trimming surrounding whitespace, collapsing repeated whitespace, and standardizing equivalent Unicode representations. |
| Snapshot | A copy of the student book retained before a command so a failed save can restore it. |
| Disk baseline | The last loaded or successfully saved book remembered by a storage instance for stale-write checks. |
| Stale window | A TAB session whose remembered book differs from the student records now saved on disk. |

## Appendix: Instructions for Manual Testing

These instructions provide a starting path through TAB's features. Testers should explore further variations using the [User Guide](UserGuide.md) for syntax and field rules.

<!-- The scenario/prerequisite/test case/expected result structure is adapted from -->
<!-- https://se-education.org/addressbook-level3/DeveloperGuide.html#appendix-instructions-for-manual-testing -->

### Launch and shutdown

1. Initial launch
   1. Prerequisites: Follow the [Java setup](UserGuide.md#quick-start), and place `tab.jar` in an empty writable folder reserved for testing.
   1. Test case: Run `java -jar tab.jar` from that folder.<br>
      Expected: The window opens with six sample students, a total of six in the summary, and the data path in the status bar.
1. Saving window preferences
   1. Test case: Resize and move the window, and enter `exit`.<br>
      Expected: The window closes.
   1. Test case: Relaunch TAB.<br>
      Expected: The window opens at the saved size and position when they fit the available screen.
   1. Test case: Close TAB using the window's close button.<br>
      Expected: The window closes.
   1. Test case: Relaunch TAB.<br>
      Expected: The saved window size and position are retained when they fit the available screen.

### Preparing test records

Reset the disposable book before each **top-level scenario** below, such as "Duplicate warnings and ID conflicts" (under "Adding students"). Run that scenario’s indented steps in order without resetting between them. Submit the setup commands below **one line at a time**.

* Skip `yes` if `clear` reports that the book is already empty.
* For file-related scenarios, first close all TAB windows and restore any files changed by the previous test.

```text
clear
yes
add José O'Brien -p "+65 9123 4567" -e jose@example.com --id A61401 -t "Lab 3" -t Tutorial -f
add Alice Tan --id 001234 -t "Lab 3"
add Jason Lim -p 81234567 -e jason@example.com --id A61403 -t Tutorial
```

Expected: For a nonempty book, `clear` asks for confirmation without deleting records; `yes` empties the book. Each `add` saves and displays the named student. After all three additions, three students appear in the order above, only José is flagged with `Needs follow-up`, and the summary counts two students in each tag group. Alice has no phone or email displayed.

### Adding students

1. Optional fields and revealing an added student
   1. Test case: `find -n Nobody`.<br>
      Expected: No students appear, and the summary counts are zero.
   1. Test case: `add Mei 林`.<br>
      Expected: Mei 林 is added without contact details or an ID. All four students appear, with feedback explaining that the search was cleared.
1. Duplicate warnings and ID conflicts
   1. Test case: `add Jose O'Brien -p "+65 (9123) 4567" -e JOSE@example.com --id A61404`.<br>
      Expected: A fourth student is saved, with possible-duplicate warnings for the name, phone and email.
   1. Test case: `add Different Name --id a61401`.<br>
      Expected: An ID-conflict error appears. No fifth student is added.
1. Field validation
   1. Test case: `add Invalid Probe -p 12 -e missing-at`.<br>
      Expected: Both invalid fields are reported, and the three records remain unchanged.
   1. Test case: `add Domain Probe -e probe@nus`.<br>
      Expected: The student is saved with a warning about the dotless domain.

### Finding students and checking the summary

1. Searching across fields
   1. Test cases: `find -n jose`, `find -p 9123-4567`, `find -e jose@`, `find --id a61401`, and `find -t "Lab 3" -t Tutorial -f`.<br>
      Expected: Each displays only José, with one student and one follow-up in the summary.
   1. Test case: `find -nf -t "Lab 3"`.<br>
      Expected: Only Alice appears, demonstrating that the new search starts from the full book. The summary shows one student and zero follow-ups.
   1. Test case: `find -n Alice -e example.com`.<br>
      Expected: No students appear, and the summary counts are zero.
   1. Test case: `list`.<br>
      Expected: All three students and their tag counts return.
1. Invalid searches
   1. Test case: `find -n Jason`.<br>
      Expected: Only Jason appears, at displayed index 1.
   1. Test case: `find Alice`.<br>
      Expected: An error explains that every search term needs a field option. Jason remains the only displayed student.
   1. Test case: `find -f -nf`.<br>
      Expected: An error reports conflicting follow-up options. Jason remains the only displayed student.
   1. Test case: `find -n ""`.<br>
      Expected: An error reports an empty search term. Jason remains the only displayed student.

### Editing students, tags and follow-up

1. Displayed indices and hidden ID targets
   1. Test case: `find -n Jason`.<br>
      Expected: Only Jason appears, at displayed index 1.
   1. Test case: `edit 1 -p 83334444`.<br>
      Expected: Jason's displayed phone changes to `83334444`. He remains the only displayed student.
   1. Test case: `edit --target-id 001234 -e alice@example.com`.<br>
      Expected: Feedback confirms Alice's updated email. Jason still remains the only displayed student.
   1. Test case: `list`.<br>
      Expected: All three students appear, including Jason with phone `83334444` and Alice with email `alice@example.com`.
   1. Test case: `edit A61403 -p "" -e "" --id "" -t ""`.<br>
      Expected: Jason retains his name but loses his optional details and tags.
   1. Test case: Restart TAB.<br>
      Expected: All three students load. Jason still has no optional details or tags.
1. Rejected and unchanged edits
   1. Test case: `edit 1 -n ""`.<br>
      Expected: An invalid-name error appears. All records remain unchanged.
   1. Test case: `edit 1 --id 001234`.<br>
      Expected: An ID-conflict error appears because Alice already has ID `001234`. All records remain unchanged.
   1. Test case: `edit 1 -n "José O'Brien"`.<br>
      Expected: An unchanged-record message is shown. Student data is not rewritten.
1. Replacing tags versus changing one tag
   1. Test case: `edit A61401 -t T1 -t T2`.<br>
      Expected: José's `Lab 3` and `Tutorial` tags are replaced by `T1` and `T2`.
   1. Test case: `tag A61401 -a "Project Group"`.<br>
      Expected: José gains `Project Group` while retaining `T1` and `T2`.
   1. Test case: `tag A61401 -r t1`.<br>
      Expected: José loses `T1`, leaving `T2` and `Project Group`. His contact details and follow-up flag remain unchanged throughout.
   1. Test case: `tag A61401 -a t2`.<br>
      Expected: An error reports that José already has tag `T2`. No tags change.
   1. Test case: `tag A61401 -r Missing`.<br>
      Expected: An error reports that José does not have tag `Missing`. No tags change.
1. Follow-up is separate from general tags
   1. Test case: `edit A61401 -nf`.<br>
      Expected: José's follow-up flag disappears. All three students remain visible.
   1. Test case: `edit --target-id 001234 -f`.<br>
      Expected: Alice gains the follow-up flag and retains her `Lab 3` tag. All three students remain visible.
   1. Test case: `find -f`.<br>
      Expected: Only Alice appears, with one student and one follow-up in the summary.
   1. Test case: `tag --id 001234 -a "Needs follow-up"`.<br>
      Expected: An error explains that the name is reserved for the follow-up flag and suggests `edit`. Neither tags nor follow-up change.

### Deleting students and clearing the book

1. Deleting by displayed index and by ID
   1. Test case: `find -n Jason`.<br>
      Expected: Only Jason appears, at displayed index 1.
   1. Test case: `delete 1`.<br>
      Expected: Feedback identifies Jason as deleted, and the displayed list becomes empty.
   1. Test case: `delete --id 001234`.<br>
      Expected: Feedback identifies Alice as deleted despite being hidden; the displayed list stays empty.
   1. Test case: `list`.<br>
      Expected: Only José appears, with one student in the summary.
   1. Test case: `delete 0`.<br>
      Expected: An error explains that an index must be a positive integer. José remains unchanged.
   1. Test case: `delete 999`.<br>
      Expected: An error reports that index 999 is outside the displayed list. José remains unchanged.
   1. Test case: `delete --id Missing614`.<br>
      Expected: An error reports that no student has NUS Student ID `MISSING614`. José remains unchanged.
   1. Test case: Restart TAB.<br>
      Expected: Only José remains in the book.
1. Canceling and confirming a clear
   1. Test case: `find -n Nobody`.<br>
      Expected: No students appear, and the summary counts are zero.
   1. Test case: `clear`.<br>
      Expected: A confirmation prompt counts all three students despite the empty view. No records are removed yet.
   1. Test case: `maybe`.<br>
      Expected: An invalid-reply message appears. Confirmation remains pending and records stay unchanged.
   1. Test case: `no`.<br>
      Expected: Clearing is canceled. All three records and the empty search result remain unchanged.
   1. Test case: `clear`.<br>
      Expected: A confirmation prompt counts all three students. No records are removed yet.
   1. Test case: `yes`.<br>
      Expected: Feedback confirms the book has been cleared. No students remain.
   1. Test case: `list`.<br>
      Expected: Zero students are reported and displayed.
   1. Test case: Restart TAB.<br>
      Expected: The book is still empty.
   1. Test case: `clear`.<br>
      Expected: An already-empty message appears without a confirmation prompt.

### Help, command entry and window navigation

1. Help and keyboard navigation
   1. Test case: `help`.<br>
      Expected: The built-in command reference opens.
   1. Test case: Press Space.<br>
      Expected: Help moves forward one screen and reading progress updates.
   1. Test case: Press `b`.<br>
      Expected: Help moves back one screen and reading progress updates.
   1. Test case: Press `G`.<br>
      Expected: Help reaches the end.
   1. Test case: Press `g`.<br>
      Expected: Help returns to the beginning.
   1. Test case: Press `c`.<br>
      Expected: The UG link is copied to the clipboard.
   1. Test case: Press `q`.<br>
      Expected: Command entry returns without changing students.
   1. Test case: Type an unfinished command and press F1.<br>
      Expected: Help opens.
   1. Test case: Press Esc.<br>
      Expected: Help closes and the unfinished command returns.
   1. Test case: Use Tab and Shift+Tab to move between panes.<br>
      Expected: The command box, student list, summary tag list and visible reply can be reached. Focused reading panes can be scrolled using the keyboard.
1. Parsing feedback and history
   1. Test case: `LiSt`.<br>
      Expected: The mixed-case command is accepted and all three students appear.
   1. Test case: `lsit`.<br>
      Expected: An unknown-command error suggests `list`. Records remain unchanged and failed input remains editable.
   1. Test case: `add Broken -t "Lab`.<br>
      Expected: An unclosed-quote error appears. Records remain unchanged and failed input remains editable.
   1. Test case: Type an unsent draft and press Up to recall earlier submissions.<br>
      Expected: Earlier commands appear in the input without executing.
   1. Test case: Press Down to return to the newest history position.<br>
      Expected: The unsent draft returns.
   1. Test case: Restart TAB and press Up in the command box.<br>
      Expected: No commands from the previous session are recalled.
1. Long details
   1. Test case: `add José José José José José José José José José José José José -t "A very long tutorial group name used to check wrapping" -p "+65 9123 4567 extension 1234567890"`.<br>
      Expected: The new student is saved and displayed with the supplied details.
   1. Test case: Narrow the window and hover over shortened names and tags.<br>
      Expected: Contents remain within their panes, shortened names and tags reveal full values, and contact details wrap.

### Saving and loading data

1. Hand-edited data and missing files
   1. Prerequisites: Close TAB and back up `data/tab.json` from the test JAR's folder.
   1. Test case: Add `"_comment": "Manual testing copy",` immediately after the top-level opening `{`, change Alice's name to `Alice 手动`, save as UTF-8, and relaunch TAB.<br>
      Expected: Alice appears as `Alice 手动`.
   1. Test case: `tag --id 001234 -a Probe`.<br>
      Expected: Alice gains `Probe` while retaining `Lab 3`.
   1. Test case: Close TAB and inspect `data/tab.json`.<br>
      Expected: The saved file contains the changed name, both tags and the comment.
   1. Test case: Move `data/tab.json` elsewhere and relaunch.<br>
      Expected: Six sample students appear. Close TAB and restore the backup before continuing.
1. Invalid data
   1. Prerequisites: Close TAB and back up the valid file. Restore this backup between cases.
   1. Test case: Remove a comma and launch TAB.<br>
      Expected: Startup reports a JSON syntax error. The main window does not open, and the edited file is left unchanged.
   1. Test case: Restore the backup, change José's `"flag": true` to `"flag": "true"`, and launch TAB.<br>
      Expected: Startup reports an invalid flag type. The main window does not open, and the edited file is left unchanged. Restore the backup to resume testing.
1. Save failure and rollback
   1. Prerequisites: Close all TAB windows. Open the test folder's `data` directory, move the `.tab.json.lock` file aside if present, and create a folder named `.tab.json.lock` in that directory.
   1. Test case: Relaunch TAB.<br>
      Expected: The three test students load.
   1. Test case: `find -n Nobody`.<br>
      Expected: No students appear, and the summary counts are zero.
   1. Test case: `add Blocked Probe`.<br>
      Expected: A lock-path error appears. No student is added, the empty search result returns, and the saved book is unchanged. Close TAB, remove the folder and restore the lock file if one was moved aside.
1. Two windows using the same file
   1. Prerequisites: Open two TAB instances, A and B, from the same test JAR.
   1. Test case: In A, enter `add Window A --id A61490`.<br>
      Expected: A saves and displays Window A as its fourth student.
   1. Test case: In B, enter `add Window B --id A61491`.<br>
      Expected: B reports changed disk data and rolls back Window B, retaining its three earlier students.
   1. Test case: Restart B.<br>
      Expected: Window A appears as the fourth student.
   1. Test case: In B, enter `add Window B --id A61491`.<br>
      Expected: Window B is saved and displayed as the fifth student.

## Appendix: Effort

TAB builds on AB3's layered JavaFX application, command/parser pattern, observable model, JSON storage, and test utilities. The architectural skeleton is reused; the main implementation work adapts that skeleton to a teaching assistant's student-management workflow. TAB still has one primary record type, so its added difficulty lies in behavior across layers rather than a larger entity model.

| Area | Work beyond AB3 and its main challenge |
| --- | --- |
| Student records and identity | Optional contact details and NUS Student IDs, ID-based targeting, the follow-up flag, and incremental tags require parsers, model invariants, persistence, and displays to agree. Hard ID conflicts must stay distinct from advisory contact matches. |
| Input and search | Positional names, quoted values, option aliases, Unicode normalization, combined field filters, and typo suggestions broaden accepted input. The challenge is preserving legitimate names and values while giving precise diagnostics for plausible mistakes. |
| Persistence and recovery | Save snapshots, rollback, disk baselines, file locks, and atomic replacement protect records when writes fail or another TAB window saves first. Validation must also preserve AB3's support for correctly hand-edited data files. |
| Keyboard workflow and presentation | Command history, in-window help, student reveal, and a summary of the filtered list connect command results to observable JavaFX views. UI state must remain coherent after searches, record replacement, and failed saves. |
| Verification and documentation | Parser, command, model, storage, and UI-helper tests cover these behaviors; the manual-testing appendix covers interactions and platform checks. The UG and this guide document the same input rules, recovery paths, and implementation boundaries. |

The work spans implementation, automated tests, manual verification, and documentation rather than a package rename alone. These mechanisms have known limits: possible-duplicate detection is advisory, concurrent changes are rejected rather than merged, and timing targets still require measurement. Individual contribution evidence belongs in the team members' project portfolio pages and the repository history.

## Appendix: Planned Enhancements

### Whole-Tag Matching in `find` (Coming soon)

**Limitation:** Tag searches use case-insensitive partial matching. For example, `find -t T1` also lists students tagged `T10`, `T11`, or `T12`, so a TA cannot use this search to list only tutorial group T1.

**Planned enhancement:** Add an exact-match option for tag terms. A tag term beginning with `=` will match an entire tag name, ignoring case. <br>
For example, `find -t =T1` will match students with a tag named `T1` or `t1`, but will not match students whose only tags are `T10`, `T11`, or `T12`.

Details:

* Tag terms without the `=` prefix will continue to use partial matching.

### Undo/Redo Commands (Coming soon)

**Limitation:** A TA cannot reverse an accidental change to student records within TAB. For example, deleting the wrong student or confirming `clear` removes records without a way to recover them through a command.

**Planned enhancement:** Add `undo` to reverse the most recent successful change and `redo` to restore the most recently undone change. This will cover `add`, `edit`, `tag`, `delete`, and confirmed `clear` commands, including changes to the follow-up flag. <br>
For example, after `delete 1`, `undo` will restore the deleted student and `redo` will delete that same student again.

Details:

* History will last for the open session and allow consecutive undo or redo operations.
* A new successful change after an undo will discard the remaining redo history.
* Searches, help, canceled clears, unchanged edits, and failed commands will not create history entries.
* Undo and redo will save the restored records. If saving fails, the records and history position will remain unchanged.
* When no change is available to undo or redo, TAB will explain this without modifying records.
