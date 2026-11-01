---
  layout: default.md
  title: "Developer Guide"
  pageNav: 3
---

# TAB Developer Guide

TAB (Teaching Assistant's Address Book) is an offline desktop application for teaching assistants to manage student details, groups, and pending follow-ups through typed commands. This guide explains its architecture and the implementation decisions behind it. Command syntax and field constraints are documented in the [User Guide](UserGuide.md).

<page-nav-print />

## Acknowledgements

* TAB builds on the layered architecture, command/parser structure, JavaFX UI, JSON storage, test utilities, and documentation of [AddressBook-Level3 (AB3)](https://github.com/se-edu/addressbook-level3) by the [SE-EDU initiative](https://se-education.org). This guide adapts the organization and explanations of the [AB3 Developer Guide](https://se-education.org/addressbook-level3/DeveloperGuide.html) to TAB's student model and behavior.
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

Historical JavaFX reports describe memory leaks with hardware rendering on Linux; see this [OpenJFX maintainer discussion](https://www.mail-archive.com/openjfx-dev%40openjdk.java.net/msg11422.html). This does not establish that TAB's supported JavaFX runtimes are affected; a TAB-specific leak has not been verified here.

### Logic component

The **API** of this component is specified in [`Logic.java`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/logic/Logic.java). The `delete` command is used to illustrate the design, and can be replaced with other commands.

<puml src="diagrams/LogicClassDiagram.puml" alt="Logic API, command hierarchy, dispatcher, and command results" />

The sequence diagram below illustrates the interactions within the `Logic` component, taking `execute("delete 1")` API call as an example.

<puml src="diagrams/DeleteExecutionSequence.puml" alt="Executing DeleteCommand c, deleting the displayed student, and saving changes" />

The first reference expands to the parsing diagram below. The second represents Logic's save and rollback step.

To see how `delete` command is parsed, refer to the sequence diagram below.

<puml src="diagrams/DeleteSequenceDiagram.puml" alt="Parsing delete 1 and returning DeleteCommand c to LogicManager" />

<div markdown="span" class="alert alert-info"><strong>Note</strong>: The destroy marker (X) marks the end of the parser's useful lifetime. Java garbage collection determines when its memory is reclaimed.
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
* maintains a `FilteredList<Student>` view of the student book for the current filter, such as search results. It exposes this unmodifiable view through the `ObservableList<Student>` API, so the UI can observe changes.
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
* Command words ignore case; option spellings remain case-sensitive. Repeated tags are allowed for `add` and `edit`, while repeated field selectors are allowed for `find`.

The following activity diagram illustrates how `CommandWords.suggestFor()` suggests corrections for an unknown command word.

<puml src="diagrams/CommandWordSuggestion.puml" alt="Selecting command-prefix or nearest-edit suggestions and limiting their number" />

How command suggestions work:

* Prefix matches take precedence over edit-distance matches.
* If no prefix matches, the method selects the nearest command words within the edit allowance shown in the diagram. Swapping adjacent characters counts as one edit.
* Suggestions are returned in command-list order. More than three matches produce no suggestions.
* Suggestions are displayed for the user to choose; they are never executed automatically.
* New commands must be added to both `StudentBookParser` and `COMMAND_WORDS`. `CommandWordsTest` checks that these remain consistent.

### Selecting and editing students

The `edit`, `delete`, and `tag` commands use `StudentTarget` to identify a student. The parser constructs the target; the command resolves it when executed. The following activity diagram shows the alternative index and ID resolution paths.

<puml src="diagrams/StudentTargetResolution.puml" alt="Resolving an index against the displayed list or a NUS Student ID against the entire student book" />

How target resolution works:

* An index, such as `1`, selects a student from the displayed list. `Index` converts the one-based input into a zero-based list position.
* A letter-leading target, such as `A0123456X`, is parsed as a NUS Student ID. The lookup includes students hidden by the current filter.
* An explicit ID option also accepts digit-leading IDs. `edit` uses `--target-id`, while `delete` and `tag` use `--id`. The separate edit option leaves `--id` available for changing the student's ID.
* An unresolved target produces a `CommandException` without changing records. If an out-of-range numeric index matches a stored ID, the feedback suggests the explicit ID option.

`EditStudentDescriptor` stores the requested changes separately from the selected student. The following activity diagram illustrates how `EditCommand` applies each optional field in turn before constructing a replacement record.

<puml src="diagrams/OptionalFieldEditing.puml" alt="Keeping, clearing, or replacing an optional field and applying the completed edit descriptor" />

The raked action expands to the following activity diagram, **Decide a field's value**.

<puml src="diagrams/OptionalFieldValue.puml" alt="Keeping, replacing, or clearing one optional field" />

How optional-field editing works:

* `OptionalFieldEdit<T>` records whether a field was edited and its requested value. Phone, email, and NUS Student ID use this representation.
* An omitted option keeps the saved value. A supplied value replaces it; an explicit empty value, such as `-p ""`, clears it.
* An option without a required value is rejected during parsing. It does not request a clear operation.
* The command repeats the raked action for each optional field, and `createEditedStudent()` combines all descriptor fields into a replacement `Student`. Names cannot be cleared. `-t ""` clears all general tags, while nonempty `-t` options replace the tag set.
* An equal replacement returns an unchanged result. Otherwise, the command checks ID uniqueness before calling `Model.setStudent()`. Logic saves only when the book changes.

### Tags and follow-up status

`TagCommand` adds or removes one general tag without replacing the remaining tags. The following sequence diagram illustrates `tag 1 -a T1` when the selected student does not already have that tag.

<puml src="diagrams/TagCommandSequence.puml" alt="Resolving a displayed student and replacing its record to add tag T1" />

How incremental tag changes work:

1. `TagCommand` resolves its `StudentTarget` against the model.
1. The command checks the student's tags for a case-insensitive match.
1. For a valid change, it copies the tag set, adds or removes the requested tag, and constructs a replacement `Student`. Other details are preserved.
1. The command calls `Model.setStudent()` and returns feedback. Since `Student` is immutable due to defensive programming, the model puts `updatedStudent` in the original student's place in the list rather than changing `student`. Logic saves the changed book after command execution.

Adding an existing tag or removing an absent tag throws a `CommandException` without changing the student.

Follow-up is stored separately from general tags. The following activity diagram illustrates how the parser interprets follow-up options for an edit.

<puml src="diagrams/FollowUpStatus.puml" alt="Rejecting repeated or conflicting follow-up selectors and returning the requested edit status" />

How follow-up status works:

* `Student` holds an optional `FollowUpFlag`, a subclass of `Tag` used for its display label, outside the general tag set.
* `add -f` creates a flagged student. On `edit`, `-f` sets the status, `-nf` clears it, and omitting both preserves it. Repeated or conflicting selectors are rejected by `ParserUtil.parseFollowUpSelector()`.
* `find -f` and `find -nf` filter by this status without changing it.
* The label `Needs follow-up` is reserved to prevent duplicate display and counting. Commands reject it as a new general tag; JSON stores the status in the separate `flag` field.
* `FollowUpFlag.isFollowUpName()` ignores case, equivalent dash/space separators, and trailing punctuation. Variants such as `Needs follow up` and `Needs followup` are also reserved.
* Other spellings, such as `Needs_follow_up`, remain general tags. `FollowUpTagWarning` explains that these do not set follow-up status.

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

Setting/clearing follow-up uses the same edit flow. UC03 covers the tag-specific alternatives, and UC06 covers deletion.

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

#### UC06: Delete a student

**MSS**

1. The user requests to delete a student identified by a displayed index or NUS Student ID.
1. TAB resolves the target student.
1. TAB removes the student and saves the updated book.
1. TAB confirms the deletion and refreshes the filtered view and summary.

    Use case ends.

**Extensions**

* 2a. The command is invalid or the target does not exist.
  * 2a1. TAB explains the problem without changing records or the filter.
  * Use case resumes at step 1.
* 2b. The user supplies an ID for a student outside the displayed results.
  * 2b1. TAB resolves the ID against the whole book.
  * Use case resumes at step 3.
* 3a. Saving fails.
  * 3a1. TAB restores the records and filter and explains recovery.
  * Use case resumes at step 1.

### Non-functional requirements

1. **Runtime:** Should work on any *mainstream OS* (Windows, Linux, macOS) with Java 25 and the matching JavaFX runtime as described in the UG.
1. **Portability:** Should be distributed as one executable JAR of at most 100 MB. This JAR should be usable from a writable folder without an installer or administrator privileges.
1. **Keyboard access:** Every student-management operation and in-app help should be available through the keyboard.
1. **Typing efficiency:** A practiced typist should have a lower median completion time when using TAB.
1. **Capacity and responsiveness:** Should support up to 1,000 student records, with typical add, edit, delete, tag, list, and find commands updating the GUI within 100 ms. Startup should be ready for input within 2 seconds. This can be measured on a local writable disk by recording hardware, runtime, dataset, and repeated timings.
1. **Display:** The interface should be readable and usable at 1920×1080 with 100% and 125% scaling, and at 1280×720 with scaling up to 150%.
1. **Data locality:** Should work offline and store student data locally without transmitting it to a network service.
1. **Single-user integrity:** Should preserve existing saved records when loading fails or a command cannot save. TAB should also reject a stale cooperating TAB window's write rather than overwrite newer records. Concurrent edits should not be merged.
1. **Editable data:** Should accept correctly edited UTF-8 JSON, retain supported optional fields and the file description, and report malformed data without silently dropping invalid values.

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

Given below are instructions to test the app manually.

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
1. Enter `add "Mary–Jane Tan"` with an en dash, then `find -n "Mary Jane"`, then `add "Mary Jane Tan"`.
   * Expected: The first record keeps its en dash in the card and the data file, the search finds it, and the second `add` reports a possible duplicate.
1. Enter `add DʼSouza --id TEST01` with a modifier letter apostrophe, then `find -n "D'Souza"` with the keyboard apostrophe, then `add "D'Souza" --id TEST02`.
   * Expected: The first record keeps its modifier letter in the card and the data file, the search finds it, and the second `add` reports a possible duplicate. Repeating the search with a grave accent in place of the apostrophe finds nobody.
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
   * Expected: Addition is rejected as a reserved marker and suggests the edit command that sets the flag on Alice. Removal explains that follow-up is separate and suggests the edit command that clears it.
1. Enter `tag --id 1ABC -a "Needs follow-up soon"`.
   * Expected: A normal tag is added and the existing flag remains set. Then try `tag --id 1ABC -a "Needs_follow_up"`: it is stored as a general tag with an advisory note that this spelling does not set follow-up status, and the note names the edit command that does. Alice's existing follow-up remains set.

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
1. Reply <code class="preserve-whitespace"> NO </code>, then enter `clear` and submit an empty line.
   * Expected: Each cancels, retaining the records and filter.
1. Enter `clear` and reply <code class="preserve-whitespace"> YeS </code>.
   * Expected: All students are cleared and the empty book survives a restart. Another `clear` reports that the book is already empty without starting a confirmation.
1. Enter `add Pending Probe`, enter `clear`, close the window without replying, and restart.
   * Expected: The prompt for a one-student book reads `Clear the only student?` rather than counting the student. Pending Probe remains and no confirmation is pending.

### Command entry, history, and long values

1. Enter `LiSt`, then `lsit`, then `list extra`.
   * Expected: The mixed-case command works; the typo suggests `list` without executing it; the extra argument is rejected. Failed input remains editable and its error marking clears after editing.
1. Enter `add Quote Probe --phone="9123 4567" --tag="Lab 5"`, then try `edit 1 -P 81234567` and `add Broken -t "Lab`.
   * Expected: Attached values are accepted; the capital option suggests `-p`; the unclosed quote is reported without changing records.
1. Enter `add Amy Lim -t"Lab 3"`, then `add Ben Ong --phone"+65 9123 4567"`.
   * Expected: Before submission, each glued option/value token has option coloring rather than quoted-value coloring. After submission, `There is no -tLab 3 option.` and `There is no --phone+65 9123 4567 option.` respectively. Neither command adds a student or changes saved data. Repeat with a space or `=` before the opening quote; both corrected forms record the field and leave the name unchanged.
1. Paste `add “Paste Probe” –p “8123 4567” –t “Lab 6”`, then paste a command containing tab/newline separators before pressing Enter.
   * Expected: Curly quotes and a pasted option dash are recognized. Separators become spaces in one command, and the line's color follows its structure.
1. Paste `add Dash Probe —phone 91234567`, where `—phone` carries the single em dash an editor writes for `--`, then `add Dash Probe —Phone 91234567`.
   * Expected: Both are refused as `-phone` and `-Phone`, each suggesting `--phone`, and no student is added.
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
1. Try each misshapen structure separately: replace the whole file with just its list of students, `[ { "name": "Bob" } ]`; give `students` an object instead of an array; or put `"Bob"` in place of one student record.
   * Expected: Startup says the file must be a JSON object holding a `students` array, then names the `students` field, then the offending entry, each with a line/column and excerpt. The main window does not open and the file is unchanged.
1. Give two students the same NUS Student ID with different letter case. Separately, put `Needs follow-up` into a general tag array.
   * Expected: The first identifies the duplicate ID and records. The second asks for the separate `flag` representation. Neither file is partially loaded or rewritten.
1. Remove a comma from valid JSON. Then try a repeated `name` key, a second JSON object after the book, and an empty file, restoring the backup each time.
   * Expected: Syntax errors identify a line/column and show a short numbered excerpt when a location is available. The empty file reports that it contains no data. Files remain unchanged.
1. Save a file containing `José` in Windows-1252, launch TAB, then resave it correctly as UTF-8 and relaunch. Also try UTF-8 with a leading BOM.
   * Expected: Invalid UTF-8 is rejected with encoding guidance; both supported UTF-8 forms preserve the accent.
1. Move the valid file elsewhere and start TAB, then close TAB and restore it. Separately, make the file/folder unreadable through the OS permissions UI and start TAB.
   * Expected: A genuinely missing file shows sample students. An unreadable file gives a loading error instead of sample data. Restore permissions after the test.
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

## Appendix: Planned Enhancements

1. **Whole-tag matching in `find`**

   **Limitation:** Tag searches use case-insensitive partial matching. For example, `find -t T1` also lists students tagged `T10`, `T11`, or `T12`, so a TA cannot use this search to list only tutorial group T1.

   **Planned enhancement:** Add an exact-match option for tag terms in a future version. A tag term beginning with `=` will match an entire tag name, ignoring case. For example, `find -t =T1` will match students with a tag named `T1` or `t1`, but will not match students whose only tags are `T10`, `T11`, or `T12`. Tag terms without the `=` prefix will continue to use partial matching.
