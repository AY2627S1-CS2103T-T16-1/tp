---
  layout: default.md
  title: "Developer Guide"
  pageNav: 3
---

# TAB Developer Guide

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## **Acknowledgements**

* Email local-part validation follows the dot-atom grammar in
  [RFC 5322 section 3.2.3](https://www.rfc-editor.org/rfc/rfc5322.html#section-3.2.3).
  The character set and dot-placement rules are used in `Email`; no code was copied.

* This project is based on the AddressBook-Level3 project created by the
  [SE-EDU initiative](https://se-education.org).
* [JavaFX](https://openjfx.io/) is used for the GUI. TAB uses JavaFX controls,
  FXML loading and related utilities to build and display its interface.
* [Jackson](https://github.com/FasterXML/jackson) is used for JSON serialization
  and deserialization of storage data (via `JacksonUtil`).
* [JUnit 5](https://junit.org/junit5/) is used for automated testing.
* `StringUtil.editDistance` implements the published
  [optimal string alignment distance](https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance),
  a known algorithm whose recurrence we wrote out ourselves. No code was
  copied, and the idea is reused in that one method.
* The help pager's navigation keys and progress prompt are inspired by the
  [OpenBSD more(1) manual](https://man.openbsd.org/more.1). This reuse is limited
  to interaction conventions; the JavaFX implementation is written for TAB.

--------------------------------------------------------------------------------------------------------------------

## **Setting up, getting started**

Refer to the guide [_Setting up and getting started_](SettingUp.md).

TAB supports JavaFX **17.0.7**, supplied by Gradle and bundled in the release
JAR. Use an x64 JDK 25 without bundled JavaFX to build and test this baseline.
CI tests it on Linux, macOS, and Windows. TAB also supports the course's Mac
setup, Zulu JDK+FX **25.0.3**, which uses the JDK's JavaFX **25** instead.
A separate macOS CI job tests that setup on Apple Silicon. See the
[testing guide](Testing.md) for the matching local setups.

The bundled JavaFX native libraries target x64. Apple Silicon Macs therefore
need an x64 JDK running under Rosetta when using plain JDK + JavaFX 17.
An ARM64 plain JDK cannot load these libraries. The course's native ARM64
JDK+FX setup supplies its own JavaFX libraries and does not need Rosetta.

--------------------------------------------------------------------------------------------------------------------

## **Design**

### Architecture

<puml src="diagrams/ArchitectureDiagram.puml" width="280" />

The ***Architecture Diagram*** given above explains the high-level design of TAB.

The following provides a quick overview of the main components and their interactions.

**Main components of the architecture**

**`Main`** (consisting of classes [`Main`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/Main.java) and [`MainApp`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/MainApp.java)) is in charge of TAB's launch and shut down.
* At TAB's launch, it initializes the other components in the correct sequence, and connects them up with each other.
* At shut down, it shuts down the other components and invokes cleanup methods where necessary.

The bulk of TAB's work is done by the following four components:

* [**`UI`**](#ui-component): The UI of TAB.
* [**`Logic`**](#logic-component): The command executor.
* [**`Model`**](#model-component): Holds the data of TAB in memory.
* [**`Storage`**](#storage-component): Reads data from, and writes data to, the hard disk.

[**`Commons`**](#common-classes) represents a collection of classes used by multiple other components.

**How the architecture components interact with each other**

The *Sequence Diagram* below shows how the components interact with each other for the scenario where the User issues the command `delete 1`.

<puml src="diagrams/ArchitectureSequenceDiagram.puml" width="574" />

Each of the four main components (also shown in the diagram above),

* defines its *API* in an `interface` with the same name as the Component.
* provides its functionality through a concrete `{Component Name}Manager` class that implements the corresponding API interface.

For example, the `Logic` component defines its API in `Logic.java` and implements it in `LogicManager.java`. Other components interact with a component through its interface rather than its concrete class, preventing them from coupling to that component's implementation, as illustrated in the following partial class diagram.

<puml src="diagrams/ComponentManagers.puml" width="300" />

The sections below give more details of each component.

### UI component

The **API** of this component is specified in [`Ui.java`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/ui/Ui.java)

<puml src="diagrams/UiClassDiagram.puml" alt="Structure of the UI Component"/>

The UI consists of a `MainWindow` and its parts, such as `CommandBox`, `ResultDisplay`, `StudentListPanel`, `SummaryPanel`, and `StatusBarFooter`. All of these, including `MainWindow`, inherit from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts.

`CommandBox` owns its `CommandHistory` and `CommandTextField`, and calls the static `CommandHighlighter.highlight` method to color the input. The card controls extend JavaFX classes directly: `CardHeadline` extends `Pane`, `CardName` extends `Label`, `TagRow` extends `Region`, and `ContactRow` extends `FlowPane`. These controls are composed in `StudentListCard.fxml`.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/ui/MainWindow.java).

The `UI` component,

* executes commands entered by the User using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Student` objects from the model.

The student card's `TagRow` creates a label for every tag, with its full text.
Layout shows as many complete chips as fit and lists all hidden tags in the
overflow chip's tooltip. There is no fixed chip count or label length cap.

`SummaryPanel` wraps complete tag names within the fixed 150 px sidebar.
Each count retains its preferred width, while the tag list scrolls vertically
as wrapped rows grow. Names are not shortened, so tags differing only at the
end remain distinguishable without hovering.

`HelpCommand` returns a command summary and the existing `showHelp` signal.
`MainWindow` routes that signal, F1 and the Help menu to `HelpPage`, which replaces
the entire student page (command box, result, summary sidebar and student list).
The hidden student page remains intact so closing help restores its filter,
previous result and any unfinished command. When returning focus, `CommandBox`
moves the caret to the end and clears JavaFX's automatic selection so the next
keystroke continues editing the draft. The summary uses command words and
flag labels from the command/parser definitions.

`HelpPage` is a keyboard pager: Space/f and b move a screenful with one line of
context, Enter/j and k move a line, d/u move half a screenful, g/G go to either end,
and q/Esc return to commands. PgUp/PgDn, arrows and Home/End are also accepted.
A wrapped label inside a scrollbar-free viewport takes the remaining window height.
Page movement uses the current viewport and font metrics, so resizing and different
fonts do not skip text. A persistent footer shows progress and navigation keys.
The c key copies `HelpCommand.USER_GUIDE_URL` to the system clipboard and confirms
it in the footer. Unused typed keys show a reminder to quit help before entering
a command. The reference keeps explicit lines short enough for minimum width;
the long User Guide address is available through c instead of wrapping in the label.
Letter commands are handled on `KEY_TYPED`, consuming q before focus returns to the
command box so quitting cannot insert a stray character. Enter navigates on
`KEY_PRESSED`; its typed newline is consumed without changing the notice or scrolling
again, including when focus moves into help after submitting `help`.
The empty typed event emitted by macOS F1 is also consumed so it
cannot overwrite the opening notice.
No student data is changed.

#### Restoring window geometry

`MainWindow` supplies each screen's visual bounds to `WindowBounds`, with the
primary screen first. Equal overlap therefore favors the primary screen.
Restoration chooses the screen with the greatest overlap with the saved window,
preserving valid secondary-screen positions, including negative coordinates.
With no overlap or no saved position, it centers on the primary screen. It clamps size and position to that screen's usable area;
nonpositive or nonfinite dimensions use the default size. The window minimum
also yields to a smaller usable area so it cannot defeat the size correction.
A window spanning adjacent displays is fitted entirely onto the selected screen,
even when it was visible across both. This keeps restoration predictable when
displays have different usable heights or gaps between them.

#### Fitting names and tags on a card

`CardHeadline` prefers a readable start of the name when allocating room to
tags, but yields part of that allowance for a complete tag count. The name
retains space for a character from each end. When no tag chip fits, `TagRow`
uses the shorter `N tags` total instead of `N more…`, preserving a complete
count beside the name. Its tooltip still lists every hidden tag.
`TagRow.fittedWidth` reports the space its chosen chips or count actually use,
so `CardHeadline` returns any unused allocation to the name before laying it
out. The name requests its full natural width.

#### Keeping contact details readable

`StudentListCard.fxml` puts NUS Student ID, phone and email in `ContactRow`,
a `FlowPane` that reports the natural width of one unwrapped row. This lets
the containing boxes measure height at the allocated width on both JavaFX
17 and 25, rather than reserving rows based on a fixed preferred wrap length.
Labels keep their preferred widths and share a line when they fit; a label
that does not fit the remaining space moves to the next line. Each label's
maximum width follows the pane's width, so a value longer than an entire
line shrinks with an ellipsis instead of overflowing the card.

This keeps ordinary cards compact while allowing crowded cards to grow by
only the rows they need. `UiText.showToFit` gives all three fields their full
values on hover when clipped, and removes the tooltip when widening makes
it unnecessary. Absent optional fields remain unmanaged, reserving no space.
The follow-up pill shares an `HBox` with `CardHeadline` inside the details
container, so it reserves width only on the headline. Contact rows use the
same width with or without a flag. The details clip prevents drawing outside
the card.

`StudentListViewCell` measures preferred height at the smaller of the cell's
content width and the card's maximum width, matching the width used by its
skin during layout. Measuring at the wider cell width would miss a wrapped
contact row near a wrap threshold and squeeze the headline.

#### Colouring the command line

`CommandTextField` converts separators to spaces in text replacement before
JavaFX filters control characters. It asks `StringUtil#isWhitespace` the
question the tokenizer asks of each character, so the box cannot hide a
boundary the parser reads: JavaFX deletes a vertical tab or a form feed along
with the other control characters, and draws a line, paragraph or next-line
separator with no width, each of which joins the words around it on screen.
The field preserves pasted word boundaries while keeping the command on one
line, including when a paste replaces selected text. CRLF is treated as one
separator.

`CommandHighlighter` breaks a command where the parser breaks it, which the
User Guide invites the User to rely on. That means two splits the colouring
cannot make with the rule it uses for values. `StudentBookParser` ends the
command word at the first separator whatever the quotes, so `add"Ravi s/o
Kumaran"` is the command word `add"Ravi` and two words after it; reading that
word with the value rule would draw one bold quoted word and then report an
unknown command the colours never showed. `FlagTokenizer` reads `--tag="Lab
3"` as an option and a value, so the marker and the equals sign are coloured
as the option and the rest as its value. A marker holding a quote or a
backslash is left whole instead, because either moves the equals sign the
parser counts to, and a word split in the wrong place would be the same bug
again.

A `TextField` cannot colour its own content, so `CommandBox` keeps the field
for input and draws a coloured copy of the command behind it, with the field's
own text transparent. The field still takes every keystroke and still decides
which character a click lands on, which keeps editing exactly as it was.

`CommandHighlighter` decides the colours. It returns spans covering every
character of the command. It walks the command itself, rather than calling
`CommandTokenizer`, because it has to keep the text exactly as typed, quotes
and all, where the tokenizer removes them and may refuse an unclosed quote
that is merely half-typed. It asks the tokenizer the questions the two must
agree on — `CommandTokenizer#isEscapable`, and `StringUtil#isWhitespace` for
where a token ends — and `CommandHighlighterTest` runs both over a list of
awkward commands and compares how many tokens each found. That catches one of
them splitting a command into a different number of pieces; it does not
compare where each piece begins and ends, so a boundary that moves without
changing the count slips through. It never rejects anything: a
command is coloured while it is still half-typed.

The copy measures its natural width from the whole shaped `TextFlow`, rounded
up to avoid wrapping at a fractional pixel boundary. Ordinary value words and
spaces share a text node so that splitting an unquoted right-to-left name does
not change shaping or click positions.

Three things then have to agree with the field, because the User reads our copy
but interacts with the field:

* **the scroll offset.** The copy is drawn wherever the field is drawing its
  own text, read from the field's text node rather than worked out again here.
  A field scrolls by rules that differ between typing, deleting, cutting and
  undoing, and every attempt to reproduce them was right for some of those and
  wrong for others; a copy even a pixel out of step shows one character where
  the field hit-tests another, and clicks land in the wrong place. Reading
  that node once is not enough, because the field's skin re-scrolls it after a
  delete, a replace or a change of width, which is after the caret and the
  text the colouring was told about have both settled. The copy therefore
  follows the node's own position. The node is the one the field is drawing
  in, since an empty line shows the prompt in a node of the same kind which
  the field hides rather than throws away, and a command reading as the
  prompt would otherwise be taken for the hidden one.
* **the caret.** The field's caret is drawn in its text colour, which is
  transparent, so the line carries one of its own, sized from a run of the text
  so it sits where the text does. Its horizontal position follows the primary
  caret shape of the field’s text node, including its leading/trailing bias at
  script boundaries. Logical prefix widths cannot locate RTL insertion points.
* **the selection.** The field draws its highlight at its own offset, which
  broke into fragments behind the copy, so it is turned off in the stylesheet
  and drawn here instead. A path from `TextFlow.rangeShape` covers each visual
  segment, including selections that cross RTL/LTR boundaries and appear as
  separate bands.

`CommandBoxTest` checks the contrast of the selection band against the line,
and of every colour the line uses against that band, including the one a
refused command is written in, so that selecting a command cannot hide half of
it. The bar it holds them to is 3:1 rather than the 4.5:1 WCAG asks of text
this size: on a window this dark, a band light enough to be seen against the
line (2.2:1) lightens the background enough that 4.5:1 would need every token
colour moved towards white, which is not the palette the mockup sets. The
trade-off is deliberate and worth revisiting if the palette changes.

#### Recalling session commands

`CommandBox` records each submitted line in `CommandHistory` before execution,
so rejected input can be recalled too. The helper stores submissions separately
from the draft captured when Up first leaves the newest position. Down past
the newest submission restores that draft. Up skips the newest submission when
it already matches the text in the box and an earlier entry exists. Blank
submissions and consecutive identical submissions are skipped; editing a recalled entry does not alter stored text.

The command box consumes unmodified Up/Down key presses before the text field
handles them. Recalled text uses the existing text listener for colouring, and
the caret moves to its end. This UI state is session-only and never enters the
student model or persisted data.

#### Summarising the list

`SummaryPanel` shows the flagged count, a count per tag and the total, beside
the list. It counts `Logic#getFilteredStudentList()`, which is the only list
`Logic` exposes, so it summarises **what the list is showing** rather than the
whole student book: after `find -n Alex` it counts the matches. That is the
reading the User Guide documents, and it keeps the panel answering the same
question as the list it sits beside. Summarising the whole book instead would
need `Logic` to expose it.

The list is an `ObservableList`, so a listener keeps the counts current
without the panel being told when a command changes anything. The counting
itself is static and takes a plain `List<Student>`, so it is tested without a
toolkit; only the rendering needs one.

Tags counted the same are listed alphabetically ignoring case, because a
reader does not read `Zoo` as coming before `apple`.

Case variants of one tag are counted as one tag, because that is what `tag`
makes of them when it refuses a tag a student already carries: a reader
looking for the size of tutorial group `T1` would otherwise have to add up a
row per spelling. The row is named with the spelling most of its students
carry, which is settled by the counts rather than by the order of the list,
so narrowing the list does not read as a second tag appearing.

The panel groups two spellings with `String.CASE_INSENSITIVE_ORDER`, which
calls them equal exactly when `String#equalsIgnoreCase` does, and so exactly
when `TagCommand` refuses the second of them. Folding case by lowercasing
would answer a different question and split a group `tag` has already joined:
a Greek capital sigma lowercases to a medial sigma, while the final sigma a
student may carry the same tag under lowercases to itself.

### Logic component

**API** : [`Logic.java`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<puml src="diagrams/LogicClassDiagram.puml" width="550"/>

The sequence diagram below illustrates the interactions within the `Logic` component, taking `execute("delete 1")` API call as an example.

<puml src="diagrams/DeleteSequenceDiagram.puml" alt="Interactions Inside the Logic Component for the `delete 1` Command" />

<box type="info" seamless>

**Note:** The lifeline for `DeleteCommandParser` should end at the destroy marker (X), but due to a limitation of PlantUML, the lifeline continues till the end of diagram.
</box>


How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to a `StudentBookParser` object, which in turn creates a parser that matches the command (e.g., `DeleteCommandParser`) and uses it to parse the command.
1. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `DeleteCommand`) which is executed by the `LogicManager`.
1. The command can communicate with the `Model` when it is executed (e.g. to delete a student).<br>
   Note that although this is shown as a single step in the diagram above for simplicity, the code can require several interactions between the command object and the `Model` to complete the operation.
1. The result of the command execution is encapsulated as a `CommandResult` object which is returned from `Logic`.

`LogicManager` compares a snapshot of the full student book before execution
with the records afterwards and saves only if they differ. Filter changes and
commands such as `list`, `find`, `help`, and `exit` therefore need no write
permission. Confirming `clear` still saves the emptied book. If a save fails,
`LogicManager` restores the snapshot and active filter before reporting the
error, so a failed command does not remain applied in memory.

Here are the other classes in `Logic` (omitted from the class diagram above) that are used for parsing a command entered by the User:

<puml src="diagrams/ParserClasses.puml" width="600"/>

How the parsing works:
* When called upon to parse a command entered by the User, the `StudentBookParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddCommandParser`). The parser uses the other classes shown above to parse the command and create an `XYZCommand` object (e.g., `AddCommand`). The `StudentBookParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddCommandParser` and `DeleteCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.
* A command's arguments are read in two passes: `CommandTokenizer` splits them into tokens, then `FlagTokenizer` sorts those tokens into the `Flag` each belongs to and hands back a `FlagArgumentMap`. [How commands read their arguments](#how-commands-read-their-arguments) covers why.

### Model component
**API** : [`Model.java`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/model/Model.java)

<puml src="diagrams/ModelClassDiagram.puml" width="450" />


The `Model` component,

* stores the student book's data i.e., all `Student` objects (which are contained in a `UniqueStudentList` object).
* treats two records as the same student only when both hold the same NUS Student ID. Names are not identity keys,
  and records without an NUS Student ID remain distinct.
* stores the `Student` objects selected by the current filter, such as search results, in a separate _filtered_ list. It exposes this list as an unmodifiable `ObservableList<Student>` that the UI can observe and bind to, so the UI updates when the list changes.
* stores a `UserPrefs` object that represents the User’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)


<box type="info" seamless>

**Note:** The alternative, arguably more object-oriented, design below keeps a unique list of tags in `StudentBook`, and each `Student` references tags from that list. This lets `StudentBook` maintain one `Tag` object per unique tag instead of each `Student` holding its own `Tag` objects.<br>

<puml src="diagrams/BetterModelClassDiagram.puml" width="450" />
</box>


### Storage component

**API** : [`Storage.java`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/storage/Storage.java)

<puml src="diagrams/StorageClassDiagram.puml" width="550" />

The `Storage` component,
* can save both the student book's data and the User's preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonStudentBookStorage` and `JsonUserPrefsStorage` (one class per data file).
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`)

`JsonUtil.readJsonFile` reports a file as missing only when its absence is
established, with `Files.notExists`. `Files.exists` answers false both for a
file that is not there and for one whose folder cannot be opened, and
`MainApp.initModelManager` reads a missing file as a first run, so a folder
TAB merely cannot open would open the window on sample students in place of
records it was never able to read. A file whose existence cannot be determined
is therefore read, and the access failure reported like any other.

The records themselves are already safe from that mistake. The baseline
`JsonStudentBookStorage` records for an unreadable folder is `Optional.empty()`,
so once access is restored `saveIfUnchanged` finds a book on disk that the
window never loaded and refuses the save. What is left without this check is a
window opened on sample data that then refuses every save as stale, telling a
User who changed nothing to close it and restart. Reporting the access failure
at startup replaces both with the reason.

`JsonUtil.readJsonFile` rejects a whole-file `null` with a
`DataLoadingException`, so both preference and student data loading use their
existing error handling instead of crashing. Invalid student data is preserved
for repair. `JsonUserPrefsStorage` also rejects an explicit `null` for
`guiSettings` with a `DataLoadingException`; a missing setting retains the
`UserPrefs` defaults.
`MainApp.initPrefs` handles invalid preferences by restoring and saving
defaults before constructing the model. Student data is stored separately
and is unchanged by preference recovery.
Recoverable preference writes in `MainApp.initPrefs` and `MainApp.stop` log
one `WARNING` naming the preferences path and I/O reason, without a stack
trace. Startup retains the loaded or default preferences for the session;
shutdown explains that window preferences were not saved. Permission errors
without a reason are described as permission denied.

`FileUtil.writeToFile` protects an existing JSON file from a partial save. It
resolves an existing symbolic link to its destination, so replacing the saved
data leaves the link in place. An unresolvable link is rejected without
replacing it or creating its missing target. It also refuses a destination that is not
writable and copies an existing file's POSIX permissions to its replacement.
The new contents are written and forced to a temporary file beside the real
destination before an atomic move replaces it. Keeping both files in one
folder makes the atomic move possible, but also means that the folder must
allow temporary-file creation. If writing or moving fails, the previous file
remains unchanged and TAB removes the temporary file where possible.
`FileUtil` creates only missing parent directories before writing the temporary
file; it never pre-creates an empty destination. This applies to student-book
and preference writes. A failed first save preserves the absence of a book and
allows normal sample-data startup. On POSIX filesystems, a new file retains the
temporary file's owner-only permissions; existing files retain their permissions.

`StorageManager` logs read/write attempts and successful saves at `FINE`,
including preference-file operations. Entries identify the file path without
copying student or preference contents. Save completion is logged only after
the storage delegate returns successfully.

`JsonAdaptedStudent` and `JsonSerializableStudentBook` use Jackson's
`@JsonAnySetter` callbacks to reject unknown keys while the data is being read.
The student callback includes the record name and allowed field names in its
error, and permits the removed `address` key as a compatibility exception.
`JsonStudentBookStorage` adds the one-based student record number from Jackson's
mapping path, so even unnamed records or students sharing a name can be located.
The book also accepts the existing `_comment` metadata key.
This prevents hand-edited values from disappearing during a later unrelated
save. The shared JSON mapper keeps its existing behavior for preferences;
startup follows the existing loading-error path and leaves rejected data intact.

### Common classes

Classes used by multiple components are in the `seedu.tab.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Protecting saves from stale windows

`JsonStudentBookStorage` retains a separate copy of the student book loaded or
successfully saved at each path. Before saving, it rereads the current records
and compares them with that snapshot. A difference, a removed file, or an
unreadable replacement refuses the save; `LogicManager` then restores records
and the active filter. The snapshot advances only after a successful save.
Comparison uses parsed student records, so harmless JSON formatting changes do
not create a conflict. Each normal application startup reads the file before
issuing commands; callers saving without a prior read adopt the on-disk book
as their initial baseline.

The comparison and replacement share an operating-system lock on
`.tab.json.lock` beside the real destination. Resolving symbolic links makes
windows reaching the same destination share a lock. A busy lock refuses the
save with retry guidance. The sidecar remains after closing TAB: deleting it
could allow different processes to lock different files with the same name.
Closing its channel releases the lock, including on a failed save or process
exit. This prevents stale TAB windows from overwriting each other's records;
it does not add concurrent multi-user editing. External editors do not honor
this lock and must be used with all TAB windows closed.

### Rolling back model changes when saving fails

`LogicManager` takes a copy of the student book and its active display filter
before executing a command. It compares the resulting student book with that
copy and writes to storage only when student records changed. Commands such as
`list`, `find`, `help`, and `exit` can therefore run even when the data file is
not writable.

When a write fails, `LogicManager` restores both the student-book copy and the
previous filter before reporting the storage error. Restoring the filter as
well as the records matters because `add` normally resets the display to show
every student. The failed command consequently has no visible or
in-memory effect, and retrying it cannot apply the same change twice. A
confirmed `clear` follows the same save-and-restore path; its pending
confirmation is still removed before the save attempt so that a failure does
not leave a stale reply active.

### Resolving application file locations

`AppUtil#getApplicationDirectory` uses the code-source location to distinguish a
packaged JAR from an exploded Gradle or IDE build. A packaged application stores
`data/tab.json`, `preferences.json`, and `tab.log.*` beside the JAR regardless of
the process's working directory. An exploded build keeps the working directory
as its application directory, preserving the development workflow.

### How commands read their arguments

`add` and `edit` mark their fields with options,
`add NAME -p PHONE [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up]` and
`edit INDEX [-n NAME] [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up | --no-follow-up]`,
where AB3 marked them with prefixes such as `p/`.
The value options have descriptive long aliases: `--name`, `--phone`,
`--email`, and `--tag` correspond to `-n`, `-p`, `-e`, and `-t` respectively.
`add` takes its name in the preamble, so `--name` applies only to `edit`.

A prefix has to be a sequence a value never contains, and no such sequence
exists for a name. `s/o`, `d/o`, `a/l` and `a/p` are ordinary parts of
Singaporean and Malaysian names, and `a/l` was in fact unreadable for as long
as an address field was marked `a/`. Widening the prefixes would only move the
collision somewhere else. Options move the problem out of the value entirely:
an option is a whole token, so a name is read as a name however it is spelled.

Parsing runs in two passes, the way a shell does it.

`CommandTokenizer` splits the arguments on whitespace and lets double quotes
group a value containing whitespace. A token records whether a quote took part in it,
because that is the only thing separating the value `-Ahmad` from the option
`-e`. A backslash escapes a quote or another backslash, so a value may hold
either; a backslash before anything else stays in the value, so nothing the User
typed disappears without being asked for.

An option appearing where a value is expected is read as the option before it
having been left empty, rather than as a value. `add John -p 123 -t -e` reports
that `-t` needs a value instead of storing `-e` as a tag. A value that really
does open with a hyphen is given in quotes.

`FlagTokenizer` reads the tokens. Those before the first option are the
*preamble*, joined with single ASCII spaces, which each command reads as
whatever it takes in that position: the name for `add`, so a name of several
words needs no quotes unless it would otherwise be misread, and the index for
`edit`. Each command names its own, in `AddCommand.FIELD_NAME` and
`EditCommand.FIELD_INDEX`, rather than the shared `CliFlags` doing it for
them. Each short and long marker resolves to the same `Flag`, so mixed aliases
for a single-valued field are duplicates rather than independent fields.
Value-taking options take the token after them, and `-t` or `--tag` repeats
for `add` and `edit`. The `tag` command takes exactly one tag through `-a`/`--add`
for addition or `-r`/`--remove` for removal.
They also accept a value joined to any short or long marker by an equals sign.
`FlagTokenizer` splits only on the first equals sign, so later ones remain in
the value. An attached value is unambiguously part of its option when it opens
with a hyphen. An empty attached value must be quoted as in `--tag=""`, so an
accidental `--tag=` cannot clear data. `CommandTokenizer` records whether a
token began with a quote so a fully quoted `"--tag=T1"` remains a value, while
the quote around the value in `--tag="Lab 3"` does not hide the option marker.

For `edit`, tag values use `StringUtil.normalizeFieldValue` before validation.
A single normalized blank value clears all tags, including when the input holds
Unicode spaces. A blank beside other tag values is rejected with an explanation
that clearing requires the blank to be given on its own. Nonblank values are
still validated so every problem can be reported without silently discarding tags.

The presence-only `--follow-up` option (alias `-f`) and edit-only
`--no-follow-up` option (alias `-nf`) instead record an empty value and consume
no following token. This lets
`FlagArgumentMap` distinguish an
absent option from a present marker without a separate representation. An
attached value on a presence-only option is refused specifically. An unknown
option names its marker without echoing an attached value, while a value-taking
option left without a value and a token belonging to no option are each
reported for what they are, rather than as one generic complaint.

`add` keeps its name before the options so names containing spaces need no
quotes. If the tokenizer refuses `-n` or `--name`, `AddCommandParser` adds a
placement hint to the existing error. A missing name gets the same hint,
so users familiar with the name options in `edit` can recover immediately.

`Flag` also lets a value-taking option carry aliases. NUS Student ID uses
`--id` as its canonical option and the unused `-i` as its typing-friendly
alias. Both spellings map to the same `FLAG_NUS_STUDENT_ID`, so giving
both is rejected as a repeated single-valued field rather than letting one
silently override the other. Usage labels and error messages use the canonical
`--id NUS_STUDENT_ID` spelling.

Duplicate-option feedback names repeated option markers rather than values,
because presence-only options such as `--follow-up` do not take values.
`FlagArgumentMap` retains the first spelling supplied for each repeated option;
the shared message lists those options and asks for each listed option only once.

The shape of the command is settled before any field is read. An unclosed
quote, an unknown option, or a single-valued option given twice, including
once under each alias, all leave it
unclear which field a value belongs to, so each is refused on its own instead
of being collected with the field errors.

`edit` clears a field with an explicitly empty value rather than with a bare
option. `-e ""` removes the email, `--id ""` removes the NUS Student ID, and
`-t ""` removes every tag. Email and NUS Student ID values are trimmed before
this check, so quoted whitespace clears either optional field too.
A bare option cannot mean "clear" here, because a bare option
is what reports the mistake in `edit 1 -t -e a@b.com`: the `-e` is read as the
value `-t` was left without, rather than being stored as a tag. An empty quoted
value also separates a value left empty by accident from one cleared on
purpose. `CommandTokenizer` produces an empty token for `""` and leaves the
decision to the field parser, so no tokenizer change was needed.

`edit` sets follow-up with `--follow-up` (alias `-f`) and clears it with
`--no-follow-up` (alias `-nf`). The parser rejects contradictory options before describing
any edit, so neither the model nor the saved book changes on that error.
The `-nf` alias makes clearing quick to type without making opposite actions
differ only in case, as `-f` and `-F` would. Both clear spellings map to the
same option, so mixing them is also detected as a duplicate.

### Adding or removing one tag

`TagCommandParser` reads `tag TARGET -a TAG` (alias `--add`) or
`tag TARGET -r TAG` (alias `--remove`) with the existing
`FlagTokenizer` and `ParserUtil`. The option names the action: both spellings
map to `FLAG_ADD_TAG` or `FLAG_REMOVE_TAG`, respectively. Exactly one action
option is accepted; conflicting actions and repeated options, including mixed
aliases, are rejected. Missing fields and invalid field values are collected
before a command is created.
`TagCommandParser` retains `ParserUtil.parseRequiredIndex` for numeric targets
and shares `parseRequiredStudentTarget` with edit for letter-leading IDs.
`--id`/`-i` explicitly selects any valid ID, including one starting with a
digit. Mixing the explicit ID option with a preamble or repeating it is
rejected rather than guessing which student to change. `ParseProblems`
collects target and tag validation errors before constructing the command.
Tag and edit both use `FlagTokenizer.tokenizeWithIndex`, which reports an
unquoted negative
number as both an invalid index and an unknown option. A value following a valid
index is reported separately by `parseRequiredIndex`. Tag syntax failures and
a missing action option include guidance for both addition and removal.

`TagCommand` stores a `StudentTarget`, as edit and delete do. It resolves an
index against the displayed list and an ID across the entire book, so a
roster ID remains usable while a search hides the student. It compares the
normalized tag names ignoring case before copying the existing tag set and
constructing a replacement student with every other field preserved. The
search filter stays active; removing a tag used by that filter may hide the
student from the displayed list. `TagCommand.Action` selects addition or
removal while sharing target resolution, matching, student replacement and
feedback.
Tag equality and storage remain unchanged. Addition preserves existing case
variants; removal removes every matching variant of the requested group while
retaining unrelated tags. A general tag named `Needs follow-up` remains separate
from the follow-up flag. Duplicate and successful removal feedback use a matching
stored tag's spelling. If several case variants match, the reply chooses the
first in the card's tag display order.

For a hidden flagged student, the absent-tag hint asks the User to run `list`
and use that student's displayed index to clear follow-up. It never suggests
an unavailable index or an edit target that could identify someone else.

Invalid indexes or absent IDs, existing tags on addition and absent tags
on removal throw `CommandException` before mutation; invalid syntax or values throw
`ParseException`. These paths never reach
`LogicManager`'s save. Successful additions and removals use the normal JSON
save path without changing the schema. Integration tests exercise reloads,
human-edited case and whitespace variants, absent optional fields and unchanged
saved bytes on failure.

### Commands with no arguments

`StudentBookParser` rejects non-whitespace input after `clear`, `list`,
`help`, and `exit` before returning a command. Its existing Unicode
whitespace rule accepts pasted whitespace as well as spaces and tabs.
The feedback names the command and unexpected input, with long input
shortened to keep the reason visible. Quotes do not make an argument absent:
`clear ""` is refused too. A parse failure reaches neither command execution
nor `LogicManager`'s storage write, so it preserves both the displayed list
and the JSON file.

### Invisible characters on the command line

Before the command word is read, `StudentBookParser` strips the same Unicode
format characters that `StringUtil.normalizeFieldValue` strips from a stored
value, through `StringUtil.stripFormatCharacters`. A direction mark or a
zero-width space pasted before a command word or an index is otherwise a word
that looks right but matches no command, or an index reported as not a positive
integer. The zero-width joiner and non-joiner are kept, because a script such as
Sinhala needs them to shape a word, and a command word holding one is a
different word rather than the same word with a character removed.

### Reading a command word in any case

`StudentBookParser` folds the first word of the input to lower case before it
dispatches, so `List`, `LIST` and `list` all reach `ListCommand`. No two
command words differ only in case, so a capitalised word can only mean the one
command, and refusing it over a Caps Lock left on or a Shift held a moment too
long costs the User a retyping for nothing.

The fold is `CommandWords.foldCase`, which `CommandWords.suggestFor` applies to
the word it measures as well. Dispatch and suggestion therefore agree by
construction, where a second fold written into the parser could come to differ
from this one, leaving a word that is refused and then suggested back to the
User unchanged. The fold is `Locale.ROOT`'s rather than the User's, whose
Turkish rules would turn `LIST` into a word with a dotless `ı` that answers to
nothing.

The first word alone is folded. The arguments reach the command parsers as
typed, so `Find -n Alex` searches for `Alex` with its capital and `Add John Doe`
stores the name as it was written. Option spellings stay case-sensitive, as the
User Guide documents them: an option is read among the values rather than in
front of them, so folding there would have to leave the values it sits beside
untouched, which is a decision of its own. The typed word is kept beside
the folded one, because an unknown word is quoted back in the case it was
typed in, while a command refusing arguments names the command: `LIST oops`
reports that `list` takes no arguments.

### Suggesting a command word

A command word TAB does not dispatch on is answered with the word the User
typed and the command words it is a likely attempt at. `CommandWords` works
out which those are in two steps: what the typed word opens, and, failing
that, what it is near.

A word that opens a command word is read as that command word being typed.
`del` stands for `delete`, although `help` is the nearer of the two by edits
alone, at two against three. A part-typed word is right as far as it goes,
which says more about what was meant than the distance to some other word
does. Only a word that opens nothing is measured for edits at all.

The measure is `StringUtil.editDistance`: how many insertions, deletions,
substitutions and swaps of neighbouring characters turn one word into the
other. A swap counts as one edit because a fast typist makes them as readily
as the other three, and `lsit` would otherwise sit two edits from `list`, as
far away as a wilder guess.

Two edits is the budget for every command word, whatever its length. That is
what the user story asks for, and it is what reaches the words a teaching
assistant types: `ls` opens nothing and is two insertions short of `list`,
and `dlte` is two deletions from `delete`.

Two edits also reach past the first letter of a short command word, which is
how `dlt` came to be answered with `edit`. A word measured for edits
therefore has to open with the character the command word opens with, unless
the single edit between the two is that character. The first letter is the
one a typist is least likely to get wrong, so a word two edits out that
disagrees on it as well has little to do with the command it happens to sit
near; where one edit is the whole difference, that edit may fall on the first
letter and the rest of the word already agrees, which keeps `felete`,
`edlete` and `elete` reading as `delete`.

Every command word equally likely is named. `eit` is a letter short of both
`edit` and `exit`, and answering with `edit` alone would read as knowledge TAB
does not have, with the other ruled out. Choosing between equally likely words
dresses a guess up as help, while naming them all says exactly what is known.
They are named in the alphabetical order of `COMMAND_WORDS`, so one typo
always draws one reply.

Past three equally likely words the reply falls back to the pointer to `help`,
because a list that long says only that TAB cannot tell. No typed word reaches
that with the command words TAB has: `edit` and `exit` are the only two
sharing a first letter, so at most two are ever named at once, and the limit
is there for the command words it may gain.

The comparison folds case with the `CommandWords.foldCase` the parser
dispatches with, so `Delte` is read as an attempt at `delete`; a word spelt
correctly in any case is dispatched before it reaches here. The message quotes
the word as it was typed, because a User who typed capitals needs to see them
to recognise what they did. The quoted word is shortened the
way a rejected field value is, so that a pasted line cannot push the rest of
the message out of sight. Lengths are compared before any distance is, which
also settles such a line against every command word without measuring it.

A suggestion is never run. Dispatching `delete` for `dlte` would act on a word
the User did not type, and a wrong guess there costs a student record, where a
wrong suggestion costs a line of text. `StudentBookParser` raises the same
`ParseException` as it does for any unknown word, which leaves the model, the
saved file and the typed text in the command box as they were.

The parser dispatches with a `switch` rather than from
`CommandWords.COMMAND_WORDS`, so the two can disagree: a word missing from the
list would work when typed while being suggested to nobody. `CommandWordsTest`
resolves the command words out of the parser's own cases and compares them
against the list rather than leaving that to be noticed.

### Follow-up flag

`Flag` extends `Tag` and uses its validation, normalization, and display
behavior. Its fixed name is `Needs follow-up`. A general tag with those words
remains a distinct value: tag equality requires the same concrete class as
well as the same normalized name. `Student` holds an optional `Flag` and
exposes it through `getFlag()`; `isFlagged()` preserves the existing boolean
interface for commands and the UI. Full equality and hashing include the flag,
but `isSameStudent` does not: changing whether a student needs a reply does not
change their identity. The flag is stored as `Optional<Flag>`; the constructor
without a follow-up argument defaults to `Optional.empty()`.

Tag searches match ordinary tags as parts of their inherited `tagName` values,
ignoring case. Follow-up status is selected independently through `-f` or `-nf`, so its
displayed label never produces a text-field match.
The student card and command feedback display the same value, while the card
keeps the flag at the trailing edge of its headline.

The add parser maps the presence of `--follow-up`, or its `-f` alias, to true.
For edit, the `EditStudentDescriptor` stores a nullable `Boolean isFlagged`:
null preserves the existing state, true sets follow-up, and false clears it.
`getFlagged()` exposes this as an `Optional<Boolean>`. `EditCommand` uses the
requested value when present, otherwise the selected student's existing state.
This makes either request safe to repeat while preserving follow-up during
unrelated edits. Descriptor copies and equality retain all three states.

`JsonAdaptedStudent` uses a nullable `Boolean flag` at the storage boundary.
Only `Boolean.TRUE` creates a `Flag` in the model; a missing key, JSON `null`, and
`false` all map to no flag. When saving, a present flag is written
as `"flag": true`, while an absent flag is represented as null and omitted by
Jackson's `NON_NULL` policy. Files created before the field existed therefore
load without migration or gratuitous rewrites.
`JsonFollowUpFlagDeserializer` accepts only JSON Boolean tokens, with Jackson
retaining `null` for a missing or null key. Rejecting other token types before
Jackson can coerce them prevents numeric hand edits from silently changing
follow-up status. The normal loading-error path identifies the record and
`flag`, blocks startup, and preserves the file.

`StudentCard` shows a `Needs follow-up` label for true. For false it makes the
label both invisible and unmanaged, so the card keeps no blank placeholder
row. Successful add, edit and delete feedback formats student records
consistently with the student card: optional fields (email, tags, follow-up
status) appear only when present, with tags ordered alphabetically ignoring
case, avoiding placeholder labels or empty tag markers.

### Field values

`Student` stores email and NUS Student ID as `Optional<Email>` and
`Optional<NusStudentId>`. Both constructors require non-null arguments;
`Optional.empty()` explicitly represents an unknown detail. One constructor
accepts the follow-up boolean, and the other defaults to unflagged for sample
data. There are no overloads accepting nullable email or ID values, and no
`Student` field is null. Add, edit, storage conversion, and test builders pass
optionals directly into this contract. Nullable JSON strings remain confined
to the storage adapter, preserving the existing file format.

`Name`, `Tag`, `Phone`, `Email` and `NusStudentId` each store the form returned by
`StringUtil.normalizeFieldValue`: without invisible format characters such as
direction marks, with each run of whitespace reduced to one ASCII space
and leading and trailing whitespace removed, and with the result composed to
Unicode NFC. Composing happens last, because an invisible character between a
base letter and its combining mark blocks the two from composing.

They normalize for different reasons, and the rule each one enforces differs.

| Field | Why it is normalized | What it must hold |
| --- | --- | --- |
| `Name` | it is displayed and searched as text, so visually identical names should have the same stored form | at least one letter or number |
| `Tag` | it is a key in the set of a student's tags, so two that look alike must not both be stored | at least one letter or number |
| `Phone` | it is displayed beside the others and gains nothing from being stored differently | at least 3 digits |
| `Email` | it is displayed and searched as text, so visually identical addresses should have the same stored form | RFC 5321 address format, max 254 chars |
| `NusStudentId` | it is the sole identity key, so visually identical IDs must have the same stored form | 1-32 ASCII letters/digits, stored uppercase |

**The NUS Student ID may be absent.** `NusStudentId` accepts between 1 and 32
ASCII letters and digits. Input letters are case-insensitive and the value is
stored in uppercase, so `e0123456` and `E0123456` are the same value. This
accommodates variations in the identifiers issued to different groups of
students while preventing an accidentally pasted value from hiding the other
details on a student's card. `Student` stores and exposes the value as
`Optional<NusStudentId>`, using an empty optional rather than a sentinel value.
`NusStudentId.getFailureReason` performs the single validation scan, and
`isValidNusStudentId` delegates to it. This keeps the accepted format and the
specific user-facing error synchronized without maintaining a separate regex.

The add and edit parsers read either `--id` or `-i` through the same flag
and validate with `ParserUtil.parseNusStudentId`. Add leaves the field absent when no
option is given. For edit, `EditStudentDescriptor.OptionalFieldEdit` encapsulates
the value and edit intent as one three-state concept: an absent option preserves
the old value, a present non-empty option replaces it, and `--id ""` clears it.
The edit parser trims the supplied value before this check, so a quoted value
containing only spaces also clears the field.
Descriptor copies and equality operate on this encapsulated state, rather than
repeating knowledge of its internal representation.

`JsonAdaptedStudent` represents the storage property as a nullable string.
Jackson omits it on save when no ID is present; both a missing `nusStudentId` key and
JSON `null` load as absent, preserving files written before the field existed.
A malformed non-null value rejects the data file instead of being silently
dropped. The student card and command feedback show a present ID and omit the
label entirely when absent.

NUS Student ID is the sole **identity key**. `Student.isSameStudent` returns
true only when both records hold equal NUS Student IDs. Because construction
normalizes letters to uppercase, this comparison is case-insensitive. A shared
name does not establish identity, and neither does a shared phone number or
email. Records without an NUS Student ID therefore remain distinct even when
their other details match. `UniqueStudentList` applies this rule to add, edit,
and model replacement, so command execution and storage loading enforce the
same contract.

When `add` or `edit` finds a matching NUS Student ID, it rejects the operation
without mutation and names the normalized ID and student who already holds it.
During storage loading, `JsonSerializableStudentBook` admits repeated names but
rejects a repeated NUS Student ID with both record numbers, both names, and the
ID. This keeps hand-edited files diagnosable while allowing common names in a
class.

The activity diagram below follows `AddCommand.execute` after parsing. The
identity check scans the complete student book. A matching NUS Student ID stops
the command before it changes the model; other matching details can warn the
User while still allowing the record to be added.

<puml src="diagrams/AddDuplicateCheckActivityDiagram.puml" alt="Add command duplicate check" width="500" />

`ModelManager.addStudent` preserves the active filter. After a successful
addition, `AddCommand` checks the new student against that filter. A matching
student appears at the end of the results without changing earlier indexes.
For a non-match, the command clears the filter and appends
`Search cleared to show the new student.` to its feedback so the student is
visible and the User knows to re-read indexes. A rejected addition leaves
both the book and filter unchanged.

After the identity check succeeds, `LikelyDuplicateWarning` compares the
prospective record with every existing student. Name and email comparisons
ignore case; names already carry `normalizeFieldValue`'s whitespace
normalization. Phone search and warnings share `Phone.normalizeDigits`, which
removes non-digit characters and converts Unicode decimal digits to ASCII.
Warnings require equality of the complete digit sequence; search permits
substrings. Country codes and extension digits remain significant, while the
stored number retains its formatting. Absent emails do not match.

The commands collect the warnings before mutation and separate them from the
ordinary success message with a blank line. Matches are grouped by field into
one prominently labelled `WARNING: Possible duplicate student` line. A line
names at most two students and summarizes further matches as `N others`, so a
shared placeholder does not fill the result box. The review advice appears
once after every matching field.

`EditCommand` compares only name, phone, or email values that differ from the
selected record. It excludes the selected `Student` instance but still uses
the complete unfiltered student book, so a filtered list cannot hide a match
and unrelated tag or follow-up edits do not repeat an acknowledged warning.
Name comparisons share `StringUtil.normalizeNameForComparison` with name
searches: Latin accents and straight/curly apostrophe differences are ignored,
so copied roster names can be found or flagged using ordinary keyboard input.

These comparisons are advisory only: they never feed `isSameStudent` or the
storage duplicate check, and therefore cannot reject or discard a legitimate
shared detail.

**The email may be absent.** A student is recorded before an email address is
known rather than one being invented. `Student` accepts, stores, and exposes
`Optional<Email>` so callers handle absence explicitly. `JsonAdaptedStudent`
writes the email as a string when present; the optional wrapper is not written
to JSON.

The data file normally carries no `email` key. A missing key, JSON `null`, and
a string that is empty after trimming all represent an absent email. A
non-empty string is trimmed before validation, matching command input; a
malformed address is still refused rather than being quietly dropped.

`edit INDEX -e ""` clears an email without removing the student or changing
other fields. `EditStudentDescriptor` stores an `Optional<Email>` and records
`isEmailEdited` separately: false leaves the existing value alone, true with
an empty optional clears it, and true with an address replaces it. `setEmail`
requires an address and `clearEmail` explicitly requests removal. Both mark
the email as edited, so callers cannot toggle the intent independently of the
replacement value. Copies use those same methods to preserve all three states.
The descriptor is not persisted, so its optional field does not affect JSON.

The parser leaves the email untouched when `-e` is absent, calls `clearEmail`
for a token that is empty after trimming, and sets a replacement only after
validation succeeds. A rejected value is recorded as a parse problem without
assigning a null email or requesting a clear. A bare option is still an error.
Clearing counts as an edit even when the email is already absent. `EditCommand`
passes the optional value directly into the `Student` constructor, including
an empty optional when clearing. The saved record omits the `email` key and
reloads through the existing optional-email storage contract.

`Email` accepts the RFC 5322 unquoted dot-atom local part so that legitimate
student addresses with apostrophes, repeated symbols or trailing underscores
can be recorded. Only dots require non-empty groups on both sides; other
permitted symbols can repeat and appear at either end. Commands and saved
records share this validator and preserve the address's spelling. Quoted
local parts and domain literals remain unsupported; domain validation is
unchanged.

`Email.MAX_LENGTH` caps at 254 characters the address a **command** may enter.
`ParserUtil.parseEmail` checks `Email.isWithinLengthLimit` before
`Email.isValidEmail`, and `getFailureReason` carries the same guard because it
matches the local part on its own.

The limit is not there to simplify validation, which the guidance on arbitrary
limits rules out. `VALIDATION_REGEX` nests one quantifier inside another,
a repeated dot followed by a non-empty local-part atom, and Java matches a
repeated group by recursion, one frame
per repetition, so an address with enough `.`-separated groups exhausts the
stack. `StackOverflowError` is an `Error` that no parser catches, and
`CommandBox` catches only `CommandException` and `ParseException`, so the User
saw no feedback at all. 254 is the limit RFC 5321 section 4.5.3.1 puts on a
forward path, so the bound comes from the domain rather than from the parser.
It is not the product's only bound: a `NusStudentId` holds at most 32
characters, while `Name`, `Phone` and `Tag` are checked for the content they
hold rather than for their length, and take a value of any size. What is
particular to this bound is its purpose, which is to keep `VALIDATION_REGEX`
from walking an address long enough to exhaust the stack, and 254 is the
longest measured to be safe on every stack size tried: on a 256 KB stack even
a 513-character address overflows.

`isValidEmail` deliberately keeps **no** length limit, so each input boundary
can report length and format failures separately. Both `ParserUtil` and
`JsonAdaptedStudent` check `Email.MAX_LENGTH` before calling the matcher. A
saved address of up to 254 characters therefore loads, while a longer one
raises `IllegalValueException` with its actual and allowed lengths before its
shape can exhaust the stack. The storage tests cover both sides of that
boundary.

`Messages.getErrorMessageForInvalidValue` shortens a rejected value past
`MAX_QUOTED_VALUE_LENGTH` characters, so that a very long value cannot push the
reason out of view. It counts characters through `StringUtil.firstCharacters`,
as a reader counts them, so it separates neither an emoji nor a letter from the
accent that belongs to it; a student card cuts a name or a tag with the same
helper. Command parsing and storage share that message, so both paths report
the same invalid value consistently.

`Phone` is **not** an identity field and **not** a key: two students may hold the
same number, and `isSameStudent` does not read it. It is normalized only so
that a number typed with unusual whitespace, or wrapped in the invisible marks a
chat app puts around a copied number, is stored consistently.

`FindCommandParser` uses `FlagTokenizer` and the existing `CliFlags` aliases for
name, NUS Student ID, phone, email and tag. Every search term must have a field
option. An option takes one word or one double-quoted phrase; apostrophes are
literal characters, so names such as `O'Brien` do not need quoting. Repeated
options retain all values. Bare terms receive `MESSAGE_UNQUALIFIED_TERM`, and
an extra bare word after an option receives the shared tokenizer's
unexpected-value diagnostic. Empty or whitespace-only values receive
`MESSAGE_EMPTY_KEYWORD`; unterminated double quotes report
`CommandTokenizer.MESSAGE_UNCLOSED_QUOTE`.

Search terms are normalized with `StringUtil.normalizeFieldValue`, collapsing
whitespace runs and applying Unicode NFC normalization. The parser creates a
`DetailsContainsKeywordsPredicate` with terms grouped by its `Field` enum.
Every term must match its selected field, using case-insensitive substrings:
AND applies both within and between fields. Each tag term must occur within
one tag, but separate terms may match the same tag or different tags. Absent
optional fields do not match. Explicit field selectors prevent unrelated
matches in other details and replace the earlier unqualified OR search.
Follow-up status is selected with the presence-only `-f`/`--follow-up` and
`-nf`/`--no-follow-up` options. The predicate holds an `Optional<Boolean>`:
empty imposes no status restriction, true requires follow-up, and false
requires its absence. A status-only search is valid, and combinations require
both the status and every field term. The parser rejects conflicting or
repeated status options before execution, matching the existing edit rules.

Case comparison folds each Unicode code point independently using
`Character.toLowerCase(Character.toUpperCase(...))`, avoiding context-sensitive
whole-word lowercasing of Greek final sigma. Tag searches therefore agree with
the case comparison used by `TagCommand`. Name comparison additionally applies
canonical decomposition and removes marks attached to Latin letters, then maps
curly apostrophes to straight ones. Marks in other scripts remain significant,
so vowels needed to distinguish names are retained. This is a comparison key,
not transliteration; model values and JSON retain their existing spelling.

For phone terms containing only Unicode decimal digits, or those digits with
grouping whitespace and phone punctuation (`+`, `-`, `(`, `)`, `.`), matching
also compares the digits with non-digit characters removed and each decimal digit
mapped to its ASCII equivalent by numeric value. This lets
`91234567`, `9123-4567` and `"9123 4567"` match `+65 9123 4567` or `91234567`,
so copying a grouped number does not require manual reformatting. A query with
a country code still requires those digits to occur in the stored phone.
The digit fallback requires at least one digit; annotations such as `"ext 21"`
and punctuation-only terms retain literal matching. Unicode whitespace follows
the same rule as field normalization. Normalizing both the phone digits and the
search digits lets ASCII, full-width and other Unicode decimal digits match each
other, including digits represented by supplementary code points. For example,
`"９１２３ ４５６７"` matches `91234567`. Stored student details and saved data do not change.

`FindCommand` and `ListCommand` report the count of displayed students in
their execution feedback. `FindCommand` counts only the matching students
(`Messages.getStudentsFoundOverview`), distinguishing zero (`Found 0 students.`),
singular (`Found 1 student.`), and plural (`Found <n> students.`) results.
`ListCommand` reports the number of students displayed after resetting the filter
(`Listed 0 students.`, `Listed 1 student.`, or `Listed <n> students.`).

Nothing else is rejected in any of the three. A phone number may hold `+`,
whitespace, brackets and an extension; a tag may hold whitespace, hyphens and any
script. None of those characters can hinder TAB, because none of these
fields is parsed, dialled, or used to build a file path.

### Handling an unreadable data file

Startup distinguishes a missing student-book file from an unreadable one. A
missing `data/tab.json` is a first launch and receives the sample student book.
An existing file is first parsed as JSON and then every student record and
field is converted to its model type. Both stages finish before the normal UI
can accept a command.

`JsonUtil` enables Jackson's strict duplicate-key detection and checks for
another token after reading the root value. A repeated key or trailing value
would otherwise silently lose data on the next save. Both are rejected through
the existing loading-error path, with the key or offending token's location;
whitespace after the document and keys reused in separate objects remain valid.

`FileUtil.readFromFile` decodes UTF-8 strictly and removes only an optional
leading BOM before JSON parsing. This accepts files saved by editors that add
a BOM while rejecting malformed byte sequences rather than silently replacing
letters with U+FFFD. `JsonUtil` translates decoding failures into an encoding
explanation and recovery guidance through the same `DataLoadingException`
boundary; startup preserves the original bytes and blocks the normal UI.

`JsonUtil` turns malformed JSON or an incompatible JSON shape into a
`DataLoadingException`. For a syntax error, it uses the deepest parser
exception with a usable location so that the displayed line and column
identify the offending token rather than the last successfully mapped
value. It pairs the concise parser explanation with the affected line
and at most one line on either side. Each source line is limited to 160
characters; an affected long line is cropped around the reported column
so that minified JSON remains useful without filling the dialog. If the
parser supplies no usable location, `JsonUtil` omits the source instead
of falling back to Jackson's source-bearing message.
During model conversion, `JsonAdaptedStudent` identifies the invalid field and
`JsonSerializableStudentBook` adds the one-based record number and its name
when available. `JsonStudentBookStorage` carries that explanation through the
same `DataLoadingException` boundary.

`MainApp` records a student-book loading error and creates an empty model only
as an internal placeholder so that application shutdown remains safe. It gives
the error to `UiManager`. Before constructing `MainWindow`, `UiManager` checks
for that error. If one is present, it shows an error dialog containing the
data-file path, the specific reason, and recovery guidance. Closing the dialog
closes TAB because the normal window was never opened. No command, including
`exit`, can therefore save the placeholder over the unreadable file.

Failing closed was chosen over displaying a partial or read-only student book.
It keeps recovery small and unambiguous: the User repairs or replaces the file
and restarts TAB. Loading readable records separately and reloading without a
restart remain separate future features.

#### Student names

A name is displayed and searched as text, but is not an identity field.
`Name` stores a normalized field value rather than the raw input so that two
names which look identical are displayed and searched consistently. Multiple
students may hold the same normalized name.

**Normalizing.** On construction a name is

* composed to Unicode form NFC, so that `Nguyễn` written as one code point and
  as `e` plus two combining marks become the same string
* stripped of Unicode format characters such as direction marks and zero-width
  spaces, apart from the joiners described below
* reduced to single ASCII spaces, with leading and trailing whitespace removed,
  so that a non-breaking space cannot hide a word from the search

The zero-width joiner and non-joiner are deliberately kept, because scripts
such as Sinhala and Arabic need them to shape correctly. Two names that differ
only by a joiner are therefore still two students.

**A name is valid** when something remains after normalizing: at least one
letter or number, judged by Unicode category rather than by ASCII.

| Accepted | Example |
| --- | --- |
| Slashes | `Ravi s/o Kumaran`, `Anita d/o Rajan` |
| Hyphens and apostrophes | `Siti Nur-Aisyah`, `Ma Ying-jeou`, `Sean O'Brien` |
| Other punctuation | `J. R. R. Tolkien`, `James&`, `-Ahmad` |
| Ligatures and Roman numerals | `X Æ A-Xii`, `X Æ A-Ⅻ` |
| Any script | Chinese, Japanese, Korean, Arabic, Tamil, Hebrew, Mongolian, Vietnamese |
| Characters outside the basic plane | `𠮷田`, Adlam, Cuneiform |
| Digits only | `12345` |

| Rejected | Why |
| --- | --- |
| Empty, or whitespace only | nothing remains after normalizing |
| Punctuation only, such as `---` | no letter or number |
| An emoji, or a zero-width space, on its own | no letter or number |

**A name that holds a command prefix parses.** AB3 marked its fields with the
prefixes `n/`, `p/`, `e/` and `t/`, so a name holding any of them after an
ASCII space was read as the start of another field. `a/l` and `a/p`, ordinary
components of a Malaysian name, failed this way until the address field was
removed and the `a/` prefix with it, and the rest failed until `add` and `edit`
took positional arguments and command options instead. `Name` itself never
needed changing.

**Showing a long name.** A card gives the name one line, shared with the tags
and follow-up marker. `StudentCard` supplies the complete name through
`UiText.showToFit`, and `CardName` measures it in the displayed font. Names
appear in full whenever their allocated width permits; there is no fixed
character limit. Widening the card restores text previously hidden by a cut.

When space runs out, `CardName` shortens from the middle through
`UiText.middleShortened`. It keeps both ends, retaining up to nine characters
at the end and using the remaining character budget for the opening.
`StringUtil.firstCharacters` and `StringUtil.lastCharacters` preserve complete
characters, including combining accents and emoji. Names can still coincide
when their distinguishing text is hidden, so a tooltip shows the complete
name whenever it is shortened and disappears when the name fits again.

The cut happens during layout, after `CardHeadline` knows the actual width.
Leaving middle shortening to JavaFX can split a surrogate pair: a character
such as `𠮷` may then be drawn as a broken glyph.

`DarkTheme.css` therefore tells `.card-name` to clip, so that a cut `CardName`
gets a pixel wrong shows a glyph against the edge rather than a broken one.
That belongs in the stylesheet rather than beside the cut: an author
stylesheet overrides what code sets on a styleable property, so a
`setTextOverrun` call on the label would be dropped without a word.

`CardName` asks for the room the whole name needs however little of it is
drawn. The row shares its width out from what the name asks for, so a name
that asked only for what it is drawn at would be handed less room on every
pass, and the name and the tags beside it would chase each other down.

### Confirming a clear

`ClearCommand.execute` returns a prompt containing the whole-book count,
including students hidden by the current filter. It does not mutate the
model. An empty book instead returns a no-op message without a prompt.
`LogicManager.requestClearConfirmation` keeps a non-empty book's request in
`pendingClearCommand`, an `Optional<ClearCommand>`, and returns before the
usual storage write. `Optional.empty()` represents no pending request.
While the request is pending, it reads the
next input as a reply to the concise `[y/N]` prompt. It first applies
`StringUtil.stripFormatCharacters`, sharing the command parser's treatment of
copied invisible marks while preserving shaping joiners. It then trims only
surrounding Unicode whitespace; internal spaces and visible characters are
kept so extra words cannot become a confirmation. Replies ignore case.
Trimming scans from both ends with `StringUtil.isWhitespace`, retaining the
command parser's Unicode whitespace rules. This takes linear time at worst;
an internal whitespace run is never searched for a trailing match.

An empty reply, `n`, or `no` drops the request without executing or saving
anything. Only the exact replies `y` and `yes` invoke
`ClearCommand.executeConfirmed` and save the empty book. Words merely starting
with either letter, such as `yellow` or `nope`, are invalid. Other input raises
a `ParseException` with feedback and the prompt repeated and keeps the request
pending; it is never executed as another command.
If the reply handler unexpectedly has no request, it raises a
`CommandException` asking the user to enter `clear`, rather than letting an
unchecked exception escape. The normal presence guard prevents this case.
The pending request is cleared before saving so that a storage error cannot
leave a stale confirmation active. It lives only in memory, so closing and
restarting TAB cannot confirm a previous request. The existing result area
and command field provide the prompt and replies without a mouse or dialog.
`MainWindow` supplies `Logic.isAwaitingConfirmation` to `CommandBox`, which
submits an empty Enter only during confirmation; otherwise an empty Enter
keeps its existing no-op behavior. Each `Logic` implementation must supply
this state explicitly so a default cannot silently suppress cancellation.

### \[Proposed\] Data archiving

This feature is proposed but not yet designed. No implementation plan is specified.


--------------------------------------------------------------------------------------------------------------------

## **Documentation, logging, testing, dev-ops**

* [Documentation guide](Documentation.md)
* [Testing guide](Testing.md)
* [Logging guide](Logging.md)
* [DevOps guide](DevOps.md)

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Requirements**

### Product scope

**Target user profile**:

An NUS School of Computing teaching assistant tutoring 2 to 3 tutorial or lab
slots per semester. They are usually fast typists who prefer a CLI.

* has a need to manage 150 to 250 student records, sliced by slot, course,
  project team, and follow-up status
* prefers desktop apps to web or mobile apps
* can type fast, and prefers typing to mouse interactions
* is comfortable with CLI apps and with hand-editing plain-text data files
* is expected to keep student contact data off third-party cloud services

**Value proposition**: Manage a semester of students faster than a spreadsheet
or a mouse-driven contact app. They can find anyone by any field, slice the list
by tag, and see who is still awaiting a reply. Records stay fully offline in a
local plain-text file.


### User stories

Priorities: High (must have) - `* * *`, Medium (nice to have) - `* *`, Low (unlikely to have) - `*`

| Priority | As a ...  | I want to ...  | So that I can... |
|----------|---------|--------------|----------------|
| `* * *` | teaching assistant who prefers a CLI | start TAB from a terminal | begin working without leaving the keyboard |
| `* * *` | teaching assistant who prefers a CLI | exit TAB with a command | close it without reaching for the mouse |
| `* * *` | teaching assistant | view a list of all my students | see everyone I am responsible for at a glance |
| `* * *` | teaching assistant | view a student's name | identify the student |
| `* * *` | teaching assistant | view a student's email | contact them via email for any emergency updates |
| `* * *` | teaching assistant | view a student's tag | know which class the student is in |
| `* * *` | teaching assistant | add a student record | keep track of new students I have |
| `* * *` | teaching assistant | delete a student's record | remove students who dropped my class from the student book |
| `* * *` | teaching assistant | save the updates I made to student records | ensure my updates persist in future sessions |
| `* * *` | teaching assistant who can type fast and prefers CLI | complete every task TAB offers using only the keyboard | never break out of typing to reach for a mouse or trackpad |
| `* * *` | teaching assistant in Singapore | save a name containing s/o, d/o, hyphens or apostrophes | record students under the name that appears on the official roster |
| `* * *` | teaching assistant | flag a student as needing follow-up, and clear the flag once I have replied | keep an accurate list of who is still waiting on me |
| `* * *` | teaching assistant | find students by part of a detail in a field I choose, whatever the letter case | locate a student from a fragment, such as part of a name or a few digits of a phone number |
| `* * *` | teaching assistant handling student data | use TAB fully offline with data stored only on my own machine | comply with the expectation that student data stays off third-party services |
| `* *` | forgetful or new user | see a summary of everything TAB can do, from inside TAB | learn or recall how to use it without leaving the keyboard |
| `* *` | teaching assistant | edit a student's record | ensure a student's record reflect the latest information I know |
| `* *` | teaching assistant | add a tag to a student without disturbing the tags already on their record | build up a student’s tags over the semester without losing earlier ones |
| `* *` | teaching assistant | remove a single tag from a student | drop a student from one group without rebuilding the rest of their record |
| `* *` | teaching assistant | add a student without an email address | record the students I only have partial details for, instead of inventing data |
| `* *` | teaching assistant | know how long it has been since I marked a student for follow-up | know how stale our last interaction is |
| `* *` | teaching assistant who hand-edits the data file | be told at startup that my saved data is unreadable, and where the problem is, instead of TAB starting empty | repair it rather than silently lose everything |
| `* *` | teaching assistant | view a student's phone number | call them directly when an urgent matter comes up |
| `* *` | teaching assistant | undo my last command | revert a mistake before it makes trouble |
| `* *` | teaching assistant | find students by a tag | focus on a single tutorial group |
| `*` | teaching assistant | view a student's NUS Student ID | match a student's name to records on other digital platforms (e.g. attendance) |
| `*` | teaching assistant | find students by a specific detail such as name, NUS Student ID, phone or email | go straight to the right record when I know one thing about the student |
| `*` | teaching assistant who can type fast and prefers CLI | see a student record whenever I make changes to it | confirm that the changes I made are correct |
| `*` | teaching assistant with many students | import many students from a file and give them all the same tag at once | get a whole roster into TAB without entering each student by hand |
| `*` | teaching assistant with many students | edit student records en masse | update a whole group of students without repeating the same edit |
| `*` | teaching assistant with many students | delete entire classes | remove a class I no longer teach at the end of a semester |
| `*` | teaching assistant | define my own tag alias for a tag I use constantly | label a whole roster with far fewer keystrokes |
| `*` | teaching assistant | reset the tag aliases I have defined | clear tag aliases that no longer match the classes I teach |
| `*` | teaching assistant with exchange students | save a phone number with a country code and spaces | contact students whose numbers are not local 8-digit numbers |
| `*` | teaching assistant | be asked to confirm before all data is wiped | avoid destroying a semester of student records with one mistyped command |
| `*` | teaching assistant | view a student's remarks | recall important context such as special needs |
| `*` | teaching assistant | add a remark to a student's record | note down requests or needs I should remember |
| `*` | teaching assistant | redo an action I undid | restore a change I reverted by accident |
| `*` | teaching assistant | be warned when a new student matches an existing record on phone, email or NUS Student ID | catch genuine duplicates even when the two names are spelled differently |
| `*` | teaching assistant | merge two duplicate student records after seeing how they differ | combine the information from both without losing either |
| `*` | teaching assistant | find students who have no tag | spot students not yet assigned to a group |
| `*` | teaching assistant | sort my list by a detail I choose, such as name, NUS Student ID, or when the student was added | read the list in the order that suits the task in front of me |
| `*` | teaching assistant | group my students by tag | see the class composition at a glance |
| `*` | teaching assistant | export my student records to a file | share or print the list externally |
| `*` | teaching assistant | clear all data | start fresh with a clean student book |
| `*` | teaching assistant | reset TAB to sample data | explore how TAB works before entering real records |
| `*` | teaching assistant | keep separate data files per course | avoid mixing student records from different courses |
| `*` | long-time teaching assistant | archive student records after students graduate | keep past students for reference without cluttering my active list |
| `*` | teaching assistant who can type fast | recall and reuse my recent commands | repeat the same command without retyping it |
| `*` | teaching assistant who can type fast | auto-complete commands as I type | type common commands with fewer keystrokes |
| `*` | teaching assistant | see a count of my students | quickly confirm nothing was lost after an import or delete |
| `*` | teaching assistant | be told what was wrong with my input and which part of it caused the problem | correct it in one attempt instead of guessing |
| `*` | teaching assistant who hand-edits the data file | reload the data file from disk without restarting TAB | fix the file and carry on in the same sitting |
| `*` | teaching assistant | list students I have not followed up with since a given date | make sure no one who needs help flies under the radar |
| `*` | teaching assistant working in twenty-second windows | identify a student by their NUS Student ID when deleting a record | avoid removing the wrong student when several students are listed |
| `*` | teaching assistant working in twenty-second windows | identify a student by their NUS Student ID when editing a record | avoid changing the wrong student’s details |
| `* * *` | teaching assistant | record two students who genuinely have the same name | keep separate student records for them |
| `*` | teaching assistant who can type fast | be shown a likely correction when I mistype a command word | recover without looking up the syntax |
| `*` | teaching assistant | look back at results I have already replaced | re-read earlier output without running the search again |
| `*` | teaching assistant | start from my previous input when an entry is rejected | correct one detail instead of retyping everything |
| `*` | teaching assistant on a 1920 × 1080 laptop | start TAB with all of it already visible on my screen | begin working without resizing anything first |

#### Considered and dropped

These were raised during requirement gathering and left out of the product.

| As a ... | I wanted to ... | Why it was dropped |
|--------|---------------|--------------------|
| teaching assistant | view a student's address | Cancelled, no need for address |
| teaching assistant | send an email to a student | Not in the scope of TAB, it is not a messaging app |
| teaching assistant | send a Telegram message to a student | Not in the scope of TAB, it is not a messaging app |
| teaching assistant | send an SMS to a student | Not in the scope of TAB, it is not a messaging app |
| teaching assistant with many students | send a mass message to an entire class, by connecting to email | Not in the scope of TAB, it is not a messaging app |
| teaching assistant | record a student's attendance status per tutorial | Not in the scope of TAB, it is not an attendance taking app. Other platforms already exist to serve this purpose. |
| teaching assistant | view a student's attendance status | Not in the scope of TAB, it is not an attendance taking app. Other platforms already exist to serve this purpose. |
| teaching assistant | record grades for a student | Not in the scope of TAB, it is not a gradebook. Other platforms already exist to serve this purpose. |

### Use cases

(For all use cases below, the **System** is `TAB` and the **Actor** is the `User`, unless specified otherwise.)

**Table of contents**

* [UC01 - Add a student](#uc01-add-a-student)
* [UC02 - Find a student and act on the right one](#uc02-find-a-student-and-act-on-the-right-one)
* [UC03 - Flag and clear a follow-up](#uc03-flag-and-clear-a-follow-up)
* [UC04 - Import a roster (proposed)](#uc04-import-a-roster)
* [UC05 - Recover from an unreadable data file](#uc05-recover-from-an-unreadable-data-file)
* [UC06 - Delete a class no longer taught (proposed)](#uc06-delete-a-class-no-longer-taught)
* [UC07 - Archive last semester's students (proposed)](#uc07-archive-last-semester-s-students)
* [UC08 - Semester close-out (proposed)](#uc08-semester-close-out)
* [UC09 - Edit a student's details](#uc09-edit-a-student-s-details)
* [UC10 - Add/remove a tag from a student](#uc10-add-remove-a-tag-from-a-student)

#### UC01 - Add a student

**MSS**

1.  User requests to add a student, providing available details including the
    name and, optionally, the NUS Student ID and that the student needs follow-up.
2.  TAB saves the student record.
3.  TAB shows the new student record, including the NUS Student ID, tags, and follow-up
    status the User provided.

    Use case ends.

**Extensions**

* 1a. TAB does not recognize the command word and identifies a likely intended command.

    * 1a1. TAB suggests the likely command.
    * 1a2. User corrects the command word and submits again.

      Use case resumes at step 2.

* 1b. At least one detail is not in an acceptable form.

    * 1b1. TAB names every detail it rejected and the reason.
    * 1b2. TAB leaves the original input in the command box.
    * 1b3. User corrects the details and submits again.

      Steps 1b1-1b3 are repeated until every detail is acceptable.

      Use case resumes at step 2.

* 1c. The NUS Student ID matches one that another student already holds, ignoring case.

    * 1c1. TAB names the repeated NUS Student ID and leaves the student records
      unchanged.

      Use case ends.

* 1d. Before submitting the command, User chooses to abandon the addition.

    Use case ends.

* 2a. TAB cannot write to the data file.

    * 2a1. TAB reports that the student was not saved, and why.

      Use case ends.

#### UC02 - Find a student and act on the right one

**MSS**

1.  User requests to find students, specifying a field for each search term.
2.  TAB shows the students for whom every term matches part of its specified
    name, NUS Student ID, phone, email or tag, regardless of letter case,
    and any requested follow-up status matches.
3.  User identifies the intended student.
4.  User initiates the desired action on that student.

    Use case ends.

**Extensions**

* 1a. The search terms are not in an acceptable form.

    * 1a1. TAB reports the error.
    * 1a2. TAB leaves the original input in the command box.
    * 1a3. User corrects the search terms and submits again.

      Steps 1a1-1a3 are repeated until the search terms are acceptable.

      Use case resumes at step 2.

* 2a. No student matches the search terms.

    * 2a1. TAB reports that no matching students were found.

      Use case ends.

* 3a. None of the matching students is the intended student.

    * 3a1. User provides revised search terms.

      Use case resumes at step 2.

* 3b. User chooses not to act on the intended student.

    Use case ends.

#### UC03 - Flag and clear a follow-up

**MSS**

1.  <u>User finds and identifies a student who needs a response (UC02).</u>
2.  User requests to mark that student as needing follow-up.
3.  TAB saves the student as needing follow-up.
4.  TAB shows the updated student record with `Needs follow-up`.
5.  Later, <u>User finds and identifies the same student after responding
    (UC02).</u>
6.  User requests to clear follow-up for that student.
7.  TAB saves the student as not needing follow-up.
8.  TAB shows the updated student record without `Needs follow-up`.

    Use case ends.

**Extensions**

* 1a. UC02 ends without identifying a student, or User chooses not to act on
  the identified student.

    * 1a1. TAB leaves the student records unchanged.

      Use case ends.

* 3a. TAB cannot save the updated student record.

    * 3a1. TAB reports that the follow-up status was not changed, and why.

      Use case ends.

* 5a. UC02 ends without identifying the student, or User chooses not to clear
  the follow-up status.

    * 5a1. TAB leaves the student's follow-up status unchanged.

      Use case ends.

* 7a. TAB cannot save the updated student record.

    * 7a1. TAB reports that the follow-up status was not changed, and why.

      Use case ends.

#### UC04 - Import a roster (proposed)

**MSS**

1.  User requests to import a roster of students, providing the file and the class tag to label every student with.
2.  TAB saves the new student records with the given class tag, preserving each student record's other tags.
3.  TAB shows how many students were added, matched and skipped.

    Use case ends.

**Extensions**

* 1a. TAB cannot find or read the file.

    * 1a1. TAB reports the problem and leaves the student records unchanged.

      Use case ends.

* 1b. A row in the file is missing a required detail or is otherwise malformed.

    * 1b1. TAB names the row and the reason, skips it, and continues to process the other rows.

      Step 1b1 is repeated for each malformed row.

      Use case resumes at step 2.

* 1c. A student matches another student whom TAB already holds, on NUS Student ID, email or phone.

    * 1c1. TAB shows the match and asks whether to update the existing student record with the imported details.
    * 1c2. User chooses whether to update or skip the existing student record.
    * 1c3. TAB applies the User's choice, preserving the student's other tags, and continues to process the other
      rows.

      Steps 1c1-1c3 are repeated for each match.

      Use case resumes at step 2.

* 1d. Before TAB saves the import, User chooses to abandon it.

    * 1d1. TAB leaves the student records unchanged.

      Use case ends.

* 2a. TAB cannot write to the data file.

    * 2a1. TAB reports that the import was not saved, and why.

      Use case ends.

#### UC05 - Recover from an unreadable data file

**MSS**

1.  User starts TAB.
2.  TAB informs the User that a student record is unreadable, identifies the
    data file, student record and reason, and leaves the data file unchanged.
3.  User dismisses the error and TAB closes without opening the main window.
4.  User repairs or replaces the data file.
5.  User starts TAB again.
6.  TAB loads the repaired data and opens the main window.

    Use case ends.

**Extensions**

* 2a. TAB cannot read the data file at all, for example because it is not a TAB data file.

    * 2a1. TAB identifies the data file and parsing location when available,
      reports why it is unreadable, shows a bounded line-numbered source
      excerpt when possible, and leaves it unchanged.

      Use case resumes at step 3.

* 6a. One or more student records are still unreadable.

    * 6a1. TAB reports the first unreadable student record and reason, leaves
      the data file unchanged, and closes after User dismisses the error.
    * 6a2. User repairs the data file and starts TAB again.

      Steps 6a1-6a2 are repeated until all student records are readable.

      Use case resumes at step 6.

#### UC06 - Delete a class no longer taught (proposed)

**MSS**

1.  User requests to delete a class, giving its class tag.
2.  TAB shows the class and every student in it.
3.  TAB asks the User to confirm the deletion, warning that students whose only tag is this class will be removed as
    well.
4.  User confirms the deletion.
5.  TAB removes the class from the student book and removes students who belong to no other class.
6.  TAB shows how many students were affected.

    Use case ends.

**Extensions**

* 2a. TAB holds no student tagged with that class.

    * 2a1. TAB reports that there is no such class.

      Use case ends.

* 3a. User chooses not to delete the class.

    * 3a1. TAB leaves the student records unchanged.

      Use case ends.

* 5a. TAB cannot write to the data file.

    * 5a1. TAB reports that the deletion was not saved, and why.

      Use case ends.

#### UC07 - Archive last semester's students (proposed)

**MSS**

1.  User requests to archive a past class, giving its class tag and the semester.
2.  TAB shows the student records that would be archived.
3.  TAB asks for confirmation.
4.  User confirms.
5.  TAB removes those student records from the active list while keeping them available in the archive.
6.  TAB shows how many student records were archived.

    Use case ends.

**Extensions**

* 2a. No student is tagged with that class for that semester.

    * 2a1. TAB reports that there is nothing to archive.

      Use case ends.

* 3a. User chooses not to archive the students.

    * 3a1. TAB leaves the student records unchanged.

      Use case ends.

* 5a. TAB cannot write to the data file or the archive.

    * 5a1. TAB reports that the archiving was not saved, and why.

      Use case ends.

#### UC08 - Semester close-out (proposed)

**MSS**

1.  User requests a semester close-out, giving the semester and, if needed, the course(s) it covered.
2.  TAB lists every student still active for that semester.
3.  TAB asks the User to confirm the semester close-out.
4.  User confirms the semester close-out.
5.  TAB archives the semester's student records except any the User chose to hold back, preserving the archived
    student records' follow-up status.
6.  TAB prepares the active list for the coming semester, retaining only student records the User chose to hold back
    and keeping the archive available.

    Use case ends.

**Extensions**

* 2a. No student is active for that semester.

    * 2a1. TAB reports that there is nothing to include in the semester close-out.

      Use case ends.

* 2b. One or more students still await a follow-up.

    * 2b1. TAB reminds the User how many, and asks whether to archive all of them or hold any back.
    * 2b2. User chooses which students, if any, to hold back.

      Use case resumes at step 3.

* 3a. User chooses not to proceed with the semester close-out.

    * 3a1. TAB leaves the student records unchanged.

      Use case ends.

* 5a. TAB cannot write the archive or the new data file.

    * 5a1. TAB reports that the semester close-out was not saved, and why.

      Use case ends.

#### UC09 - Edit a student's details

**MSS**

1.  <u>User finds and identifies the intended student (UC02).</u>
2.  User requests to edit the student.
3.  User specifies changes to the student's details, follow-up status, or both.
4.  TAB updates the provided details, sets or clears follow-up if requested, and
    preserves the other details.
5.  TAB shows the updated student record.

    Use case ends.

**Extensions**

* 3a. User specifies no change.

    * 3a1. TAB reports that the edit must include a change.

      Use case ends.

* 3b. At least one new detail is not in an acceptable form.

    * 3b1. TAB names every detail it rejected and the reason.
    * 3b2. User corrects the details and submits again.

      Steps 3b1-3b2 are repeated until every detail is acceptable.

      Use case resumes at step 4.

* 3c. User chooses to abandon the edit.

    * 3c1. TAB leaves the student record unchanged.

      Use case ends.

* 3d. The edited NUS Student ID matches one that another student already holds, ignoring case.

    * 3d1. TAB names the repeated NUS Student ID and leaves both student records
      unchanged.

      Use case ends.

* 4a. TAB cannot write to the data file.

    * 4a1. TAB reports that the changes were not saved, and why.

      Use case ends.

#### UC10 - Add/remove a tag from a student

**MSS**

1.  <u>User finds and identifies the intended student (UC02).</u>
2.  User requests to add or remove a tag from the student, providing the tag.
3.  TAB makes the requested change while preserving the student's other tags.
4.  TAB shows the student record.

    Use case ends.

**Extensions**

* 2a. The tag is not in an acceptable form.

    * 2a1. TAB explains why the tag was rejected.
    * 2a2. User corrects the tag and submits again.

      Steps 2a1-2a2 are repeated until the tag is acceptable.

      Use case resumes at step 3.

* 2b. User chooses to abandon the change.

    * 2b1. TAB leaves the student's tags unchanged.

      Use case ends.

* 3a. User requests to add a tag that the student already has.

    * 3a1. TAB reports that the student already has the tag and leaves the student's tags unchanged.

      Use case resumes at step 4.

* 3b. User requests to remove a tag that the student does not have.

    * 3b1. TAB reports that the student does not have the tag and leaves the student's tags unchanged.

      Use case resumes at step 4.

* 3c. TAB cannot write to the data file.

    * 3c1. TAB reports that the tag was not added or removed, and why.

      Use case ends.


### Non-Functional Requirements

1.  **Operating environment**: Should work on any _mainstream OS_ as long as it has Java `25` (64-bit)
    installed without requiring platform-specific installation or OS-dependent libraries. This
    satisfies `Constraint-Platform-Independent` and `Constraint-Java-Version`.
2.  **Portability**: Should be packaged as a single executable JAR file of at most 100 MB that runs
    directly without requiring an installer or system administrator privileges. This satisfies
    `Constraint-Portable`, `Constraint-Single-File`, and `Constraint-File-Size`.
3.  **Single-user operation**: Should be designed for a single user operating locally at any given time,
    without requiring or supporting concurrent multi-user write access to the data file. This
    satisfies `Constraint-Single-User`.
4.  **CLI-first accessibility**: All student management operations must be executable entirely via
    keyboard commands without requiring a mouse, trackpad, or other pointing device. This
    satisfies `Constraint-Typing-Preferred`.
5.  **Typing efficiency**: A user with above-average typing speed for regular English text should be able
    to accomplish regular student management tasks faster using CLI commands than using a traditional
    mouse-driven graphical user interface. This satisfies `Constraint-Typing-Preferred`.
6.  **Capacity**: Should comfortably store and manage up to 1,000 student records (sufficient for multiple
    semesters of active and archived tutorial and lab slots) without noticeable degradation in performance.
7.  **Responsiveness**: Should execute typical commands (such as adding, editing, deleting, tagging, listing,
    and filtering across up to 1,000 student records) and update the GUI within 100 milliseconds on
    standard modern desktop hardware.
8.  **Startup time**: Should launch and display the GUI, ready for user command input, within 2 seconds
    on standard modern desktop hardware with an existing data file containing up to 1,000 student records.
9.  **Screen resolution and scaling**: The GUI should render cleanly without visual clipping or overlapping
    components at screen resolutions of 1920×1080 and higher with display scaling at 100% and 125%, and
    remain functional and usable down to 1280×720 with display scaling up to 150%. This satisfies
    `Constraint-Screen-Resolution`.

### Glossary

> Names do have a predetermined goodness. If they are straightforward, simple, and do not conflict, then they are called “good names.”
>
> -Xunzi

* **Active list**: The student records used for current teaching work, excluding records moved to the proposed
  archive.
* **Address book**: A legacy term found in the upstream AddressBook-Level3 project and retained here only when
  referring to that project's history. TAB calls its collection of student records the student book.
* **Archive**: In the proposed archiving feature, the collection of past student records retained by TAB but excluded
  from the active list.
* **ASCII space**: The ordinary space character (`U+0020`). This term means that exact character rather than every
  whitespace character.
* **Class**: In the product domain, a tutorial or lab group taught by the User. This is distinct from a Java class.
* **Class tag**: A tag used to identify a class.
* **CLI (Command-Line Interface)**: The keyboard-driven interface through which the User enters commands into TAB.
* **Command option**: A separate, hyphen-prefixed token, such as `-p` or `--phone`, that identifies the value
  following it. The `add` and `edit` commands use options.
* **Command prefix**: A marker attached to the beginning of a field value, such as `p/`. AB3 marked its fields with
  prefixes, where TAB uses options.
* **Course**: An academic subject (e.g., CS2103T) taught by the User.
* **Data file**: The local, human-editable JSON file (`tab.json`) used by TAB to store student records,
  distinct from `preferences.json`.
* **Filtered list**: The subset of student records currently visible in the GUI as a result of a filter or search
  command (such as `find` or `list`). Commands taking an Index target records relative to this list.
* **Follow-up**: An action or response that the User still owes to a student,
  tracked in TAB as a pending status on a student record.
* **Identity field**: A field used by TAB to identify whether two records refer to the same student. The NUS Student
  ID is TAB's sole identity field. Name, phone number, and email are searchable details rather than identity keys.
* **Index**: The 1-based positive integer identifying a student according to their current display position in the
  active/filtered list shown in the GUI, rather than a permanent database identifier or 0-based code index.
* **Mainstream OS**: Windows, Linux and macOS.
* **NFC (Normalization Form C)**: The Unicode normalization form TAB uses to store canonically equivalent character
  sequences consistently.
* **Normalized field value**: The standard stored form produced by `StringUtil.normalizeFieldValue`, allowing input
  that looks the same to be stored and compared consistently.
* **NUS Student ID**: A unique digital identifier for every student at the National University of Singapore. TAB
  accepts between 1 and 32 ASCII letters and digits, such as `A01234567X` or `E0123456`. Lowercase letters are
  accepted and stored in uppercase.
* **Option**: See **Command option**.
* **Person**: A legacy term found in the upstream AddressBook-Level3 project. TAB's model uses `Student` for a student
  record.
* **Prefix**: See **Command prefix**.
* **Roster**: A list of students belonging to a course, tutorial group, or lab group, particularly one prepared for
  import into TAB.
* **Semester close-out (proposed)**: The end-of-semester workflow (UC08) where active student records
are archived
  and the active list is reset in preparation for a new academic term, optionally retaining records with
  pending follow-ups.
* **Student book**: The collection of student records managed by TAB.
* **Student record**: The information TAB stores about one student, such as their name, NUS Student ID, phone number, email
  address, and tags.
* **TAB**: Teaching Assistant's Book; the desktop student-management application described in this guide.
* **Tag**: A user-defined label attached to a student record for grouping or filtering students.
* **Tag alias**: A user-defined shorthand for a longer tag (such as a shortcut for a tutorial group tag), distinct
  from a command or shell alias.
* **User**: The teaching assistant operating TAB.
* **Whitespace**: The Unicode characters with the `White_Space` property, as matched by Java's `(?U)\s`: `U+0009` to
  `U+000D`, `U+0020`, `U+0085`, `U+00A0`, `U+1680`, `U+2000` to `U+200A`, `U+2028` to `U+2029`, `U+202F`, `U+205F`
  and `U+3000`. This set excludes the zero-width space (`U+200B`), byte order mark (`U+FEFF`), zero-width non-joiner
  (`U+200C`) and zero-width joiner (`U+200D`). It governs `CommandTokenizer` and normalized field values, not every
  parser: top-level command parsing retains narrower ASCII-oriented rules.

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Given below are instructions to test TAB manually.

<box type="info" seamless>

**Note:** These instructions only provide a starting point for testers to work on;
testers are expected to do more *exploratory* testing.
</box>

### Launch and shutdown

1. Initial launch

   1. Download the JAR file and copy it into an empty folder.

   1. Double-click the JAR file.<br>
      Expected: The GUI opens with a set of sample student records. The window size may not be optimal.

1. Keeping application files with the JAR

   1. Put the JAR in an empty folder. Launch it, add a recognizable student,
      then exit.<br>
      Expected: The folder now contains `data/tab.json`, `preferences.json`,
      and `tab.log.0`.

   1. Open a terminal in a different folder and launch the same JAR using its
      absolute path.<br>
      Expected: The recognizable student is present. The status bar shows the
      absolute path to the original `data/tab.json`, and no data, preference,
      or log file is created in the terminal's working folder.

1. Launching from a read-only folder

   1. Prerequisites: Copy the JAR into a new folder that the current user can
      read but cannot write to. On Linux/macOS, run `chmod 555 FOLDER` and use
      a non-root account. On Windows, remove write permission for the test
      account using the folder's Security settings.

   1. From that folder, run `java -jar tab.jar`.<br>
      Expected: The GUI opens with sample records. The terminal warns that it
      cannot write the JAR folder's `tab.log`, continues with console logging,
      and prints one preferences warning naming `preferences.json`, the reason,
      and that preferences will be used for this session only. The preferences
      warning has no stack trace or `SEVERE` label. There is no
      `ExceptionInInitializerError` or `NullPointerException` from logging.

   1. Run `find -n "Read Only Test"`, then `list`, then `help`.<br>
      Expected: The search shows no matches, `list` restores all sample
      records, and the help page opens. None reports a data-file save error.
      Press q or Esc to return to the command box.

   1. Run `add Read Only Test -p 91234567`.<br>
      Expected: TAB reports a data-file save error and remains open. The
      record is not visible because the failed change is rolled back.

   1. Run `list`, then `exit`.<br>
      Expected: `list` excludes the failed record without a save error, and
      `exit` closes TAB with one warning naming `preferences.json` and
      explaining that window preferences could not be saved. That warning has
      no stack trace or `SEVERE` label. Restore write permission and relaunch.
      The failed record is still absent.

1. Full-page help pager

   1. Enter `help`.<br>
      Expected: Help replaces the student list, summary sidebar, command box and
      result area. No extra window or browser opens. Navigation keys remain
      visible in the footer and work immediately without mouse interaction.
      The notice reads `Help mode: press q or Esc to return to commands.`.

   1. Press Down, then Enter.<br>
      Expected: Each moves down exactly one line and leaves the opening notice
      unchanged. Press q and reopen with F1; the same opening notice appears.

   1. Press Space/f and b to move forward/back a screenful. Try Enter/j and k,
      d/u, PgDn/PgUp, arrows and Home/End. Press g then G.<br>
      Expected: Each moves as documented; G reaches the last guide-copy hint and
      the footer says `(END)`. Repeated paging at either end stays there.
      Repeat at minimum size and resize while on the last screenful: all text
      remains reachable without scrollbars.

   1. Press c while help is open, then paste into a browser address bar.<br>
      Expected: The exact address `https://ay2627s1-cs2103t-t16-1.github.io/tp/UserGuide.html`
      is copied and the footer confirms `User Guide URL copied.`. The reading
      position, student data and unfinished command stay unchanged. Typing an
      unused letter such as x shows `Press q or Esc to return to the command box.`.
      Reopening help clears that temporary notice.

   1. Press q, enter `list`, then reopen help and press Esc.<br>
      Expected: Both quit keys restore the student view and focus the command
      box. No q is inserted into it. The next command works normally; the
      previous result and student filter are preserved while help is open.

   1. Type `find -n Alex` without submitting it, press F1, then q.<br>
      Expected: Help takes the whole content area and the unfinished command
      is restored on return, unselected with the caret at the end. Type ` Tan`:
      the box now contains `find -n Alex Tan`. Repeat with Help → Help and with
      Esc to quit. On returning, Backspace removes only the final character;
      Delete leaves the draft unchanged. Also repeat with an empty command box:
      typing starts a new command normally. Reopening help starts at the beginning.

   1. Enter `help oops`, `help -p` and `help ""`.<br>
      Expected: Each reports `help takes no arguments. Unexpected input: ...`
      with the offending input quoted. No help page opens and student data stays
      unchanged. `help` with trailing spaces or tabs opens the pager normally.

1. Saving window preferences

   1. Resize the window to an optimal size. Move the window to a different location. Close the window.

   1. Relaunch TAB by double-clicking the JAR file.<br>
       Expected: The most recent window size and location are retained when
       they fit the available display's usable area.

   1. With TAB closed, set `windowWidth` and `windowHeight` in
      `preferences.json` under `guiSettings` to `1.0E9` and set
      `windowCoordinates` to `{"x":99999,"y":99999}`. Relaunch.<br>
      Expected: The entire window fits the primary display's usable area.
      Its title bar, command box and status bar remain reachable. Close and
      relaunch again: the corrected bounds are retained and student data is
      unchanged.

   1. Move a normally sized window to a secondary display, close and relaunch
      while both displays are connected, then close, disconnect that display
      and relaunch again.<br>
      Expected: With both displays, the saved position is retained, including
      on a display left of the primary. After disconnection, TAB opens fully
      on an available display without deleting preferences. Repeat after
      reducing resolution or increasing scaling, including 1280×720 at 150%.

1. Recovering from null preferences or window settings

   1. Prerequisites: Add a recognizable student, close TAB, and keep a backup
      of `data/tab.json`.

   1. Replace the contents of `preferences.json` with `{"guiSettings": null}`
      and run `java -jar tab.jar`.<br>
      Expected: The main window opens with default window settings and shows
      the saved student. The terminal reports that the preference file could
      not be loaded and defaults are being used, followed by
      `GUI settings must not be null.` The preferences file now
      contains a non-null `guiSettings` object.

   1. Close TAB and launch it again.<br>
      Expected: The window opens normally again. The saved student is still
      present, and `data/tab.json` matches the backup.

   1. Close TAB, replace the contents of `preferences.json` with only `null`,
      and repeat both launches.<br>
      Expected: Both launches open normally with default window settings and
      the saved student. The first launch reports
      `The JSON document must not be null.` and rewrites valid preferences.
      The student data file still matches the backup.

1. Narrow student cards

   1. Add a student with
      `add Narrow Card -p "+65 9123 4567" -e a.very.long.address.indeed@u.nus.edu --id AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA`.

   1. Resize the window to its minimum width, then widen it slowly.<br>
      Expected: The ID and email shorten as needed, the phone remains readable,
      and the email stays inside the card's details area. Check on Windows as
      well as Linux and macOS, since font widths differ.

### Rejecting unexpected command arguments

1. Commands that take no parameters

   1. Prerequisites: Add a student, run `find -n no-such-student` to hide the
      student, and keep a copy of `data/tab.json`.

   1. Test cases: `clear oops`, `list oops`, `help oops`, and `exit oops`.<br>
      Expected: Each command reports that it takes no arguments and quotes
      `oops`. The list stays filtered, no help summary opens, TAB stays open,
      and the JSON file matches the copy byte for byte.

   1. Test cases: `clear "oops"`, `clear ""`, and `clear "unfinished`.<br>
      Expected: Each reports the unexpected input. The list and JSON file
      stay unchanged, including for an empty quoted argument.

   1. Test cases: `list` and `help` with only trailing spaces or tabs.<br>
      Expected: The list shows all students and the help summary appears respectively.
      `exit` with trailing whitespace closes TAB. Run plain `clear` with
      trailing whitespace only when the test data can be removed; it follows
      the documented clear flow.

### Typing a command word in another case

1. A correctly spelt command word in capitals

   1. Prerequisites: at least one student listed.

   1. Test cases: `List`, `LIST`, and `Help`<br>
      Expected: Each runs the command of that name, so all students are listed
      and the help summary opens. No reply names a word TAB does not know.

   1. Test case: `ADD McDonald Tan -p 91234567`<br>
      Expected: A student named `McDonald Tan` is added with the capitals as
      typed, in the card and in `data/tab.json`, the fold reaching the command
      word alone.

   1. Test case: `LIST oops`, after taking a copy of `data/tab.json`, since
      the step above writes to it<br>
      Expected: The reply reads `list takes no arguments. Unexpected input:
      "oops".`, naming the command rather than the capitals typed. The list is
      unchanged and the JSON file matches that copy byte for byte.

   1. Test case: `EXIT`<br>
      Expected: TAB closes, with the added student in the saved file.

### Mistyping a command word

1. A command word one slip away from a real one

   1. Prerequisites: at least one student listed, and a copy of
      `data/tab.json`.

   1. Test case: `delte 1`<br>
      Expected: The reply quotes `delte` and suggests `delete`. The student is
      still listed, the JSON file matches the copy byte for byte, and
      `delte 1` is still in the command box with the line red, so the missing
      letter can be typed in rather than the command retyped.

   1. Test cases: `Delte 1` and `DELTE 1`<br>
      Expected: The same suggestion, with the word quoted in the case it was
      typed in rather than in lower case.

   1. Test cases: `lsit`, `hepl`, `addd`, and `clexr`<br>
      Expected: Each suggests `list`, `help`, `add`, and `clear` respectively.
      None of them runs: the list is unchanged, no help summary opens, and no
      student is added.

   1. Test cases: `ls`, `dlte 1`, and `fit`<br>
      Expected: Two letters out is still near enough, so `list`, `delete` and
      `find` are suggested. None of them runs, and the list is unchanged.

   1. Test cases: `felete 1` and `edlete 1`<br>
      Expected: `delete` is suggested for both, the first letter being the
      only thing out of place.

1. A command word stopped partway

   1. Test cases: `del 3`, `dele 3`, and `cle`<br>
      Expected: `delete`, `delete` and `clear` are suggested. No student is
      deleted and nothing is cleared.

1. A command word that several commands answer to

   1. Test cases: `e` and `eit`<br>
      Expected: Both name `edit` and `exit`, in that order and in the same
      order every time the word is entered again.

1. A command word resembling nothing TAB knows

   1. Test cases: `qwerty`, `rm`, `dlt 1`, and `日本語`<br>
      Expected: Each reply quotes the word typed and points to `help`, with no
      suggestion offered. `dlt` is two letters from `edit`, but opens with a
      letter that command does not, so TAB offers nothing rather than guessing.
      No help summary opens until `help` itself is run.

   1. Test case: paste a line of at least 80 characters with no spaces in it,
      such as the letter `x` repeated.<br>
      Expected: The reply quotes the opening 60 characters followed by an
      ellipsis and still ends with the pointer to `help`, rather than running
      off the result display.

### Follow-up status

1. Adding students with and without follow-up

   1. Prerequisites: Launch the JAR from an empty folder, then run `clear`
      and reply `yes` if confirmation is requested.

   1. Test case: `add Alice Tan -p 91234567`<br>
      Expected: Alice is added. Neither her card nor the command result shows
      `Needs follow-up`. The saved Alice record in `data/tab.json` has no
      `flag` key.

   1. Test case: `add Bob Lim -p 92345678 --follow-up`<br>
      Expected: Bob is added. His card and the command result both show
      `Needs follow-up`, and his saved record has `"flag": true`.

1. Setting and clearing follow-up while editing

   1. Prerequisites: Run `list`, restoring Alice
      at index 1 and Bob at index 2.

   1. Test case: Run `edit 1 --follow-up` twice, then `edit 1 -f`.<br>
      Expected: Alice shows `Needs follow-up` after each command; repeating
      either spelling keeps the flag set. Her other fields stay unchanged.

   1. Test case: `edit 1 -p 81112222`<br>
      Expected: Alice's phone changes and `Needs follow-up` remains.

   1. Test case: Run `edit 1 -nf` twice, then `edit 1 --no-follow-up`.<br>
      Expected: Alice no longer shows `Needs follow-up`, with no blank row
      left in its place. Repeating either spelling keeps the flag cleared.

   1. Test case: `edit 1 -p 82223333`<br>
      Expected: Alice's phone changes and she stays unflagged.

   1. Test case: `edit 2 --follow-up` followed by `edit 2 --no-follow-up`.<br>
      Expected: Bob stays flagged after the first command and becomes
      unflagged after the second. Exit and relaunch TAB; both Alice and Bob
      remain unflagged and Alice's phone remains `82223333`.

1. Rejecting invalid or repeated follow-up markers

   1. Prerequisites: Continue from the preceding test and note each student's
      current details and follow-up status.

   1. Test cases: `add Cara Ng -p 93456789 --follow-up -f` and
      `add Cara Ng -p 93456789 --follow-up true`<br>
      Expected: Neither command adds Cara. The first reports
      `Options given more than once: --follow-up. Give each listed option only once.`;
      the second reports that `true` belongs to no option. The saved file is unchanged.

   1. Test cases: `edit 1 --follow-up -f` and `edit 1 --follow-up true`<br>
      Expected: Neither command changes Alice. The first reports
      `Options given more than once: --follow-up. Give each listed option only once.`
      The second reports that `true` belongs to no option.

   1. Test cases: `edit 1 --follow-up --no-follow-up` and
      `edit 1 -nf -f -p 89998888`<br>
      Expected: Both report that the follow-up options cannot be used together.
      Neither changes Alice's phone, flag, other fields, or saved record.

   1. Test cases: `edit 1 --no-follow-up -nf` and
      `edit 1 -nf false`<br>
      Expected: The first reports a repeated `--no-follow-up`; the second
      reports that `false` belongs to no option. No data changes.

1. Persisting and loading follow-up status

   1. Prerequisites: Add a flagged student and an unflagged student, then exit
      TAB and launch the same JAR from the same folder again.<br>
      Expected: The flagged student's `Needs follow-up` row returns and the
      unflagged student still has no such row.

   1. Exit TAB. Back up `data/tab.json`, then replace it with a valid student
      book whose records respectively use `"flag": true`, `"flag": false`,
      `"flag": null`, and no `flag` key. Relaunch TAB.<br>
      Expected: Only the record with `true` shows `Needs follow-up`. The other
      three load as unflagged, including the legacy record with no key.

   1. Exit TAB and change one record to `"flag": 2`, then relaunch. Repeat
      with `"flag": "true"`.<br>
      Expected: The error identifies that record and its invalid `flag` value.
      The main window does not open, and the file stays byte-for-byte
      unchanged. Restore the backup before continuing.

### Adding one tag

1. Preserving the selected student's other details and saving the new tag

   1. Prerequisites: Add a uniquely named student with `add Tag Test -p 91234567
      -e tag@example.com --id A01234567X -t T1 --follow-up` as one line.
      Run `find -n "Tag Test"` and confirm only this student is shown.

   1. Test case: `tag 1 --add "Lab 3"`.<br>
      Expected: Feedback starts with `Added tag [Lab 3]:`. The student has both
      `T1` and `Lab 3`, and all other details and follow-up status are unchanged.
      The search stays active. Exit TAB and relaunch the same JAR from the same
      folder; the tags and every other field remain as recorded.

1. Tagging by stable student ID

   1. Prerequisites: Add `add ID Tag Test -p 92345678 --id E7654321 -t T1
      --follow-up` as one line. Use an ID not already recorded in the book.
      Run `find -n NoMatchingStudent` so the new student is hidden.

   1. Test case: `tag e7654321 --add="Lab 3"`, then
      `tag --id E7654321 -r "Lab 3"`.<br>
      Expected: Both succeed on ID Tag Test, preserving the active search and
      every unrelated field. Exit and restart TAB; `find --id E7654321` shows
      the original `T1` tag and follow-up state.

   1. Test case: Add `add Numeric Tag Test -p 93456789 --id 1234`, then run
      `tag -i 1234 -a T2` and `tag --id=1234 --remove T2`.<br>
      Expected: Both actions address Numeric Tag Test. A bare `tag 1234 -a T2`
      remains an index request and fails when fewer than 1234 students are shown.

   1. Test case: `tag 1 --id E7654321 -a T2`.<br>
      Expected: The reply asks for one target. No record, displayed list or
      saved file changes. Delete the two students added in this section by ID
      (`delete E7654321` and `delete --id 1234`) before continuing.

1. Rejecting duplicates and invalid input without mutation

   1. Prerequisites: Continue with the student above and run `find -n "Tag Test"`.
      Back up `data/tab.json` and compare it after each refused command.

   1. Test case: `tag 1 -a "  lab   3  "`.<br>
      Expected: `This student already has tag [Lab 3]. No changes made.` The
      existing `Lab 3` spelling, all other details, displayed list and saved
      file stay unchanged.

   1. Test cases: `tag 0 -a T2`, `tag 2 -a T2`, `tag 2147483648 -a T2`,
      `tag 1 -a ""`, `tag 1 -a "---"`, `tag 1 -a`, and `tag 1 -a T2 --add T3`.<br>
      Expected: Each is refused with an index, invalid-tag, missing-value or
      duplicate-option error as appropriate. No student or displayed list
      changes, and `data/tab.json` stays unchanged.

   1. Test cases: `tag -1 -a T2` and `edit -1 -p 91234567`.<br>
      Expected: Both report `Index must be a positive integer.` and
      `There is no -1 option.` The tag command also shows the action guidance.
      No record, displayed list or saved file changes.

   1. Test cases: `tag 1 -t T2` and `tag 1 lab4`.<br>
      Expected: Both show guidance for `-a`/`--add` and `-r`/`--remove` with examples.
      The first names the unknown `-t` option; the second names `lab4` as an
      unexpected value after the index. No record, displayed list or saved
      file changes.

### Removing one tag

1. Preserving other details and persisting removal

   1. Prerequisites: Complete the single-tag addition tests above. Run
      `find -n "Tag Test"`; the student has `T1`, `Lab 3` and follow-up enabled.

   1. Test case: `tag 1 --remove "  LAB   3  "`.<br>
      Expected: Feedback starts with `Removed tag [Lab 3]:`. Only `Lab 3` is
      removed; `T1`, name, phone, email, NUS Student ID and follow-up status
      remain unchanged. The search stays active. Exit and relaunch TAB from
      the same folder; confirm every remaining field is preserved.

   1. Test case: Run `find -n "Tag Test"` again after restarting and confirm only
      that student is shown, then run `tag 1 -r T1`.<br>
      Expected: The student has no general tags and still needs follow-up.
      After restarting TAB, that state remains.

1. Reporting absent tags and invalid input without saving

   1. Prerequisites: Run `find -n "Tag Test"` again. Back up `data/tab.json`
      and compare it after every refused command below.

   1. Test cases: `tag 1 -r "Lab 3"`, `tag 1 --remove "  lab   3  "`.<br>
      Expected: `This student does not have tag [Lab 3]. No changes made.`
      and `This student does not have tag [lab 3]. No changes made.`,
      respectively. Record, displayed list and saved bytes stay unchanged.

   1. Test case: `tag 1 -r "Needs follow-up"`, on this student, who needs
      follow-up and carries no tag of that name.<br>
      Expected: The reply adds a second line, `This student is flagged for
      follow-up, which is not a tag. To clear the flag, use edit 1
      --no-follow-up.` The card still shows the label, and running that
      `edit` command takes it off. On a student who is not flagged, the same
      command gives the first line alone.

   1. Test cases: `tag 0 -r T1`, `tag 2 -r T1`, `tag 2147483648 -r T1`,
      `tag 1 -r ""`, `tag 1 -r "---"`, `tag 1 -r`,
      `tag 1 -r T1 --remove T2` and `tag 1 -a T2 -r T1`.<br>
      Expected: Specific index, invalid-tag, missing-value, duplicate-option
      or conflicting-action errors. No record, displayed list or file changes.

1. Removing a tag that supplies the search match

   1. Test case: Run `tag 1 -a "Lab 3"`, `find -t "Lab 3"`, then
      `tag 1 -r "Lab 3"`.<br>
      Expected: The student disappears from the filtered list. Run `list`
      to see the preserved student without `Lab 3`.

1. Removing variants from a human-edited data file

   1. Prerequisites: Exit TAB. Back up the data file, then set Tag Test's
      `tags` array to `["Lab 3", "  lab   3  ", "T1", "Needs follow-up"]`.
      Keep `"flag": true` and restart TAB. Run `find -n "Tag Test"`.

   1. Test case: `tag 1 -r "LAB 3"`.<br>
      Expected: Both case variants disappear. `T1`, `Needs follow-up`, the
      follow-up flag and all other fields remain after restart.

   1. Test case: Run `find -n "Tag Test"` again after restarting and confirm only
      that student is shown, then run `tag 1 -r "needs follow-up"`.<br>
      Expected: Only the general tag disappears. `T1` and the follow-up flag
      remain. Restore the backup when finished.

### Adding a student

1. Adding a student with minimal fields

   1. Test case: `add John Doe -p 98765432`<br>
      Expected: John Doe is added to the student book and appears in the list.
      The status message shows `New student added: John Doe; Phone: 98765432`
      without placeholder labels for absent email or tags.

1. Adding a student with tags and follow-up

   1. Test case: `add Jane Doe -p 91234567 -t urgent -t lab1 --follow-up`<br>
      Expected: Jane Doe is added with tags and follow-up flag. The status message
      shows `New student added: Jane Doe; Phone: 91234567; Tags: [lab1][urgent]; Needs follow-up`
      with tags sorted alphabetically ignoring case.

1. Recovering from a name option or a missing name

   1. Test cases: `add --name "Betsy Crowe" --phone 1234567`,
      `add -n "Betsy Crowe" -p 1234567`, and
      `add --phone 1234567 --name="Betsy Crowe"`.<br>
      Expected: No student is added. The error names the unsupported option,
      followed by `In the add command, the name comes first, without an option: add NAME -p PHONE ...`.
   1. Test case: `add -p 1234567`.<br>
      Expected: `Missing required field(s): NAME`, followed by the same placement
      hint. The student list is unchanged.
   1. Test case: `add Betsy Crowe --phone 1234567`.<br>
      Expected: Betsy Crowe is added. Exit and reopen TAB; the student remains saved.

1. Adding a duplicate NUS Student ID

   1. Prerequisites: In a fresh launch, add `add John Doe -p 91234567 -i A05678901K`
      so that a student named `John Doe` with NUS Student ID `A05678901K` is in the list.

   1. Test case: `add Jane Doe -p 98765432 -i a05678901k`<br>
      Expected: No student is added. The status message names NUS Student ID
      `A05678901K` and `John Doe`, and no record is mutated.

1. Adding during a search

   1. Prerequisites: Back up the data file. In an empty book, add
      `add Alice Tan -p 91110001 -t T1`, `add Bob Lim -p 91110002 -t T2`,
      and `add Carl Ong -p 91110003 -t T1`.
   1. Test case: `find -t T1`, then `add Eve Ng -p 91110005 -t T1`.<br>
      Expected: Only Alice, Carl and Eve appear, in that order. The reply is
      `New student added: Eve Ng; Phone: 91110005; Tags: [T1]`, with no
      search-cleared notice. `tag 2 -a "Lab 3"` tags Carl; `delete 2` deletes
      Carl, leaving Bob untouched.
   1. Test case: `add Dan Koh -p 91110004 -t T2`.<br>
      Expected: All four remaining students appear. The reply ends with
      `Search cleared to show the new student.` Re-read indexes before
      using another index command.
   1. Exit and relaunch from the same folder, then run `list`.<br>
      Expected: Alice, Bob, Eve and Dan remain; Carl is absent. Restore the
      backup when finished.

### Long contact details on a card

1. Reading an annotated phone and an email

   1. Test case: `add Priya Nair -p "1234 5678 (HP) 1111-3333 (Office)" -e e0923841@u.nus.edu -t T1 --follow-up`.
      Narrow the window to its minimum width (490 px).<br>
      Expected: Phone and email remain visible, wrapping when needed. A value too
      wide for its line ends with an ellipsis. Hovering shows the complete
      corresponding phone or email, including the text after the cut.
   1. Test case: `add Ravi Kumar -p "+65 6516 2727 ext 21 (office, Mon-Fri 9-5), +65 9123 4567 (HP, after 6pm)" -e e0923841@u.nus.edu`.<br>
      Expected: Both contacts remain visible at default and minimum widths,
      including without a follow-up marker. Add `--id A01234567X` with `edit`
      and check that the ID wraps when necessary without hiding either contact.
   1. Widen the window until both contacts fit.<br>
      Expected: Details return to one line when they all fit. Complete values
      appear without ellipses, and their hover tooltips are removed. Exit and
      reopen TAB; the full contact values remain stored, regardless of how much
      was visible before exiting.
   1. Test case: `add Mei Lin -p 91234567`.<br>
      Expected: The ordinary phone remains readable. Absent email and NUS
      Student ID leave no blank lines.
   1. Add ten students with short names, ordinary phones, email addresses and
      NUS Student IDs. Compare the list at the default 740 by 600 window size
      with a list of phone-only students.<br>
      Expected: Details that fit share one line. These ordinary cards retain
      the compact height; optional fields do not force extra rows.

1. Checking full-width details and height around wrap thresholds

   1. Prerequisites: Use a disposable empty student book.
   1. Add `add Siti Aminah -p 98765432 --id A0234567X -e e0923841@u.nus.edu -t T01 -t "Lab 3" --follow-up`.
   1. Add `add Hafiz Ismail -p 91234567 --id A0123456X -e muhammad.hafiz.bin.ismail@gmail.com -t T01 -t "Lab 3" --follow-up`.
   1. Add `add Priya Nair -p 91234567 -e aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@u.nus.edu`.
   1. Start at the default window size, then resize slowly through 760 px and
      640 px down to the minimum width and back.<br>
      Expected: Every name remains above the contact rows without clipping or
      overlap. A card grows whenever a contact wraps, including Priya's card
      without tags or a flag. Complete clipped values remain accessible on hover.
   1. Add `add Muhammad Haziq -p 91234567 -e muhammad.haziq.bin.abdul.rahman.shah@alumni.u.nus.edu --follow-up`.
   1. Add `add Muhammad Haziq Shah -p 91234567 -e muhammad.haziq.bin.abdul.rahman.shah@alumni.u.nus.edu`.
      The duplicate contact warning is expected.
   1. Compare these last two cards while repeating the resize.<br>
      Expected: Their contact rows have equal widths and wrap or shorten at
      the same points. Details extend beneath the marker, which remains on
      the headline. Repeat with an NUS Student ID on both cards and with a
      short NUS email; toggling follow-up never changes the contact layout.

### Long names on a card

1. Telling apart two names that share a start

   1. Prerequisites: Launch the JAR and widen the window until a card has room
      for a long name.

   1. Test case: `add Mohamed Abdul Rahman bin Ismail -p 91234567`, then
      `add Mohamed Abdul Rahman bin Ahmad -p 92345678`.<br>
      Expected: Both names appear in full, with no name tooltip.

   1. Test case: narrow the window to its smallest width with both students
      listed.<br>
      Expected: The two names stay distinguishable. Each keeps its start and
      its end around the ellipsis as the cards shrink, rather than both ending
      up as the same text, and hovering still shows the whole name.

1. Keeping names readable beside tags and a follow-up marker

   1. Add `add Bernice Xu -p 81234567 -t attendance-low -t consult -t follow-up -t lab -t midterm -t tutorial -f`.
   1. Narrow the window to its minimum width, then widen it slowly.<br>
      Expected: The name retains its opening letters. At the minimum width,
      the count-only chip reads `6 tags`. When one or more chips appear, the
      count reads `N more…`, where N is the number of hidden tags. Hovering
      over the count lists all hidden tags.
   1. Remove the follow-up marker with `edit INDEX -nf`, using the new student's
      index, then resize again.<br>
      Expected: The name remains readable and any hidden tags remain counted.

1. Reading a short name cut by a narrow card

   1. Add `add Siti Nur-Aisyah Hassan -p 91234567 -t T01 -t "Lab 3" -f` and
      `add Siti Nur-Aisyah Hamid -p 98765432 -t T02 -t "Lab 4" -f`.
   1. Narrow the window to its minimum width and hover over each shortened name.<br>
      Expected: Each tooltip shows the corresponding complete name.
   1. Widen the window until both names fit.<br>
      Expected: Each name appears in full and its tooltip is removed.

1. Leaving a name that nearly fits alone

   1. Test case: `add Jonathan Chuah Wei Ming -p 93456789`.<br>
      Expected: The card shows the whole name when it has sufficient width.

1. Returning unused count space to a short name

   1. Add `add Tan Ah Kow -p 94445555 -t attendance-low -t consult -t "Project A/B" --follow-up`.
   1. Narrow the window to about 520 px.<br>
      Expected: Where tags collapse to `3 tags` and the name plus count fit,
      `Tan Ah Kow` appears in full. No unused chip allocation leaves the name
      shortened beside a blank gap. Hover over the count to read all three tags.
   1. Repeat with `Siti Aminah` and `Lee Wei Jie`, then remove the follow-up
      marker and resize again. Names use the room released by the marker.

1. Reading every tag that fits

   1. Add `add Alex Yeoh -p 87438807 -t T01 -t "Lab 3" -t W5 -t consult`.
   1. Widen the window to about 1400 px.<br>
      Expected: All four chips appear in alphabetical order, without a count.
   1. Add `add Charlotte Oliveiro -p 93210283 -t "Tutorial Group 01"`, then
      `add David Li -p 91031282 -t "Tutorial Group 02"`.<br>
      Expected: Both tag names appear in full, including their distinct endings.
   1. Add `add Irfan Ibrahim -p 92492021 -t "CS2103T Tutorial W09" -t "CS2103T Tutorial W10"`.
   1. Narrow and widen the window repeatedly.<br>
      Expected: Chips that fit appear in full; hidden tags are counted and
      listed on hover. A lone chip too wide for its allocation is shortened
      and shows its complete tag on hover. Widening restores complete tags.

### Reading a clipped email address

1. Add `add Siti Nur-Aisyah -p "6516-2727 ext 21" -e siti.nuraisyah.binte.ahmad@gmail.com -t T1 -f`.
1. Narrow the window until the email is cut and hover over it.<br>
   Expected: The tooltip shows `siti.nuraisyah.binte.ahmad@gmail.com` in full.
1. Widen the window until the address fits.<br>
   Expected: The whole address is visible and the tooltip is removed.
1. Clear the email with `edit 1 -e ""` (use the new student's actual index).<br>
   Expected: The email label disappears without a placeholder or a tooltip.

### Long value-option names

1. Adding and editing with long aliases

   1. Prerequisites: Launch the JAR, run `clear`, and reply `yes` if
      confirmation is requested.

   1. Test case: `add Alice Tan --phone 91234567 --email alice@example.com
      --tag T1 -t "Lab 3"` as one line.<br>
      Expected: Alice is added with the given phone, email, and both tags. The
      quoted tag retains its space, and the short and long tag spellings work
      together.

   1. Test case: `edit 1 --name "Alice Lim" -p 81112222
      --email alice.lim@example.com --tag T2` as one line.<br>
      Expected: Every named field changes. The unmentioned `T1` and `Lab 3`
      tags are removed because an edit replaces the tag set.

   1. Test cases: `edit 1 --email ""` and then `edit 1 --tag ""`.<br>
      Expected: The first command removes Alice's email, and the second removes
      every tag. No other field changes.

1. Treating aliases as one logical field

   1. Prerequisites: Continue with Alice at index 1 and note all her details.

   1. Test case: `edit 1 -p 81112222 --phone 82223333`.<br>
      Expected: No change. TAB reports that the phone was given more than once,
      even though the two values use different aliases.

   1. Test case: `add Bob Lim -p 92345678 --phone 93456789`.<br>
      Expected: Bob is not added. TAB reports the same duplicate-phone problem.

   1. Test case: `edit 1 -t T1 --tag "Lab 3"`.<br>
      Expected: The edit succeeds with both tags because a tag option may
      repeat, including under different aliases.

1. Rejecting malformed long options without changing data

   1. Prerequisites: Note Alice's details and the contents of `data/tab.json`.

   1. Test cases: `add Bob Lim --phone`, `add Bob Lim --unknown value`,
      `edit 1 --phone`, and `edit 1 --unknown value`.<br>
      Expected: Neither Bob nor any edit is saved. Each missing value names
      `--phone PHONE`; each unknown option names `--unknown`. Alice's details
      and `data/tab.json` remain unchanged.

1. Joining option values with an equals sign

   1. Prerequisites: Continue with Alice at index 1.

   1. Test case: `edit 1 --name="Alice Tan" -p=81112222
      --email=alice@example.com --id=a01234567x --tag="Lab 3"` as one line.<br>
      Expected: Every field changes. The name and tag retain their spaces, and
      the NUS Student ID is stored as `A01234567X`.

   1. Test case: `tag 1 --add=needs=review`.<br>
      Expected: The tag `needs=review` is added; only the first equals sign
      separates the option from its value.

   1. Test cases: `edit 1 --email=`, `edit 1 --id=`, and
      `edit 1 --tag=`.<br>
      Expected: No field changes. Each command reports that its option needs a
      value, because an unquoted empty value may be an accidental omission.

   1. Test case: `edit 1 --email="" --id="" --tag=""`.<br>
      Expected: The email, NUS Student ID, and all tags are cleared. No other
      field changes; quoting makes each empty value deliberate.

   1. Test case: `edit 1 --phon=1`.<br>
      Expected: No field changes. TAB reports that there is no `--phon` option,
      naming the mistyped marker without its value.

   1. Test cases: `edit 1 --follow-up=yes` and `edit 1 -nf=1`.<br>
      Expected: No field changes. TAB reports that the named option takes no
      value and should be used on its own.

### NUS Student IDs

1. Adding optional NUS Student IDs with either option spelling

   1. Prerequisites: Launch the JAR from an empty folder, then run `clear` and
      reply `yes` if confirmation is requested.

   1. Test case: `add Alice Tan -p 91234567 --id a01234567x`<br>
      Expected: Alice is added. Her card and the command result show
      `A01234567X`, with the lowercase input normalized to uppercase. Her saved
      record has `"nusStudentId": "A01234567X"`.

   1. Test case: `add Bob Lim -p 92345678 -i A12345678Y`<br>
      Expected: Bob is added with `A12345678Y`; `-i` behaves the same as
      `--id`.

   1. Test case: `add Daryl Goh -p 94567890 --id ABCDEFGHIJKLMNOPQRSTUVWXYZ123456`<br>
      Expected: Daryl is added with the 32-character NUS Student ID.

   1. Test case: `add Cara Ng -p 93456789`<br>
      Expected: Cara is added without a blank placeholder on her card and her
      saved record has no `nusStudentId` key.

1. Rejecting invalid, missing, and repeated values

   1. Prerequisites: Continue from the preceding test and keep a copy of
      `data/tab.json`.

   1. Test cases: try adding a differently named student with each of
      `--id ""`, `--id A0123-456X`, `--id A0123_456X`,
      `--id "A0123 456X"`, `--id A0123!456X`, and
      `--id ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567`.<br>
      Expected: No student is added. Each message identifies the NUS Student ID and
      explains whether it is empty, longer than 32 characters, or contains a
      character other than an ASCII letter or digit. The JSON file matches the copy.

   1. Test cases: `edit 1 --id` and
      `edit 1 --id A11111111A -i A22222222B`.<br>
      Expected: Alice stays unchanged. The first message says that
      `--id NUS_STUDENT_ID` needs a value; the second reports the same field as
      repeated, because the long and short spellings are aliases.

1. Preserving, replacing, and clearing an NUS Student ID

   1. Prerequisites: Continue with Alice at index 1 and note her other details.

   1. Test case: `edit 1 -p 81112222`<br>
      Expected: Alice's phone changes and her NUS Student ID remains `A01234567X`.

   1. Test case: `edit 1 -i A76543210Y`<br>
      Expected: Alice's NUS Student ID becomes `A76543210Y` and no other detail changes.

   1. Test case: `edit 1 --id ""`<br>
      Expected: Alice's NUS Student ID disappears from her card, feedback, and saved
      JSON record. All other details remain unchanged. Repeating the command
      succeeds and still changes nothing else.

   1. Test case: First restore Alice's ID with `edit 1 -i A01234567X`, then run
      `edit 1 --id "   "`.<br>
      Expected: The whitespace-only value is trimmed and Alice's NUS Student ID
      is cleared again.

1. Finding and persisting NUS Student IDs

   1. Prerequisites: Run `edit 1 -i A01234567X`, then exit and relaunch TAB from
      the same folder.

   1. Test cases: `find --id a0123` and `find --id 23456`.<br>
      Expected: Each search shows Alice, demonstrating partial,
      case-insensitive NUS Student ID matching. The ID remains after the restart.

   1. Exit TAB. Back up `data/tab.json`, then give otherwise valid records a
      `"nusStudentId": null` value and no `nusStudentId` key respectively. Relaunch TAB.<br>
      Expected: Both records load without an NUS Student ID.

   1. Exit TAB and change one `nusStudentId` value to `A0123-456X`, then relaunch.<br>
      Expected: TAB shows a startup error identifying `data/tab.json`, the
      affected student record, and the invalid NUS Student ID. The main window
      does not open. Dismiss the dialog with `Close TAB`; the invalid file
      remains byte-for-byte unchanged. Restore the backup with TAB closed,
      then relaunch and check that the backed-up records load.

1. Checking duplicate identity behavior

   1. Prerequisites: Start from an empty book.

   1. Test cases: add `add Alice Tan -p 91234567 -i A01234567X`, then
      `add Alice Tan -p 92345678 -i A76543210Y`, then
      `add Alice Tan -p 93456789`.<br>
      Expected: All three students are added because the matching name is not
      an identity key, and the NUS Student IDs differ or are absent. The second
      and third additions each warn that other students have the same name;
      the third result groups both matching records into one name warning.

   1. Test case: `add Bob Lim -p 94567891 -i a01234567x`<br>
      Expected: The addition is rejected. The error names NUS Student ID
      `A01234567X` and student `Alice Tan`, and none of the existing records changes.

   1. Test case: Run `list`, note the third Alice's index, then run
      `edit INDEX -i A01234567X` with that index.<br>
      Expected: The edit is rejected. The error names NUS Student ID
      `A01234567X` and student `Alice Tan`, and all three Alice records keep their
      earlier details.

   1. Test case: Note the second Alice's index, then run
      `edit INDEX -n "Alice Tan" -p 95678912` with that index.<br>
      Expected: The edit succeeds because her NUS Student ID remains distinct;
      sharing the same name does not block the edit. The unchanged name is not
      warned about again, and the new phone matches no other record.

1. Warning about likely duplicate students

   1. Prerequisites: Start from an empty book and add
      `add Alex Yeoh -p "+65 9123 4567" -e Alex@Example.com -i A01234567X`.<br>
      Expected: Alex is added without a warning because no other student is
      present.

   1. Test case: `add "  aLeX   yEoH  " -p 81112222 -e other@example.com`<br>
      Expected: The second record is saved. The result names Alex Yeoh and the
      matching `name`; case and repeated spaces do not prevent the match.

   1. Test case: `add Bernice Yu -p "+65 (9123)-4567" -e bernice@example.com`<br>
      Expected: Bernice is saved. The result names Alex Yeoh and the matching
      `phone`, because the complete digit sequences match despite punctuation.
      Exit and restart TAB; both phone numbers retain their entered formatting.

   1. Test case: `add Charlie Tan -p ６５９１２３４５６７`<br>
      Expected: Charlie is saved with the full-width digits, and one phone
      warning names both Alex Yeoh and Bernice Yu. Adding
      `add Casey Tan -p 91234567` gives no phone warning: the country code
      digits differ, even though a search for `91234567` would find all four.

   1. Test case: `add Chris Lim -p 82223333 -e alex@example.com`<br>
      Expected: Chris is saved. The result names Alex Yeoh and the matching
      `email`; email letter case is insignificant.

   1. Prerequisites: Clear the book, add `add Dana Lee -p 83334444`, and note
      Dana's index.<br>
      Test case: `edit INDEX -t tutorial` with Dana's index.<br>
      Expected: The edit succeeds without a likely-duplicate warning. Dana's
      unchanged name and phone are not compared with her own earlier record.

   1. Test case: Add `add Evan Lee -p 84445555`, then change Dana's phone with
      `edit INDEX -p 84445555`.<br>
      Expected: Dana's edit succeeds and warns that Evan Lee has the same
      changed phone. Her unchanged name is not checked.

   1. Prerequisites: Clear the book, add four students named `Student One`
      through `Student Four`, each with phone `000`.<br>
      Test case: `add Student Five -p 000`<br>
      Expected: Student Five is saved. One phone warning names Student One and
      Student Two, then says that two others match. The review advice appears
      once rather than after every student.

   1. Test case: Add `add Tan Wei Ming -p 91234567` twice, note the second
      Tan Wei Ming's index, then run `edit INDEX -p 99999999` with that index.<br>
      Expected: Only the selected second Tan Wei Ming's phone changes. The first
      identical record keeps phone number `91234567`.

### Editing a student

1. Editing a name that AB3's prefixes made impossible to type

   1. Prerequisites: Run `add Ravi Kumaran -p 91234567`, then `list`, and note
      the new student's index.

   1. Test case: `edit INDEX -n "Ravi s/o Kumaran"`<br>
      Expected: The name becomes `Ravi s/o Kumaran` in full. The slash is an
      ordinary character, where `n/John p/ Smith` once stored `Smith` as the
      phone number.

   1. Test case: `edit INDEX -n Ravi s/o Kumaran`<br>
      Expected: No change. A name marked by an option takes one token, so the
      message reports that `s/o` belongs to no option.

1. Editing by NUS Student ID

   1. Prerequisites: Run `list` and note one student's NUS Student ID such as
      Charlotte Oliveiro's `A02345678G` in the sample data.

   1. Test case: `edit A02345678G -p 81112222`<br>
      Expected: The phone number of the student holding `A02345678G` changes,
      and the status message identifies them. `edit a02345678g -p 81112222`
      does the same, because matching an ID ignores case.

   1. Test case: `edit A02345678G -n "Edited By Id"`, then `edit A02345678G -n
      "Charlotte Oliveiro"` to restore the name.<br>
      Expected: The command identifies the same student by their ID both times,
      even though the rename takes them off any list a search had narrowed to.

   1. Test case: `find -n Alex` followed by `edit A02345678G -p 81112222`, where the
      search hides Charlotte Oliveiro<br>
      Expected: Charlotte Oliveiro's phone changes even though she is not in the
      displayed list. A complete `edit 1 -p 81112222` in the same state would edit
      Alex instead, because index 1 is the first student the search shows.

   1. Test case: `edit A02345678G --id A09876543Z`<br>
      Expected: The NUS Student ID of the student found by `A02345678G` becomes
      `A09876543Z`, and no other detail changes. Editing it back with
      `edit A09876543Z --id A02345678G` works the same way.

   1. Test case: `edit A01234567X --id A03456789H`, where `A03456789H` is another
      student's ID<br>
      Expected: No change. The message refuses the ID another student already
      holds, the same way an `edit INDEX --id` would.

   1. Test case: `edit A99999999Z -p 81112222`, where no student holds that ID<br>
      Expected: No change. The status message reads
      `No student has NUS Student ID A99999999Z.`

   1. Test case: `edit A0123-456 -p 81112222`<br>
      Expected: No change. The letter-first target that is not a valid ID is
      refused with the field-level reason, so the hyphen is named.

1. Clearing the tags

   1. Prerequisites: Give the student two tags with
      `edit INDEX -t T1 -t "Lab 3"` and check that both appear on the card.

   1. Test case: `edit INDEX -t ""`<br>
      Expected: Both tags disappear and no other field changes.

   1. Test case: `edit INDEX -t`<br>
      Expected: No change. The message names `-t TAG` as needing a value,
      rather than clearing the tags.

   1. Test case: `edit INDEX -t T1 -t ""`<br>
      Expected: No change. An empty value clears the tags only on its own, so
      the message rejects the empty tag.

1. Repeating an edit after a search

   1. Prerequisites: Add `add Alex Yeoh -p 87438807` and
      `add John Doe -p 98765432`, then run `find -n john`.

   1. Test case: Run `edit 1 --follow-up` twice.<br>
      Expected: Both commands flag John Doe, the only student listed. The
      search stays active between the commands, so index 1 keeps pointing at
      John Doe and Alex Yeoh stays unflagged.

   1. Test case: `edit 1 -p 9876543` followed by `edit 1 -p 98765432`.<br>
      Expected: John Doe's phone becomes `9876543` and is then restored to
      `98765432`. Alex Yeoh's phone remains `87438807` throughout.

   1. Test case: Run `add John Smith -p 91112222`, then `find -n john`,
      `edit 1 -n Jack`, then `edit 1 -p 91112233`.<br>
      Expected: The first `edit` renames John Doe to Jack, who no longer
      matches `john` and leaves the displayed list; John Smith shifts up to
      index 1. The second `edit` therefore changes John Smith's phone, not
      Jack's. The User Guide warns that an edit removing a match shifts the
      remaining indices, so the displayed list should be checked before
      repeating a command.

1. Reporting several mistakes at once

   1. Prerequisites: Run `list` and note the list size.

   1. Test case: `edit 0 -p 12`<br>
      Expected: No change. One message names both the index and the phone
      number, rather than only the first fault found.

   1. Test cases: `edit` and `edit INDEX`<br>
      Expected: No change. The first names the missing index and says a field
      to edit is needed; the second asks only for a field.

### Clearing a student's tags with a blank value

1. Removing all tags while preserving the rest of a record

   1. Prerequisites: Add `add Alice Tan -p 91234567 -e alice@example.com
      --id A1234567X -t T1 -t "Lab 1" -f` as one line and note Alice's `INDEX`.
   1. Test case: `edit INDEX -t "   "`.<br>
      Expected: All general tags disappear. Name, phone, email, NUS Student ID,
      and follow-up status remain unchanged, including after restarting TAB.
   1. Test case: Repeat with `edit INDEX --tag=" "` and a copied non-breaking
      space (U+00A0) between the quotes.<br>
      Expected: Success even when tags are already absent, with no other changes.
   1. Test case: `edit INDEX -t " " -t T2`.<br>
      Expected: `A blank tag clears all tags only when it is given on its own.`
      Repeat with a copied U+00A0 space between the quotes; the message is identical.
      No record or saved-file changes.

### Clearing a student's email

1. Removing a mistaken address without removing the record

   1. Prerequisites: Add `add Alice Tan -p 91234567 -e alice@example.com
      -t T1 --follow-up` as one line and note Alice's displayed `INDEX`.

   1. Test case: `edit INDEX -e ""`<br>
      Expected: The email disappears from the card and feedback. Name, phone,
      tags, and follow-up status remain unchanged. Alice's saved record has
      no `email` key, and the email stays absent after restarting TAB.

   1. Test case: Repeat `edit INDEX -e ""`.<br>
      Expected: Success with no other changes, even though the email is absent.

   1. Test case: Restore the email with `edit INDEX -e alice@example.com`, then
      run `edit INDEX -e "   "`.<br>
      Expected: The whitespace-only value is trimmed and the email is removed.

   1. Test case: `edit INDEX -e "" -p 81112222 -t "" --no-follow-up`<br>
      Expected: The email stays absent, the phone changes, tags are removed,
      and follow-up is cleared. All changes survive a restart.

   1. Test cases: `edit INDEX -e`, `edit INDEX -e "" -e alice@example.com`,
      and `edit INDEX -p "" -e ""`.<br>
      Expected: Specific feedback for the missing email value, repeated option,
      or required phone respectively. Neither the student
      nor the JSON file changes. Restore an email with
      `edit INDEX -e alice@example.com` and check that it is displayed again.

### Recording email addresses with symbols

1. Test case: `add Mei Tan -p 91234567 -e mei__tan@hotmail.com`.<br>
   Expected: The student is added with both underscores in the email.
1. Test case: `add Sean O'Brien -p 82345678 -e sean.o'brien@example.com`.<br>
   Expected: The student is added with the apostrophe in the name and email.
1. Find Mei's displayed index and run `edit INDEX -e mei_@example.com`.<br>
   Expected: The edit succeeds and preserves the trailing underscore. Exit and
   restart TAB; both addresses remain exactly as recorded in the saved file.
1. Test case: `edit INDEX -e mei..tan@example.com` using Mei's index.<br>
   Expected: An Email error explains that dots must separate non-empty groups.
   The record and saved file remain unchanged. Repeat with a leading or
   trailing dot, a space inside the address, and an underscore in the domain.
1. With TAB closed and the data file backed up, change Mei's email to
   `mei__tan@hotmail.com` by hand, then restart.<br>
   Expected: The file loads and preserves both underscores. Repeat with
   `sean.o'brien@example.com`. Changing it to `mei..tan@example.com` instead
   produces a data-file error identifying Email; restore the backup afterward.

### Rejecting an over-long email

1. Refusing an address longer than the limit

   1. Prerequisites: Run `list` and note the first student's details.

   1. Test case: `edit 1 -e` followed by an address of more than 254
      characters, such as `a.` repeated a few thousand times and then
      `a@example.com`<br>
      Expected: No change. The message names the 254-character limit and the
      length actually given, and quotes only the opening characters of the
      value rather than filling the result box with it.

   1. Test case: the same address with a phone of `12` in the same command<br>
      Expected: No change. Both the phone and the email are reported, which
      confirms the length check joins the other field checks instead of
      stopping them.

   1. Test case: an address of exactly 254 characters, such as `a` repeated
      242 times then `@example.com`<br>
      Expected: The edit succeeds. The limit is the longest an email address
      may be, so no real address is refused.

### Deleting a student

1. Deleting a student while all students are being shown

   1. Prerequisites: List all students using the `list` command, with multiple students in the list.
      Note one student's NUS Student ID, such as Charlotte Oliveiro's `A02345678G` in the sample data.

   1. Test case: `delete 1`<br>
      Expected: The first student record is deleted from the list. The status message identifies
      the removed student and shows their details (`Deleted student: NAME; Phone: PHONE...`).

   1. Test case: `delete 0`<br>
      Expected: No student is deleted. The status message shows error details, and the student book
      remains unchanged.

   1. Test case: `delete A02345678G`<br>
      Expected: Charlotte Oliveiro is deleted from the book wherever she appears, and the status
      message identifies her. `delete a02345678g` does the same, because matching an ID ignores case.

   1. Test case: `add "Digit Ida" -p 91234567 --id 1ABC`, then `delete --id 1ABC`<br>
      Expected: The student holding `1ABC` is deleted, and the status message identifies them.
      `delete 1ABC` is refused with the command's format, because a target starting with a digit
      reads as an index.

   1. Test case: `find -n Alex` followed by `delete A02345678G`, where the search hides
      Charlotte Oliveiro<br>
      Expected: Charlotte Oliveiro is deleted from the book, and the status message identifies her.

   1. Test case: `delete A99999999Z`, where no student holds that ID<br>
      Expected: No student is deleted. The status message reads
      `No student has NUS Student ID A99999999Z.`, and the student book remains unchanged.

   1. Test case: `delete 1` followed by a non-breaking space (`U+00A0`),
      pasted from a word processor or a web page, or a full-width space (`U+3000`) typed
      with a Chinese input method (to type a non-breaking space: Option+Space on macOS,
      Alt+0160 on a Windows numeric keypad, or Ctrl+Shift+U then `a0` on Linux)<br>
      Expected: The first student record is deleted, exactly as with an ordinary space.
      TAB does not call the command an invalid format for a character the user cannot see.

   1. Other incorrect delete commands to try: `delete`, `delete 999` (where the number is larger
      than the list size), `delete 1 2`<br>
      Expected: `delete` is refused with the command's format, `delete 999` is refused as past
      the end of the displayed list, and `delete 1 2` names the extra value,
      `Unexpected value after INDEX: "2". delete accepts one index and nothing else.` The
      hint says `delete`, not the `edit`/`tag` advice to put a value after an option, since
      `delete` takes no field values. None of them deletes a student, and the student book
      is unchanged.

1. More test cases to be added.

### The command line

1. Recalling commands and restoring a draft

   1. In a fresh session, type `draft` and press Up and Down.<br>
      Expected: The text remains `draft` because no command has been submitted.
   1. Replace it with `list` and press Enter. Run `find -n "Siti Nur-Aisyah"`,
      then type `add draft` without submitting it.
   1. Press Up twice, then Up again.<br>
      Expected: The line recalls the find command, then `list`, then stays at
      `list`. Each recalled line has matching colours and its caret at the end.
   1. Press Down twice.<br>
      Expected: The find command reappears, then the unsent `add draft` returns.
   1. Submit `addd` twice and press Up without changing the rejected text.<br>
      Expected: The find command appears immediately. Down restores `addd`;
      another Down keeps it. Correcting or recalling input does not execute it.
   1. Type `another draft` and press Up twice.<br>
      Expected: The rejected `addd` is recalled once, followed by the find command.
   1. Run `clear` with at least one student present, then press Enter on the
      empty reply. Press Up.<br>
      Expected: Clear is cancelled without changing any students. Up recalls
      `clear` immediately, without a blank entry. Repeat with spaces as the reply;
      the result is the same.
   1. Close and reopen TAB, type `draft`, and press Up.<br>
      Expected: `draft` stays unchanged; history does not survive a restart.

1. Pasting spreadsheet cells

   1. Copy two adjacent spreadsheet cells containing `Wei Ming` and `Tan`.
   1. Type `add `, paste the cells, then type ` -p 91234567`.<br>
      Expected: `Wei Ming` and `Tan` remain separate words, with matching
      command colours. A trailing line break copied by the spreadsheet can
      leave an extra space before `-p`; this does not change the command.
   1. Press Enter.<br>
      Expected: One student named `Wei Ming Tan` is added.
   1. Paste two lines into a selected part of another command.<br>
      Expected: Line breaks become spaces, the selection is replaced, and
      the caret follows the inserted text. Nothing runs before Enter.

1. Pasting a soft line break

   1. Put `Siew Ling` and `Goh` on the clipboard with a soft line break
      between them, as a slide text box holds them when the two lines were
      joined with Shift+Enter. Without slides, copy a vertical tab (`U+000B`)
      between the words: on Linux `printf 'Siew Ling\vGoh' | xclip -selection
      clipboard`, on macOS `printf 'Siew Ling\vGoh' | pbcopy`, on Windows
      `Set-Clipboard -Value "Siew Ling$([char]11)Goh"`.
   1. Test case: type `add `, paste, then type ` -p 91234563`, and press
      Enter.<br>
      Expected: The box reads `add Siew Ling Goh -p 91234563` with the words
      apart, and the student is added as `Siew Ling Goh`.
   1. Repeat with a form feed (`U+000C`), a line separator (`U+2028`), a
      paragraph separator (`U+2029`) and a next-line character (`U+0085`)
      in place of the vertical tab.<br>
      Expected: Each reads as a space in the box, and the name saved matches
      what the box shows.

1. Checking what the colours say

   1. Test case: type `add "Siti Nur-Aisyah" -p 98765432 -t T1` without
      pressing Enter.<br>
      Expected: `add` is bold and in the accent colour, `"Siti Nur-Aisyah"` is
      green, `-p` and `-t` are mauve, and the remaining values are in ordinary
      text.
   1. Test case: type `addd` and press Enter.<br>
      Expected: The whole line turns red and the reply quotes `addd` and
      suggests `add`, without adding anyone. Typing another character takes
      the red off again.
   1. Test case: type `add"Ravi s/o Kumaran" -p 91234567` with no space
      after `add`, then press Enter.<br>
      Expected: The bold command word ends at `add"Ravi`, and `s/o` is an
      ordinary word after it rather than part of the bold word. The reply
      reports the unknown command `add"Ravi`, splitting the line where the
      colours already did.
   1. Test case: type `add Mei Tan --phone=91234567 --tag="Lab 3" -t "Lab 4"`
      without pressing Enter.<br>
      Expected: `--phone=` and `--tag=` are mauve like `-t`, `91234567` is
      ordinary text, and `"Lab 3"` is green like `"Lab 4"`. Pressing Enter
      adds Mei Tan with both tags.

1. Checking a command longer than the line

   1. Test case: type a `find` command long enough to run past the right-hand
      edge, then press `Home`, then `End`.<br>
      Expected: The line scrolls so the caret stays in view at both ends, the
      caret is visible at the end rather than cut off by the edge, and the
      colours stay with the text.
   1. Test case: with that command still typed, press `Ctrl`+`A`.<br>
      Expected: A band is drawn behind the whole command, and every part of
      the command is still readable on top of it.
   1. Test case: click in the middle of the command.<br>
      Expected: The caret lands on the character clicked, not one or two away
      from it.
   1. Test case: with that command still typed, press `End`, then `Backspace`
      three times, then `Ctrl`+`Backspace`, then click in the middle of what
      is left.<br>
      Expected: The caret lands on the character clicked after each of those
      edits, rather than a few characters away from it.
   1. Test case: with that command still typed, drag the right-hand edge of
      the window in to narrow it, then click in the middle of the command.<br>
      Expected: The command still runs to the edge of the line with the caret
      in view, and the caret lands on the character clicked.

1. Checking right-to-left and mixed-script commands in the packaged JAR

   1. Test case: type `add محمد علي حسن -p 91234567`, then try
      `add דוד כהן -p 91234567`, without pressing Enter. Repeat with each name
      in double quotes, with `add Ali محمد דוד Tan -p 91234567`, and with
      `add 陈伟明 李 -p 98123344` and `add José Tan -p 91234567` as controls.<br>
      Expected: Each command stays on one readable line. Repeating the name
      until the command exceeds the available width scrolls horizontally.
   1. Test case: type `find -n محمد`, click between letters, and type `Z`.
      Undo, then try Backspace and Delete at that position. Repeat with
      `find -n דוד` and move through each word using Left and Right.<br>
      Expected: The caret marks the insertion or deletion position, including
      after clicks and keyboard movement inside each RTL word.
   1. Test case: use Shift+Left/Right and mouse dragging to select part of a
      name, then a range across Latin and RTL words in the mixed-script case.
      Repeat after scrolling and narrowing the window.<br>
      Expected: Only selected characters are highlighted, even when the
      selection occupies separate visual segments. Replacing the selection
      changes exactly those characters.

### The summary beside the list

1. Checking that the summary follows the list

   1. Prerequisites: several students, at least one flagged and several
      sharing a tag.
   1. Test case: `list`<br>
      Expected: The panel's total matches the number of cards. Each tag in use
      is listed with its count, most used first, and tags with equal counts
      are in alphabetical order whatever case they were typed in.
   1. Test case: `find -n Alex`<br>
      Expected: The counts fall to cover only the students now listed. The
      total reads the number of matches.
   1. Note: the sample data ships no flagged student, so flag one with
      `edit 1 -f` before checking the flagged count.
   1. Test case: `clear`, then `yes`.<br>
      Expected: The flagged count and the total read zero, and the tag list is
      empty rather than the panel disappearing.

1. Counting case variants of one tag

   1. Prerequisites: an empty student book, reached with `clear`, then `yes`.
   1. Test case: `add Ann Lee -p 91234567`, `add Ben Tan -p 91234568`,
      `add Cai Wei -p 91234569`, then `tag 1 -a T1`, `tag 2 -a t1`,
      `tag 3 -a T1`.<br>
      Expected: The TAGS section holds one row, `T1` with the count `3`,
      rather than a row per spelling.

1. Distinguishing long tags

   1. Test case: `add Eve Koh -p 81112222 -t consultation-week-3`, then
      `add Fay Lo -p 81112223 -t consultation-week-3 -t consultation-week-4`.<br>
      Expected: The sidebar shows both complete tag names on wrapped lines,
      with counts `2` and `1` beside the correct names. Neither row overlaps
      the next. Widening the window is not needed to read the endings.
   1. Test case: Add enough distinct long tags to fill the sidebar.<br>
      Expected: The tags scroll vertically and the student total stays visible.

### Student counts in list and find feedback

1. Accurate student counts in command feedback

   1. Prerequisites: An empty student book. Run `clear` and confirm with `yes`
      if any students remain.

   1. Test case: `list`<br>
      Expected: Feedback reports `Listed 0 students.` and the displayed list is empty.

   1. Test case: `find -n Alex`<br>
      Expected: Feedback reports `Found 0 students.` and no students are displayed.

   1. Test case: `add "Amy Bee" -p 85355255 -e amy@example.com`, then `list`<br>
      Expected: Feedback reports `Listed 1 student.` with proper singular wording.

   1. Test case: `find -n Amy`<br>
      Expected: Feedback reports `Found 1 student.`.

   1. Test case: `add "Bob Choo" -p 88888888 -e bob@example.com`, then `list`<br>
      Expected: Feedback reports `Listed 2 students.` with plural wording and accurate count.

   1. Test case: `find -e example.com`<br>
      Expected: Feedback reports `Found 2 students.`.

   1. Test case: `delete 1`, then `list`<br>
      Expected: Feedback reports `Listed 1 student.`.

### Locating equivalent name spellings and Unicode case variants

1. Searching copied roster names without changing their stored spelling

   1. Prerequisites: In a disposable data folder, add `add Sean O’Brien -p 87654321`,
      `add Nguyễn Văn An -p 81234567`, `add José Rivera -p 81234568`, and
      `add ΝΙΚΟΣ -p 91234567 -t ΟΣ`.

   1. Test cases: `find -n O'Brien`, `find -n nguyen`, and `find -n jose`.<br>
      Expected: Each finds its respective student. Curly apostrophes and accents
      remain on the cards. `find -n missing` returns `Found 0 students.`.

   1. Test cases: `find -n Σ`, `find -n σ`, `find -n ς`, and `find -t οσ`.<br>
      Expected: Each finds ΝΙΚΟΣ. `tag 1 -a οσ` reports
      `This student already has tag [ΟΣ]. No changes made.`.
      `tag 1 -r οσ` removes the tag; `find -t ος` no longer finds it.

   1. Test case: `add Sean O'Brien -p 83334444`.<br>
      Expected: The add succeeds with a possible duplicate **name** warning for
      Sean O’Brien. Likewise, adding `JOSE RIVERA` or `νικοσ` with a distinct
      phone warns about the respective existing name. These remain hints, not
      grounds to reject a student.

   1. Test case: Restart TAB and repeat the name searches.<br>
      Expected: Original spellings survive saving and reloading; normalized
      comparisons still find the students. Run `list` to restore the full list.

### Locating students by digits in formatted phone numbers

1. Full numbers, partial numbers, and unchanged saved formatting

   1. Prerequisites: In a disposable copy of TAB, clear all students (`clear`, then `yes`).
      Run `add Alex Yeoh -p "+65 9123 4567"` and `add Bernice Yu -p 9876-5432`.
      Restart TAB and keep a copy of `data/tab.json`.
   1. Test cases: `find -p 91234567`, `find -p 91234`, and `find -p 6591234567`.<br>
      Expected: Only Alex Yeoh is shown, with `Found 1 student.` and phone `+65 9123 4567`.
   1. Test cases: `find -p 9123-4567` and `find -p +6591234567`.<br>
      Expected: Only Alex Yeoh is shown, with `Found 1 student.`, even though the stored phone
      is written `+65 9123 4567`. A keyword of digits and phone punctuation compares by the
      digits alone.
   1. Test cases: `find -p "9876 5432"`, `find -p "76 54"`, and `find -p 98765432`.<br>
      Expected: Only Bernice Yu is shown, with `Found 1 student.` and phone `9876-5432`.
      Repeat the spaced full number with a copied non-breaking space (U+00A0) or narrow
      non-breaking space (U+202F) between the groups; both find Bernice.
   1. Test case: `find -p "9876 5432" -n Alex -f`.<br>
      Expected: No students are shown, with `Found 0 students.`; the name and status still apply.
   1. Test cases: `find -p "ext 21"` and `find -p " + ( ) - . "`.<br>
      Expected: No students are shown, with `Found 0 students.`; annotations and punctuation-only
      terms do not become digit-only searches.
   1. Test case: `find -p 91234567 -n Bernice`.<br>
      Expected: No students are shown, with `Found 0 students.`; both terms are required.
   1. Test cases: `find -p 91234568` and `find -p 9123-4568`.<br>
      Expected: No students are shown, with `Found 0 students.`.
      `data/tab.json` is identical to the saved copy after all these searches.

1. Equivalent Unicode digits and unchanged saved values

   1. Prerequisites: In a disposable copy of TAB, clear all students (`clear`, then `yes`).
      Run `add Wang Fang -p ９１２３４５６７` and `add Lim Jia Hui -p 87654321`.
   1. Test cases: `find -p 91234567` and `find -p 9123`.<br>
      Expected: Only Wang Fang is shown, with `Found 1 student.` and phone `９１２３４５６７`.
   1. Run `edit 1 -p "+６５ ９１２３ ４５６７"`, restart TAB and copy `data/tab.json`.
   1. Test cases: `find -p 91234567`, `find -p 6591234567` and `find -p ٩١٢٣`.<br>
      Expected: Only Wang Fang is shown, with `Found 1 student.` and phone `+６５ ９１２３ ４５６７`.
   1. Test cases: `find -p ８７６５` and `find -p "８７６５ ４３２１"`.<br>
      Expected: Only Lim Jia Hui is shown, with `Found 1 student.` and phone `87654321`.
   1. Test case: `find -p 91234568`.<br>
      Expected: No students are shown, with `Found 0 students.`.
      `data/tab.json` is identical to the saved copy after all these searches.

### Locating students with quoted phrases and error handling

1. Quoted phrases, mixed terms, and unclosed quotes

   1. Prerequisites: Clear all students (`clear`, then `yes`). Add two students:
      `add "Alice Tan" -p 91111111 -t "Lab 3"` and
      `add "Bob Lee" -p 93333333 -t Lab`.

   1. Test case: `find -t "Lab 3"`<br>
      Expected: Matches only `Alice Tan` (`Found 1 student.`). `Bob Lee` is excluded because
      the phrase "Lab 3" does not match separate "Lab" and "3" fragments.

   1. Test case: `find -n Bob -t "Lab 3"`<br>
      Expected: No students match (`Found 0 students.`); neither has both required terms.
   1. Test case: `find -n Alice -t "Lab 3"`<br>
      Expected: Only `Alice Tan` matches (`Found 1 student.`), with the terms in different fields.

   1. Test case: `find -t "Lab 3`<br>
      Expected: Error reported: `A quoted value is never closed. Add the closing " or remove the opening one.`
      The displayed student list remains unchanged.

   1. Test case: `find -t "Lab  3"` (two spaces inside the quotes)<br>
      Expected: Matches `Alice Tan` (`Found 1 student.`), because search terms are normalized
      like stored field values, collapsing interior whitespace runs.

   1. Test case: `find -t ""` or `find -t "   "`<br>
      Expected: Error reported: `A search term is empty. Remove the empty quotes or put a value between them.`
      The displayed student list remains unchanged.

   1. Test case: `find -n Alice -t ""` or `find -n Alice -t "   "`<br>
      Expected: Error reported: `A search term is empty. Remove the empty quotes or put a value between them.`
      The displayed student list remains unchanged.

   1. Test case: `find` or `find    ` (with no search terms)<br>
      Expected: Error reported: `Invalid command format! ...` followed by find usage instructions.
      The displayed student list remains unchanged.

### Finding students within selected fields

1. Field isolation and required combinations

   1. Prerequisites: Use a disposable copy of TAB. Clear all students (`clear`, then `yes`).
      Add `add Jason Lim -p "+65 9123 4567" --id A01234567X -e jason@nus.edu -t "lab tutorial" -t friend`,
      `add Jason Zhou -p 93333333 -t "lab 5" -t "tutorial 3"`,
      `add Jason Tan -p 94444444 -t lab` and
      `add Friend Lee -p 95555555 -e friend@example.com`.
      Restart TAB and save a copy of `data/tab.json`.
   1. Test case: `find -t friend`<br>
      Expected: Only Jason Lim, excluding Friend Lee's name and email matches.
   1. Test case: `find -n Jason -t lab -t tutorial`<br>
      Expected: Jason Lim and Jason Zhou. Jason Tan lacks the required tutorial tag term.
      Repeating a selector requires all terms, without requiring different tags.
   1. Test case: `find -t "lab tutorial"`<br>
      Expected: Only Jason Lim; the phrase cannot be assembled across tags.
   1. Test case: `find --id a0123 -p 9123-4567 -e NUS.EDU`<br>
      Expected: Only Jason Lim. Optional email and ID fields that are absent do not match.
   1. Test case: `find --name Jason --tag lab --tag tutorial`, then
      `find -i a0123 --phone 9123 --email nus.edu`.<br>
      Expected: The first search shows Jason Lim and Jason Zhou, and the second only Jason Lim.
   1. Test case: `find -n Jason -t Lab -n Zhou`<br>
      Expected: Only Jason Zhou; both name terms and the tag term are required.

1. Double quotes group phrases; apostrophes remain literal

   1. Prerequisites: Add `add Jason Wei Tan -p 96666666`, then run
      `add O'Brien -p 97777777`. Save a new copy of `data/tab.json`.
   1. Test case: `find -n Jason -n Tan`<br>
      Expected: Jason Tan and Jason Wei Tan; both separate name terms are required.
   1. Test case: `find -n "Jason Tan"`<br>
      Expected: Only Jason Tan; Jason Wei Tan lacks the whole phrase.
   1. Test case: `find -n O'Brien`<br>
      Expected: Only O'Brien. The apostrophe belongs to the name.
   1. Test case: `find -n 'Jason Tan'`<br>
      Expected: An error identifies `Tan'` as an unexpected value. Single quotes do not group words.

1. Rejected input preserves the current results and saved data

   1. Test cases: `find Jason`, `find Jason -t lab` and `find -t lab tutorial`.<br>
      Expected: The first two ask for a field option for every term. The third
      identifies `tutorial` as an unexpected value and explains that a multi-word value needs quotes.
   1. Test cases: `find -t`, `find -t -n Jason`, `find --unknown friend`,
      `find -t ""`, `find -t "   "` and an unclosed quote in `find -t "lab`.<br>
      Expected: An error identifies the missing value, unknown option, empty term or unclosed quote.
      The displayed list is unchanged after each rejected command.
   1. Run `list` and restart TAB.<br>
      Expected: All six students and their original details remain. `data/tab.json`
      is identical to the saved copy after all successful and rejected searches.

### Finding students by follow-up status

1. Status-only and combined searches preserve data

   1. Prerequisites: In a disposable TAB folder, clear students (`clear`, then `yes`).
      Run `add Jason Tan -p 91111111 -t Lab -f`, `add Jason Lim -p 92222222 -t Lab`,
      and `add Alice Lee -p 93333333 -t Tutorial -f`. Restart TAB and save a copy of `data/tab.json`.
   1. Test cases: `find -f` and `find --follow-up`.<br>
      Expected: Jason Tan and Alice Lee, with `Found 2 students.`.
   1. Test cases: `find -nf` and `find --no-follow-up`.<br>
      Expected: Only Jason Lim, with `Found 1 student.`.
   1. Test case: `find -f -n Jason -t Lab`.<br>
      Expected: Only Jason Tan; the status and every field term must match.
   1. Test case: `find -n Jason -nf -t Lab`.<br>
      Expected: Only Jason Lim; the status option may appear between field options.
   1. Test case: `find -nf -t Tutorial`.<br>
      Expected: No students, with `Found 0 students.`.
   1. Test cases: `find -f -nf`, `find -f --follow-up`, `find -nf --no-follow-up`,
      `find -f true` and `find -nf=none`.<br>
      Expected: A clear error for conflicting or repeated status selectors, an extra bare
      value, or a value attached to an option that takes none. The empty result list remains unchanged.
   1. Run `list` and restart TAB.<br>
      Expected: All three students retain their details and follow-up states. `data/tab.json`
      is identical to the saved copy after all successful and rejected searches.

### Confirming before clearing all students

1. Confirmation, cancellation, and keyboard use

   1. Prerequisites: Add at least two students, run `find -n no-such-student`
      so none is visible, and keep a copy of `data/tab.json`.

   1. Test case: Type `clear` and press Enter.<br>
      Expected: The prompt counts every student, including the hidden ones,
      warns that deletion cannot be undone, and ends with `[y/N]`. It uses
      `student` for one record and `students` for several.
      Neither the filtered list nor the JSON file changes.

   1. Test cases: Enter `maybe`, `yellow`, `nope`, `"yes"`, or `exit`.<br>
      Expected: No command executes. Feedback names the accepted replies and
      repeats the prompt. The filter and JSON file stay unchanged.

   1. Test case: Enter ` NO `. Repeat from a new `clear` request with `n`.<br>
      Expected: Cancellation is acknowledged. The JSON matches the copy byte
      for byte. `list` works again and shows all preserved records.

   1. Test case: Enter `clear`, then press Enter without typing a reply.
      Repeat from a new `clear` request with a whitespace-only reply.<br>
      Expected: Cancellation is acknowledged, with the filter and JSON file
      unchanged. This is the default indicated by the capital `N`.

   1. Test case: Enter `clear`, then `y`, pressing Enter each time.
      Repeat with fresh test records and ` YeS `.<br>
      Expected: All students disappear, the saved book is empty, and
      restarting TAB shows an empty list.

   1. Test case: Add fresh records and enter `clear`. Paste `y` followed by
      U+200E LEFT-TO-RIGHT MARK. Repeat with fresh records and U+200B ZERO WIDTH SPACE.<br>
      Expected: Each reply confirms and saves an empty book. Add fresh records and repeat with `n`
      followed by either mark, or the mark alone: each cancels without changing
      the filter or JSON file. A marked `yellow`, or `y es`, remains invalid
      and keeps confirmation pending without saving. Cancel with plain `n`.

   1. Test case: Clear the test records, confirm, then enter `clear` again.<br>
      Expected: TAB reports that the book is already empty. No confirmation
      is pending, so `help` works immediately.

   1. Test case: Add a student, enter `clear`, close TAB before replying,
      and restart it.<br>
      Expected: The student is still present and no confirmation is pending.

### Saving data

1. Refusing a stale second window's save

   1. Prerequisites: Use a disposable TAB home folder. Open two TAB windows
      from that folder before making any changes. Repeat this check once with
      sample data and no saved file, and once with an existing saved book.

   1. Test case: In window A, enter
      `add Alice StorageProbe -p 81230001 --id A1111111X`. In window B, enter
      `add Bob StorageProbe -p 81230002 --id A2222222X`.<br>
      Expected: A reports success. B reports that the student data file changed,
      asks you to close this window and restart, and says no changes were made.
      B retains its previous records and display filter. Its retry is also
      refused. A's saved JSON remains byte for byte unchanged by B.

   1. Test case: Close B, restart it, and search for Alice.<br>
      Expected: Alice is present and Bob is absent. Adding Bob now succeeds.
      Close both windows and relaunch; both students are present. The empty
      `.tab.json.lock` file may remain and does not prevent ordinary saves.

1. Loading a correctly hand-edited data file

   1. Prerequisites: In a disposable copy of TAB, add a recognizable student,
      close every TAB window using this data file, and back up `data/tab.json`.
   1. Test case: Open `data/tab.json` in a text editor, change that student's
      phone to `81234567`, preserve valid JSON, and save as UTF-8. Restart TAB.<br>
      Expected: The student shows the edited phone. Run `list`, close TAB,
      and restart again; the hand-edited value remains. Restore the backup
      with TAB closed when finished.

1. Preserving existing data when a save fails

   1. Prerequisites: Launch TAB, add a recognizable student, exit, and keep a
      byte-for-byte copy of `data/tab.json`. Leave the file readable, but make
      its `data` folder non-writable. On Linux/macOS, run `chmod 555 data` as a
      non-root User. On Windows, remove create/write permission for the test
      account from the folder while retaining read permission.

   1. Test case: Launch TAB and enter `add Bob Tan -p 91234567`.<br>
      Expected: TAB reports that it could not save to `data/tab.json` due to
      insufficient permissions. Bob does not appear in the list because the
      failed `add` is undone in memory. `data/tab.json` still matches the backup
      byte for byte, and no temporary `.tab-*.tmp` file remains.

   1. Test case: Enter `exit`, restore write permission to `data`, relaunch TAB,
      and enter `list`.<br>
      Expected: The recognizable student and every record from the backup are
      present. Bob remains absent because the failed `add` was undone in memory
      and never replaced the last valid data file.

1. Recovering from a failed first save

   1. Prerequisites (Linux): Use a disposable TAB home folder with the packaged
      JAR. In a private mount namespace (for example, start a shell with
      `unshare --user --map-root-user --mount`), enter that folder and run
      `mkdir data`, then `mount -t tmpfs -o size=4096 tmpfs data`.
      This limits only the throwaway data folder; do not fill the real drive.
      Keep TAB and the following commands inside that namespace.

   1. Launch `java -jar tab.jar` and confirm the sample students appear.
      Fill the private filesystem with
      `dd if=/dev/zero of=data/filler bs=4096 count=1`.
      Verify `touch data/probe` succeeds while
      `printf x > data/probe` fails with `No space left on device`, then
      remove only `data/probe`. The directory must remain writable: removing
      directory write access would fail before the old code creates its target
      and would not reproduce this bug.

   1. Test case: Enter `add Mei Tan -p 91234567`.<br>
      Expected: A save error reports `No space left on device` and that no
      changes were made. The sample students remain visible, Mei is absent,
      and `data/tab.json` does not exist, even as an empty file.

   1. Test case: Exit TAB, run `rm data/filler` to free space, and relaunch
      `java -jar tab.jar` inside the same namespace.<br>
      Expected: TAB opens with sample students without file repair. Repeat the
      add, exit, and relaunch; Mei is now saved and present. Exit TAB, run
      `umount data`, and exit the private namespace.

1. Dealing with missing/corrupted data files

   1. Prerequisites: Launch TAB, add a recognizable student, exit, and keep a
      byte-for-byte copy of `data/tab.json`.

   1. Test case: Replace the file contents with `not json format!` and launch
      TAB.<br>
      Expected: An error dialog identifies `data/tab.json`, reports malformed
      JSON with its line and column, shows the affected line with its line
      number, and asks the User to repair or replace it. The main window does
      not open. After dismissing the dialog, the file still matches the
      malformed contents byte for byte.

   1. Test case: Restore the copied multi-student file, remove the comma after
      one student's `"name"` value, and launch TAB.<br>
      Expected: The dialog marks the next line as affected and shows no more
      than one numbered line on either side. Unrelated student records do not
      appear. The recovery instructions and `Close TAB` button remain visible,
      and the malformed file remains byte-for-byte unchanged.

   1. Test case: Put otherwise valid JSON on one line, remove a comma after the
      first 160 characters, and launch TAB.<br>
      Expected: The dialog reports line 1 and the error column. Its excerpt is
      shortened around the error with an ellipsis instead of showing the whole
      line. The recovery instructions and `Close TAB` button remain visible,
      and the malformed file remains byte-for-byte unchanged.

   1. Test case: Replace the file contents with `{}` and launch TAB.<br>
      Expected: The dialog reports that the student list is missing. The main
      window does not open and the file remains byte-for-byte unchanged.

   1. Test case: Restore the backup, append a second `{"students":[]}` after
      the final closing brace, and launch TAB. Repeat with a second `"phone"`
      key inside one student, and then with two top-level `"students"` keys.<br>
      Expected: Each launch reports the malformed location (and repeated key
      when applicable). The main window does not open, and the file stays
      byte-for-byte unchanged. Restore the backup before continuing.

   1. Test case: Replace the file contents with `null` and launch TAB.<br>
      Expected: The dialog reports `The JSON document must not be null.`
      The main window does not open and the file remains byte-for-byte unchanged.

   1. Test case: Replace the file with well-formed JSON containing two student
      records, where the second has a phone value of `12`, and launch TAB.<br>
      Expected: The dialog identifies student record 2, its name, the Phone
      field and the three-digit requirement. The main window does not open and
      the JSON file remains byte-for-byte unchanged.

   1. Test case: In an otherwise valid file, give a student an email longer
      than 254 characters, such as `a.` repeated 130 times followed by
      `a@example.com`, and launch TAB.<br>
      Expected: The dialog identifies the record and Email field, reports the
      actual and allowed lengths, and TAB exits without a stack trace or file
      change.

   1. Test case: Repair the invalid value and relaunch TAB.<br>
      Expected: The main window opens and shows every saved student.

   1. Test case: Replace the file with three valid student records whose email
      values are respectively `""`, `"alice@example.com "`, and absent. Launch
      TAB, enter `list`, then add another student.<br>
      Expected: All three saved students are listed. The first and third have
      no email, and the second shows `alice@example.com` without the trailing
      space. The newly added student and all three existing students remain in
      `data/tab.json`.

   1. Test case: Move `data/tab.json` elsewhere and relaunch TAB.<br>
      Expected: TAB treats the missing file as a first launch and opens with
      sample students instead of showing an error.

   1. Test case: Restore the file, then make the `data` folder one the account
      cannot open. On Linux/macOS, run `chmod 000 data` as a non-root User. On
      Windows, deny the test account permission to list the folder. Relaunch
      TAB.<br>
      Expected: An error dialog identifies `data/tab.json` and says that TAB
      does not have permission to read the file or the folder holding it. The
      main window does not open, and no sample students are shown. Restore the
      folder's permission with `chmod 755 data`, relaunch, and confirm that
      every saved student is back.

   1. Test case: Restore a valid file written before the optional `flag` field
      existed, including its removed `address` keys, and relaunch TAB.<br>
      Expected: TAB loads every student, ignores the old address values and
      treats a missing flag as not needing follow-up.

1. Detecting misspelled data-file keys

   1. Prerequisites: Close TAB and back up a valid multi-student `data/tab.json`.
      In the second student's record, change `nusStudentId` to `nusStudentID`
      while retaining the value and leave the other records unchanged.

   1. Launch TAB.<br>
      Expected: The startup error names `data/tab.json`, the unknown key
      `nusStudentID`, student record 2, and the affected student's name. It lists
      the supported student fields. The main window does not open; dismissing the
      error leaves the file byte-for-byte unchanged.

   1. Restore the backup and repeat with `Email`, `tag` or `Flag` in place of
      the corresponding supported key. Also try an unknown top-level key
      beside `students`.<br>
      Expected: Each unknown key stops loading with a diagnostic identifying
      its location in a student or in the student book; no value is erased.

   1. Correct the spelling and relaunch, then change another student through
      TAB and relaunch again.<br>
      Expected: Every supported value survives the save and relaunch. A legacy
      `address` key is still ignored and does not prevent loading old files.

1. Recovering from a save failure

   1. Prerequisites: Launch TAB with at least three students, make a change
      such as `edit 1 -p 91234567`, and exit so that `data/tab.json` exists.
      Keep a byte-for-byte copy of the file. Make both `data/tab.json` and its
      `data` folder non-writable, then relaunch TAB using a non-root account.
      On Linux/macOS, run `chmod 444 data/tab.json` and `chmod 555 data`. On
      Windows, remove write permission from both the file and folder for the
      test account while retaining read permission.

   1. Test case: Enter `delete 1` twice, then enter `clear` followed by `y`.<br>
      Expected: Each delete reports that TAB cannot save the data. No student
      disappears, and the clear prompt still gives the original student count.
      Confirming the clear also reports the save error and restores every
      student.

   1. Test case: Enter `list`, `find -n Amy`, and `help`, then press q to leave the
      help page and enter `exit`.<br>
      Expected: Each command succeeds, including opening the help page and
      closing TAB. The read-only `data/tab.json` still matches the saved copy
      byte for byte. Restore write permission to both the file and folder before
      continuing to use TAB.

1. Loading files saved with different encodings

   1. Prerequisites: Exit TAB and back up a valid `data/tab.json` containing
      a student named `José Tan` and another recognizable student.
   1. Test case: Save the file as UTF-8 with BOM in an editor, then launch TAB.<br>
      Expected: Both students load, including the accented name. Edit a tag,
      exit and relaunch; both records and the accent remain intact.
   1. Test case: Restore the backup, save it as Windows-1252 (ANSI), then
      launch TAB.<br>
      Expected: The recovery dialog says the file is not valid UTF-8 and
      explains how to resave it. The main window never opens. After dismissing
      the dialog, the file remains byte-for-byte unchanged.
   1. Test case: Reopen the file using Windows-1252 in the editor, save as
      UTF-8 without BOM, and relaunch.<br>
      Expected: Both students load with their names intact.

1. More test cases to be added.
