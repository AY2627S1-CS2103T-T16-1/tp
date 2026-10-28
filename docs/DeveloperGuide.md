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
* `StringUtil.computeEditDistance` implements the published
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

Two of those parts are built from controls of their own, which the Implementation chapter draws beside the behavior they support: [the command box's](#coloring-the-command-line) and [the student card's](#fitting-names-and-tags-on-a-card).

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/ui/MainWindow.java).

The `UI` component,

* executes commands entered by the User using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Student` objects from the model.

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

`LogicManager` writes to storage only when a command changes the student
records, and puts them back if that write fails.
[Rolling back model changes when saving fails](#rolling-back-model-changes-when-saving-fails)
covers how.

`UnknownFlagException` retains the offending option marker separately from its
message, so command-specific guidance does not depend on error-message wording.

Here are the other classes in `Logic` (omitted from the class diagram above) that are used for parsing a command entered by the User:

<puml src="diagrams/ParserClasses.puml" width="600"/>

How the parsing works:

* When called upon to parse a command entered by the User, the `StudentBookParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddCommandParser`). The parser uses the other classes shown above to parse the command and create an `XYZCommand` object (e.g., `AddCommand`). The `StudentBookParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddCommandParser` and `DeleteCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.
* A command's arguments are read in two passes: `CommandTokenizer` splits them into tokens, then `FlagTokenizer` sorts those tokens into the `CliFlag` each belongs to and hands back a `FlagArgumentMap`. [How commands read their arguments](#how-commands-read-their-arguments) covers why.

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
* never leaves a half-written file behind, and never loads part of one it cannot read. The Implementation chapter covers [replacing a data file](#replacing-a-data-file-without-losing-it), [saves from stale windows](#protecting-saves-from-stale-windows), [an unreadable data file](#handling-an-unreadable-data-file), [unknown keys and malformed tags](#rejecting-unknown-keys-and-malformed-tags) and [bad preferences](#recovering-from-bad-preferences).

### Common classes

Classes used by multiple components are in the `seedu.tab.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Revealing a newly added student

`AddCommand` includes the new student in its `CommandResult`, and `MainWindow`
asks `StudentListPanel` to reveal that card only after `Logic.execute` returns
successfully, so a failed save cannot scroll to an unsaved student. Scrolling
leaves focus in the command box and reads the displayed list after the
search-preservation rules have run. It locates the exact saved object, so an
earlier record with identical details is not revealed instead, and skips a
target that has left the displayed list before layout.

English numeric feedback and data-load diagnostics format with `Locale.ROOT`,
and highlight style names lowercase with it, keeping digits and CSS selectors
consistent without touching the JVM locale or student data.

### Keyboard focus order

`MainWindow` handles plain Tab and Shift+Tab on the student page to keep one
cycle: command box, student list, summary tag list, then the reply when it is
visible. The student list stays one Tab from command entry even after a reply
appears, Shift+Tab returns directly, and empty lists stay reachable.
`SummaryPanel.fxml` makes the tag `ScrollPane` focus-traversable and the theme
outlines its focus without changing its layout size. JavaFX supplies arrow and
Home/End navigation for both lists and paging for the student list;
`SummaryPanel` handles Page Up/Down from the tag viewport's height and overflow,
because JavaFX's default `ScrollPane` pages by its arrow-key increment. The
command box keeps its own Up/Down history, and help uses its own pager.

### The help pager

`HelpCommand` returns a command summary and the existing `isHelpRequested` signal.
`MainWindow` routes that signal, F1 and the Help menu to `HelpPage`, which
replaces the whole student page. The hidden page stays intact, so closing help
restores its filter, previous result and any unfinished command; `CommandBox`
then moves the caret to the end and clears JavaFX's automatic selection so the
next keystroke continues the draft. The summary draws its command words and
flag labels from the command and parser definitions. No student data changes.

`HelpPage` is a keyboard pager: Space/f and b move a screenful with one line of
context, Enter/j and k a line, d/u half a screenful, g/G either end, and q/Esc
back to commands, with PgUp/PgDn, arrows and Home/End also accepted. The
reference is drawn as spans of `Text` in a wrapped `TextFlow` inside a
scrollbar-free viewport that takes the remaining window height. `HelpText` gives
each span the class its token asks for — heading, command word, option,
placeholder or plain — reusing the command box's own colors, so a marker reads
the same where it is described as where it is typed. A command word carrying
punctuation stays plain, being named in a sentence rather than shown as syntax,
and a two-letter capital such as `ID` is prose rather than a field. Page
movement uses the current viewport and the font those spans share, so resizing
and different fonts do not skip text.

A persistent footer shows progress and the navigation keys. `c` copies
`HelpCommand.USER_GUIDE_URL` and confirms it there, which is how the long
address is offered rather than wrapped into the reference; the reference keeps
its explicit lines short enough for the minimum width. An unused typed key
shows a reminder to quit help first. Letter commands are handled on
`KEY_TYPED`, consuming `q` before focus returns so quitting cannot insert a
stray character; Enter navigates on `KEY_PRESSED` and its typed newline is
consumed without changing the notice or scrolling again, as is the empty typed
event macOS sends for F1.

### Shared parsing helpers

`CommandTokenizer` defines the syntax characters for both the parser and the
highlighter, so quoting, escaping and attached option markers stay consistent;
each keeps its own scanning loop because highlighting must also accept
unfinished input. `ParserUtil.parseFollowUpSelector` decodes the optional
follow-up state for find and edit alike. Edit runs its duplicate-option check
first, then the selector, then field errors, preserving that priority. Email
and ID edits share clear-or-parse handling while keeping their own validator
and descriptor setter, and add and edit share
`StudentIdValidation.requireNoDuplicate`, with edit letting the replaced record
keep its ID.

### Restoring window geometry

`MainWindow` supplies each screen's visual bounds to `WindowBounds`, with the
primary screen first. Equal overlap therefore favors the primary screen.
Restoration chooses the screen with the greatest overlap with the saved window,
preserving valid secondary-screen positions, including negative coordinates.
With no overlap or no saved position, it centers on the primary screen. It clamps size and position to that screen's usable area;
nonpositive or nonfinite dimensions use the default size. The window minimum
also yields to a smaller usable area so it cannot defeat the size correction.
The normal minimum height is 400 logical pixels, leaving room for window
decorations and desktop panels within the 480 logical pixels available at
1280×720 with 150% scaling. The student list and help text scroll within the
remaining space, keeping the summary total, help footer and status bar visible.
A window spanning adjacent displays is fitted entirely onto the selected screen,
even when it was visible across both. This keeps restoration predictable when
displays have different usable heights or gaps between them.

### Fitting names and tags on a card

A card is a `StudentCard` holding a `CardHeadline` and a `ContactRow`, and the
headline in turn holds a `CardName` and a `TagRow`. These four controls extend
JavaFX classes directly rather than `UiPart`, and `StudentListCard.fxml`
composes them.

<puml src="diagrams/StudentCardClasses.puml" alt="Student card controls" width="330" />

`CardHeadline` prefers a readable start of the name when allocating room to
tags, but yields part of that allowance for a complete tag count. The name
retains space for a character from each end. When no tag chip fits, `TagRow`
uses the shorter `N tags` total instead of `N more…`, preserving a complete
count beside the name. Its tooltip still lists every hidden tag. A partial
chip requires room for four characters and an ellipsis, measured in its
styled font with padding, so a two-letter fragment gives way to the total.
A shorter tag can still appear in full.
`TagRow.computeFittedWidth` reports the space its chosen chips or count actually use,
so `CardHeadline` returns any unused allocation to the name before laying it
out. The name requests its full natural width.

### Keeping contact details readable

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

### Coloring the command line

`CommandBox` owns a `CommandHistory`, a `CommandTextField` and a
`HighlightedTextOverlay`. The overlay is what calls the static
`CommandHighlighter.highlight` method to color the input, which leaves
`CommandBox` with running the command, [recalling earlier
ones](#recalling-session-commands) and marking a failure.

<puml src="diagrams/CommandBoxClasses.puml" alt="Command box controls" width="500" />

`CommandTextField` converts separators to spaces during text replacement,
before JavaFX filters control characters, asking `StringUtil#isWhitespace` the
question the tokenizer asks of each character. Otherwise the box could hide a
boundary the parser reads: JavaFX deletes a vertical tab or form feed with the
other control characters, and draws a line, paragraph or next-line separator
with no width, each joining the words around it on screen. Pasted word
boundaries survive while the command stays on one line, including when a paste
replaces a selection, and CRLF counts as one separator.

`CommandHighlighter` breaks a command where the parser breaks it, which the
User Guide invites the User to rely on. Two splits therefore cannot follow the
value rule. `StudentBookParser` ends the command word at the first separator
whatever the quotes, so `add"Ravi s/o Kumaran"` is the command word
`add"Ravi` and two words after it; the value rule would draw one bold quoted
word and then report an unknown command the colors never showed.
`FlagTokenizer` reads `--tag="Lab 3"` as an option and a value, so the marker
and equals sign take the option color and the rest its value color. A marker
holding a quote or backslash is left whole, because either moves the equals
sign the parser counts to.

A `TextField` cannot color its own content, so `CommandBox` keeps the field for
input and hands `HighlightedTextOverlay` the nodes that draw a colored copy
behind it, with the field's own text transparent. The overlay owns that copy,
the caret and the selection band. The field still takes every keystroke and
still decides which character a click lands on, so editing is unchanged.

`CommandHighlighter` returns spans covering every character. It walks the
command itself rather than calling `CommandTokenizer`, which removes quotes and
may refuse an unclosed one that is merely half-typed, but asks the tokenizer
the questions the two must agree on — `CommandTokenizer#isEscapable` and
`StringUtil#isWhitespace` for where a token ends. It rejects nothing: a command
is colored while still half-typed. The copy measures its width from the whole
shaped `TextFlow`, rounded up so it cannot wrap at a fractional pixel, and
ordinary value words share a text node with their spaces so splitting an
unquoted right-to-left name does not change shaping or click positions.

Three things must then agree with the field, because the User reads the copy
but interacts with the field.

* **Scroll offset.** The copy is drawn wherever the field draws its own text,
  read from the field's text node rather than recomputed. A field scrolls by
  rules that differ between typing, deleting, cutting and undoing, and a copy
  even a pixel out of step shows one character where the field hit-tests
  another. Reading the node once is not enough, because the skin re-scrolls
  after a delete, a replace or a width change, which is after the caret and the
  text have settled; the copy follows the node's own position. It is the node
  the field is drawing in, since an empty line shows the prompt in a node of
  the same kind that the field hides rather than discards.
* **Caret.** The field's caret is drawn in its transparent text color, so the
  line carries one of its own, sized from a run of the text. Its position
  follows the primary caret shape of the field's text node, including the
  leading/trailing bias at script boundaries, because logical prefix widths
  cannot locate RTL insertion points.
* **Selection.** The field's own highlight broke into fragments behind the
  copy, so it is disabled in the stylesheet and drawn here from a
  `TextFlow.rangeShape` path, which covers each visual segment including
  selections that cross RTL/LTR boundaries.

Every color the line uses is held to 3:1 against the selection band, so
selecting a command cannot hide half of it. That is below the 4.5:1 WCAG asks
of text this size, deliberately: on a window this dark, a band light enough to
be seen against the line sits at 2.2:1, and reaching 4.5:1 against it would
mean moving every token color towards white, away from the palette the mockup
sets. Worth revisiting if the palette changes.

### Recalling session commands

`CommandBox` records each submitted command in the `CommandHistory` it owns, before execution,
so rejected commands can be recalled too. While clear confirmation is pending,
replies are excluded because they cannot be reused as standalone commands; the
navigation position resets without recording the reply. The helper stores submissions separately
from the draft captured when Up first leaves the newest position. Down past
the newest submission restores that draft. Up skips the newest submission when
it already matches the text in the box and an earlier entry exists. Blank
submissions and consecutive identical submissions are skipped; editing a recalled entry does not alter stored text.

The command box consumes unmodified Up/Down key presses before the text field
handles them. Recalled text uses the existing text listener for coloring, and
the caret moves to its end. This UI state is session-only and never enters the
student model or persisted data.

### Summarizing the list

`SummaryPanel` shows the flagged count, a count per tag and the total, beside
the list. It counts `Logic#getFilteredStudentList()`, which is the only list
`Logic` exposes, so it summarizes **what the list is showing** rather than the
whole student book: after `find -n Alex` it counts the matches. That is the
reading the User Guide documents, and it keeps the panel answering the same
question as the list it sits beside. Summarizing the whole book instead would
need `Logic` to expose it.

The list is an `ObservableList`, so a listener keeps the counts current
without the panel being told when a command changes anything. The counting
lives in `StudentSummary`, which takes a plain `List<Student>` and owns tag
grouping, representative spelling and count ordering. It is independent of
JavaFX; `SummaryPanel` observes the displayed list and renders those results.

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


### Replacing a data file without losing it

`FileUtil.writeToFile` protects an existing JSON file from a partial save. It
resolves an existing symbolic link to its destination, so replacing the saved
data leaves the link in place. A link whose target is not there yet is followed
too, so a link set up before its file receives the first save, and only a link
pointing into a folder that does not exist is refused — no save can create that
folder without guessing where the records were meant to live. Startup applies
the same rule through `FileUtil.requireResolvableLink`, so such a link is
reported rather than read as a first run. It also refuses a destination that
is not writable, and copies an existing file's POSIX permissions to its
replacement. The new contents are written and forced to a temporary file beside
the real destination before an atomic move replaces it. Keeping both files in
one folder is what makes the atomic move possible, and also means the folder
must allow temporary-file creation. If writing or moving fails, the previous
file remains unchanged and TAB removes the temporary file where it can.
`FileUtil` creates only missing parent directories before writing the temporary
file and never pre-creates an empty destination, so a failed first save
preserves the absence of a book and allows normal sample-data startup. This
applies to student-book and preference writes alike. On POSIX filesystems a new
file keeps the temporary file's owner-only permissions, and an existing file
keeps its own.

`JsonStudentBookStorage` reports a data-folder or file collision and a
save-lock failure at the operation boundary, where the failing path and its
purpose are known. It preserves permission exceptions for the existing
permission-specific feedback and never removes an obstructing file or folder.
Logic rolls back records and the filter after these failures just as it does
for other save errors.

`StorageManager` logs read and write attempts and successful saves at `FINE`,
preference-file operations included. An entry identifies the file path without
copying student or preference contents, and a save is logged as complete only
after the storage delegate returns successfully.

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

The activity diagram below follows one save attempt:

<puml src="diagrams/StaleWindowSave.puml" alt="Stale window save check" width="420" />

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

The following sequence diagram shows a command whose save fails:

<puml src="diagrams/SaveRollbackSequenceDiagram.puml" alt="SaveRollbackSequenceDiagram" />

### Resolving application file locations

`AppUtil#getApplicationDirectory` uses the code-source location to distinguish a
packaged JAR from an exploded Gradle or IDE build. A packaged application stores
`data/tab.json`, `preferences.json`, and `tab.log.*` beside the JAR regardless of
the process's working directory. An exploded build keeps the working directory
as its application directory, preserving the development workflow.

### How commands read their arguments

A command marks each field with an option rather than a prefix attached to the
value:

```
add NAME [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up]
edit TARGET [-n NAME] [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up | --no-follow-up]
```

**Aspect: how a command tells one field from the next**

* **Alternative 1 (current choice):** an option, a whole token of its own, such
  as `-p` or `--phone`.
  * Pros: a value is read as a value whatever it contains, so a name may hold
    any character.
  * Cons: a value with spaces has to be quoted, and the User types a space
    between the option and its value.
* **Alternative 2:** a prefix joined to the value, as AB3 uses, such as
  `p/91234567`.
  * Pros: shorter to type, with no quoting.
  * Cons: a prefix has to be a sequence no value ever contains, and no such
    sequence exists for a name. `s/o`, `d/o`, `a/l` and `a/p` are ordinary
    parts of Singaporean and Malaysian names, and `a/l` was unreadable for as
    long as an address field was marked `a/`. Widening the prefixes moves the
    collision rather than removing it.

Each value option has a long alias: `--name`, `--phone`, `--email` and `--tag`
answer to `-n`, `-p`, `-e` and `-t`. `add` takes its name in the preamble;
`edit` and `find` both read `-n`/`--name`.

`edit` names its target in the preamble or with `--target-id
NUS_STUDENT_ID`; a target option of its own keeps selection unambiguous while
`--id` sets or clears the ID field. [Selecting a
student](#selecting-a-student) covers the targets `edit`, `tag` and `delete`
share.

Parsing runs in two passes, the way a shell does it.

**`CommandTokenizer` splits the arguments.** Whitespace separates tokens and
double quotes group a value that contains it. Straight, curly and full-width
quotes are interchangeable delimiters, including in mixed pairs, so input
methods and pasted documents are accepted; `CommandTokenizer#isQuote` shares
that rule with the highlighter, and every supported quote can be escaped to
keep it literally. A backslash escapes a quote or another backslash, and
before anything else stays in the value, so nothing the User typed disappears
unasked. Each token records whether a quote took part in it, which is the only
thing separating the value `-Ahmad` from the option `-e`.

A token opening with a dash an editor writes for a typed hyphen — U+2010 to
U+2015 and the minus sign U+2212 — opens an option, for the same reason the
quote styles are interchangeable: a pasted command is read the way it was
typed. `CommandTokenizer#isOptionMarker` shares that rule with the highlighter,
so the box and the parser agree on which word is an option, and a token that
opens with a quote keeps the dash it was given. One dash stands for one
hyphen, so an em dash autocorrected from `--` reaches the parser as `-phone`
and is refused by name rather than stored in a field.

An option where a value was expected is read as the option before it having
been left empty: `add John -p 123 -t -e` reports that `-t` needs a value rather
than storing `-e` as a tag. A value that really opens with a hyphen is quoted.

**`FlagTokenizer` sorts the tokens into options.** Tokens before the first
option are the *preamble*, joined with single ASCII spaces, and each command
reads it as whatever it takes there — the name for `add`, so a multi-word name
needs no quotes, and the index for `edit`. Each command names its own through
`AddCommand.FIELD_NAME` and `EditCommand.FIELD_INDEX` rather than leaving it to
the shared `CliFlags`.

| Kind | Behavior |
| --- | --- |
| Value-taking (`-n`, `-p`, `-e`, `--id`, `-t`) | takes the token after it, or a value joined by `=`. `-t`/`--tag` repeats for `add` and `edit`; `tag` takes exactly one through `-a`/`--add` or `-r`/`--remove`. |
| Presence-only (`--follow-up`/`-f`, `--no-follow-up`/`-nf`) | records an empty value and consumes no token, which lets `FlagArgumentMap` tell an absent option from a present marker without a second representation. `add` accepts only `--follow-up`, `edit` both to set or clear, `find` both to filter. An attached value on one is refused specifically. |

Each short and long marker resolves to the same `CliFlag`, so mixed aliases for
a single-valued field are duplicates rather than independent fields. NUS
Student ID uses `--id` as its canonical option with the otherwise unused `-i`
as a typing-friendly alias; giving both is rejected as a repeated field rather
than letting one silently override the other, and usage labels and messages use
the canonical spelling. Duplicate-option feedback names the repeated markers
rather than values, since presence-only options have none, and
`FlagArgumentMap` keeps the first spelling supplied.

An attached value splits on the first `=` only, so later ones stay in the
value, and it is unambiguously part of its option when it opens with a hyphen.
An empty attached value must be quoted, as `--tag=""`, so an accidental
`--tag=` cannot clear data. Because `CommandTokenizer` records whether a token
began with a quote, a fully quoted `"--tag=T1"` stays a value while the quote
in `--tag="Lab 3"` does not hide the marker.

For `edit`, tag values are normalized before validation. One normalized blank
clears every tag, including when the input holds Unicode spaces; a blank beside
other tag values is refused with an explanation that clearing needs the blank
on its own; and non-blank values are still validated, so every problem is
reported without tags being discarded silently.

`add` keeps its name before the options. If the tokenizer refuses `-n` or
`--name`, `AddCommandParser` adds a placement hint to the error, and a missing
name gets the same hint, so a User who knows `edit`'s name option recovers at
once.

The shape of the command is settled before any field is read. An unclosed
quote, an unknown option, or a single-valued option given twice — including
once under each alias — each leave it unclear which field a value belongs to,
so each is refused on its own rather than collected with the field errors. An
unknown option names its marker without echoing an attached value, and a
missing value and a token belonging to no option are each reported for what
they are.

The following sequence diagram shows how a command's arguments are parsed
in two passes:

<puml src="diagrams/CommandArgumentParsing.puml" alt="CommandArgumentParsing" />

`edit` clears a field with an explicitly empty value rather than with a bare
option. `-p ""` removes the phone number, `-e ""` removes the email,
`--id ""` removes the NUS Student ID, and
`-t ""` removes every tag. Phone, email and NUS Student ID values are trimmed
before this check, so quoted whitespace clears any of those optional fields too.
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

### Selecting a student

`edit`, `tag` and `delete` each take a target: a displayed index, a
letter-leading NUS Student ID, or an explicit option. That option is
`--target-id` for `edit`, whose `--id` sets the ID field instead, and `--id`
(alias `-i`) for `tag` and `delete`. Either form builds a `StudentTarget`,
through `ParserUtil.parseRequiredStudentTarget` for a preamble target or
`ParserUtil.parseNusStudentId` for an explicit one; `tag` keeps
`ParserUtil.parseRequiredIndex` for a numeric preamble.

The explicit option admits a digit-leading ID without changing how a bare
number selects a displayed position, so an in-range number stays an index even
when another record holds the same numeric ID. Repeating the option, or giving
a preamble target and an explicit one together, is refused rather than guessed
at, and selecting a target does not count as a field edit.

An index resolves against the displayed list and an ID across the whole book.
A roster ID therefore reaches a student a search has hidden, and a search
predicate survives an edit.

Diagnostics keep the spelling the User typed. `ParserUtil.parseIndex` separates
malformed or non-positive input from a positive integer beyond the supported
range, and `StudentTarget` reports an out-of-range position with that spelling
and the current displayed count, zero included. A non-positive or non-integer
target keeps the concise positive-integer reason alongside any field errors,
and `delete` adds the explicit `--id` form whenever the refused target reads as
a valid NUS Student ID — spelling intact, so a stored numeric ID keeps its
leading zeroes. `edit` and `tag` share `FlagTokenizer.tokenizeWithIndex`, which
reports an unquoted negative number as both an invalid index and an unknown
option, and each reports a value following an otherwise valid target.
`ParserUtil.getTargetFieldName` names that target by the form it was read as,
`NUS_STUDENT_ID` for a letter-leading target and the command's index label
otherwise, so a user who typed a name is not told that the name is an index.
The retained spelling affects diagnostics only, not target equality or hashing.

### Adding or removing one tag

`TagCommandParser` reads `tag TARGET -a TAG` (alias `--add`) or
`tag TARGET -r TAG` (alias `--remove`) with the existing `FlagTokenizer` and
`ParserUtil`. The option names the action: both spellings of each map to
`FLAG_ADD_TAG` or `FLAG_REMOVE_TAG`. Exactly one action option is accepted, so
a conflicting pair and a repeated option are both refused, and `ParseProblems`
collects every target and tag error before a command is built. A tag syntax
failure and a missing action option each carry guidance for addition and for
removal.

`TagCommand` compares normalized tag names ignoring case, then copies the
existing tag set and constructs a replacement student with every other field
preserved. `TagCommand.Action` selects addition or removal while sharing target
resolution, matching, student replacement and feedback. The search filter stays
active, so removing a tag that filter uses may hide the student from the
displayed list.

Case variants of one tag name fold together. `ParserUtil.parseTags` folds them
in `add` and in `edit`'s replacement tags using `String.CASE_INSENSITIVE_ORDER`,
matching the tag command and the summary, while `ParseProblems` retains input
order so the first spelling wins and every invalid tag still gets a diagnostic.
`JsonAdaptedStudent` applies the same ordering after validating and normalizing
stored tags, keeping the first spelling in the JSON array, so loaded cards and
tag summaries agree and the next successful save writes one tag per group. Tag
equality itself is unchanged. Removal takes every matching variant of the
requested group and leaves unrelated tags alone. Duplicate and successful
removal feedback quote a matching stored tag's spelling — the first in the
card's tag display order when several variants match. A general tag named
`Needs follow-up` stays separate from the follow-up flag, though the parsers no
longer let one be added.

For a hidden flagged student, the absent-tag hint supplies
`edit --target-id NUS_STUDENT_ID --no-follow-up` with the student's stored ID.
This addresses even digit-leading IDs without changing the active search.

The following sequence diagram shows how a `tag` command adds or removes a tag:

<puml src="diagrams/TagCommandSequence.puml" alt="TagCommandSequence" />

Invalid indexes, existing tags on addition and absent tags on removal throw
`CommandException` before mutation; invalid syntax or values throw
`ParseException`. Neither path reaches `LogicManager`'s save. A successful
addition or removal uses the normal JSON save path without changing the schema.
Integration tests exercise reloads, human-edited case and whitespace variants,
absent optional fields and unchanged saved bytes on failure.

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
format and non-whitespace control characters that `StringUtil.normalizeFieldValue`
strips from a stored value, through `StringUtil.stripFormatCharacters`. Whitespace controls such as tabs
and line breaks remain separators rather than joining adjacent words. A direction mark or a
zero-width space pasted before a command word or an index is otherwise a word
that looks right but matches no command, or an index reported as not a positive
integer. The zero-width joiner and non-joiner are kept, because a script such as
Sinhala needs them to shape a word, and a command word holding one is a
different word rather than the same word with a character removed.

### Reading a command word in any case

`StudentBookParser` folds the first word of the input to lower case before it
dispatches, so `List`, `LIST` and `list` all reach `ListCommand`. No two
command words differ only in case, so a capitalized word can only mean the one
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

A word TAB does not dispatch on is answered with the word as typed and the
command words it is a likely attempt at. `CommandWords` asks two questions in
order.

**What does the word open?** A part-typed word is right as far as it goes, so
`del` stands for `delete` even though `help` is nearer by edits, at two
against three. Only a word that opens nothing is measured at all.

**What is it near?** `StringUtil.computeEditDistance` counts insertions, deletions,
substitutions and swaps of neighboring characters. A swap costs one edit
because a fast typist makes them as readily as the rest; otherwise `lsit`
would sit two edits from `list`, as far as a wilder guess. The budget is two
edits for every command word whatever its length, which is what the user story
asks for and what reaches the slips a teaching assistant makes: `ls` is two
insertions short of `list`, `dlte` two deletions from `delete`.

Two edits also reach past a short command word's first letter, which is how
`dlt` came to be answered with `edit`. A measured word must therefore open
with the character the command word opens with, unless the single edit between
them is that character — which keeps `felete`, `edlete` and `elete` reading as
`delete`. The first letter is the one a typist is least likely to get wrong.

Every equally likely word is named, in the alphabetical order of
`COMMAND_WORDS`: `eit` is a letter short of both `edit` and `exit`, and
answering with one alone would read as knowledge TAB does not have. Past three
the reply falls back to the pointer to `help`, since a list that long says only
that TAB cannot tell. No typed word reaches that today, `edit` and `exit` being
the only pair sharing a first letter; the limit is there for command words TAB
may gain.

Comparison folds case with the `CommandWords.foldCase` the parser dispatches
with, so `Delte` reads as `delete`, while a correctly spelled word in any case
is dispatched before reaching here. The reply quotes the word as typed, so a
User who typed capitals can recognize what they did, shortened the way a
rejected field value is. Lengths are compared before distances, which settles a
pasted line against every command word without measuring it.

A suggestion is never run: dispatching `delete` for `dlte` would act on a word
the User did not type, and a wrong guess there costs a student record where a
wrong suggestion costs a line of text. `StudentBookParser` raises the same
`ParseException` as for any unknown word, leaving the model, the saved file and
the typed text as they were.

The parser dispatches from a `switch` rather than from `COMMAND_WORDS`, so the
two could disagree and a word missing from the list would work while being
suggested to nobody. `CommandWordsTest` resolves the command words out of the
parser's own cases and compares them against the list.

The activity diagram below follows one typed word:

<puml src="diagrams/CommandWordSuggestion.puml" alt="Command word suggestion" width="470" />

### Follow-up flag

`FollowUpFlag` extends `Tag` and uses its validation, normalization, and display
behavior. Its fixed name is `Needs follow-up`. A general tag with those words
remains a distinct value: tag equality requires the same concrete class as
well as the same normalized name. `ParserUtil.parseTagToAdd` and
`JsonAdaptedTag.toModelType` both refuse that name however it is spelled, so that
a card cannot show the same label as a tag and as the marker whether the value
arrives from a command or from a hand-edited file. A file written before the
rule stops loading, with an error naming the record and the field to correct.
`Tag` itself still accepts the name, so removal reads it as an ordinary tag and
`TagCommand` keeps explaining that the marker is what a flagged student carries. `Student` holds an optional `FollowUpFlag` and
exposes it through `getFlag()`; `isFlagged()` preserves the existing boolean
interface for commands and the UI. Full equality and hashing include the flag,
but `isSameStudent` does not: changing whether a student needs a reply does not
change their identity. The flag is stored as `Optional<FollowUpFlag>`; the constructor
without a follow-up argument defaults to `Optional.empty()`.

Tag searches match ordinary tags as parts of their `getTagName()` values,
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
Only `Boolean.TRUE` creates a `FollowUpFlag` in the model; a missing key, JSON `null`, and
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
consistently with the student card: optional fields (NUS Student ID, phone, email,
tags, follow-up status) appear only when present, with tags ordered alphabetically
ignoring case, avoiding placeholder labels or empty tag markers.

### Field values

Two decisions shape every field a student carries.

**A detail TAB does not know is absent, not blank.** `Student` holds phone,
email and NUS Student ID as `Optional<Phone>`, `Optional<Email>` and
`Optional<NusStudentId>`, and no `Student` field is ever null. Both
constructors require non-null arguments, one taking the follow-up boolean and
the other defaulting to unflagged for sample data. Add, edit, storage
conversion and the test builders all pass optionals into that contract, so
nullable strings stay inside the storage adapter and the file format is
unchanged.

**Two values that look the same are stored the same.** `Name`, `Tag`, `Phone`,
`Email` and `NusStudentId` each keep the form `StringUtil.normalizeFieldValue`
returns. That strips characters the reader cannot see, such as direction marks
left by a copy and paste; collapses each run of spaces to one and trims the
ends; and finally composes the result, so that a letter typed as a base plus a
separate accent is stored as the single character it looks like. Composing
comes last because an invisible character between the two blocks them from
joining. The composed form is Unicode NFC.

The following activity diagram shows the normalization pipeline:

<puml src="diagrams/NormalizeFieldValue.puml" alt="NormalizeFieldValue" />

They normalize for different reasons, and the rule each one enforces differs.

| Field | Why it is normalized | What it must hold |
| --- | --- | --- |
| `Name` | it is displayed and searched as text, so visually identical names should have the same stored form | at least one letter or number |
| `Tag` | it is a key in the set of a student's tags, so two that look alike must not both be stored | at least one letter or number |
| `Phone` | it is displayed beside the others and gains nothing from being stored differently | at least 3 digits |
| `Email` | it is displayed and searched as text, so visually identical addresses should have the same stored form | RFC 5321 address format, max 254 chars |
| `NusStudentId` | it is the sole identity key, so visually identical IDs must have the same stored form | 1-32 ASCII letters/digits, stored uppercase |

Nothing else is rejected in `Name`, `Tag` or `Phone`. A phone number may hold `+`,
whitespace, brackets and an extension; a tag may hold whitespace, hyphens and any
script. None of those characters can hinder TAB, because none of these
fields is parsed, dialled, or used to build a file path.

**Aspect: how long a phone value may be**

* **Alternative 1 (current choice):** no maximum, with the minimum of three
  Unicode decimal digits still applying.
  * Pros: a teaching assistant can record several numbers, extensions and
    contact annotations in one field, and no saved record becomes unloadable.
  * Cons: a 400-digit value is accepted. A long value is shown with an ellipsis
    and its full text on hover, rather than the stored text being cut.
* **Alternative 2:** a fixed cap, as `Email` has at 254 characters.
  * Pros: bounds what the card and the file have to hold.
  * Cons: rejects useful contact details, and a cap below an existing value
    stops that record loading. Email's limit answers its own validation and
    storage concerns and does not transfer.

Worth revisiting if a concrete problem appears, with a migration plan for
existing data and a limit justified by contact use cases.

**The three optional details share one contract.** Phone, email and NUS
Student ID are each an `Optional` on `Student`, and behave alike at every
layer bar one storage detail:

| Layer | Behavior |
| --- | --- |
| `add` | leaves the field absent when its option is not given |
| `edit` | `EditStudentDescriptor` holds an `OptionalFieldEdit<T>` carrying value and intent as one three-state concept: no option keeps the old value, a value replaces it, and an empty value clears it. The parser trims first, so a quoted run of spaces clears too; a bare option is still an error, and a rejected value is a parse problem rather than a clear. Copies and equality work on that state rather than on its representation. |
| Storage | `JsonAdaptedStudent` keeps a nullable string. Jackson omits the key when the detail is absent, and for all three a missing key or JSON `null` loads as absent while a malformed non-null value refuses the file rather than being dropped silently. Phone and email also pass through `normalizeOptionalValue`, so a string that normalizes to nothing loads as absent; NUS Student ID does not, so `"nusStudentId": ""` — and whitespace-only or invisible-mark-only text with it — refuses the file with the non-empty-ID reason. Files written before a field became optional still load. |
| Display | the card leaves the label unmanaged and `Messages.format` omits it, so the remaining details close up with no blank line |
| `find` | a student without the detail matches no term for it |

What differs is the rule each field enforces, and why it may be missing.

**NUS Student ID.** Between 1 and 32 ASCII letters and digits, stored
uppercase, so `e0123456` and `E0123456` are one value. The range covers the
identifiers issued to different groups of students while stopping an
accidentally pasted value from hiding the rest of a card.
`NusStudentId.getFailureReason` performs the single validation scan and
`isValidNusStudentId` delegates to it, keeping the accepted format and the
user-facing error in step without a second regex.

**Email.** `Email` accepts the RFC 5322 unquoted dot-atom local part, so
addresses with apostrophes, repeated symbols or trailing underscores can be
recorded; only dots require non-empty groups on both sides. Quoted local parts
and domain literals stay unsupported. A student is recorded before an address
is known rather than one being invented.

`Email.MAX_LENGTH` caps an entered or loaded address at 254 characters, based
on RFC 5321 section 4.5.3.1. `ParserUtil` and `JsonAdaptedStudent` report that
boundary separately from format failures. `isValidEmail` keeps its independent
format contract, including for direct model construction: it checks local
atoms and domain labels iteratively rather than matching the recursive full
regex. The equivalent `VALIDATION_REGEX` remains available to describe the
existing grammar, and regression tests compare both forms on short inputs.
Long direct inputs therefore cannot exhaust the stack through validation.

**Phone.** A class roster can give a name, an ID and a university email without
a personal mobile number. `Phone` is neither an identity field nor a key: two
students may hold the same number or none, and `isSameStudent` reads neither
case. It is normalized only so that a number typed with unusual whitespace, or
wrapped in the invisible marks a chat app puts around a copied one, is stored
consistently.

`ValueMessages` in `commons.util` owns the invalid-value template and bounded
value rendering. Commands access it through `Messages`, while storage uses it
directly, avoiding a dependency on command-facing logic. It shortens a rejected value past
`MAX_QUOTED_VALUE_LENGTH` characters so a very long one cannot push the reason
out of view. It counts characters through `StringUtil.getFirstCharacters`, as a
reader counts them, separating neither an emoji nor a letter from its accent;
a student card cuts a name or tag with the same helper. Command parsing and
storage share that message, so both report an invalid value the same way.

#### Student names

A name is displayed and searched as text but is not an identity field, so
several students may hold the same normalized name. `Name` keeps the
normalized form described above, with one exception: the zero-width joiner and
non-joiner survive, because scripts such as Sinhala and Arabic need them to
shape correctly. Two names differing only by a joiner are therefore two
students.

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

**A name may hold anything, including a sequence that looks like an option.**
`a/l` and `a/p`, ordinary components of a Malaysian name, are stored and shown
as typed, because a field is marked by an option of its own rather than by a
marker joined to the value. `Name` enforces no restriction beyond holding at
least one letter or number.

**Showing a long name.** A card gives the name one line, shared with the tags
and follow-up marker. `StudentCard` passes the complete name to
`UiText.showToFit` and `CardName` measures it in the displayed font, so a name
appears in full whenever its width permits and widening the card restores what
a cut hid. There is no character limit.

When space runs out, `CardName` shortens from the middle through
`UiText.shortenMiddle`, keeping up to nine characters at the end and
spending the rest of the budget on the opening.
`StringUtil.getFirstCharacters` and `getLastCharacters` keep characters whole,
combining accents and emoji included. Two names can still coincide once their
distinguishing text is hidden, so a shortened name carries a tooltip with the
whole of it, which goes when the name fits again.

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

### Detecting duplicate students

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
identity check scans the complete student book: a matching NUS Student ID stops
the command before it changes the model, while other matching details only warn
the User.

<puml src="diagrams/AddDuplicateCheckActivityDiagram.puml" alt="Add command duplicate check" width="500" />

`ModelManager.addStudent` preserves the active filter. After a successful
addition, `AddCommand` checks the new student against that filter. A matching
student appears at the end of the results without changing earlier indexes.
For a non-match, the command clears the filter and appends
`Search cleared to show the new student.` to its feedback so the student is
visible and the User knows to re-read indexes. A rejected addition leaves
both the book and filter unchanged.

`EditCommand` first compares the prospective record with the selected one by
full equality. An unchanged edit returns an explicit no-change response before
any duplicate check or replacement, so the filter survives and the list is not
notified for nothing; `LogicManager` still skips saving an unchanged book.

After the identity check succeeds, `LikelyDuplicateWarning` compares the
prospective record with every existing student. Name and email comparisons
ignore case; names already carry `normalizeFieldValue`'s whitespace
normalization. Phone search and warnings share `Phone.normalizeDigits`, which
removes non-digit characters and converts Unicode decimal digits to ASCII.
Warnings require equality of the complete digit sequence; search permits
substrings. Country codes and extension digits remain significant, while the
stored number retains its formatting. Absent phones and absent emails do not
match: two records that both lack the detail share no value, so neither is
offered as evidence of a duplicate.

The commands collect the warnings before mutation and set them off from the
success message with a blank line. Matches group by field under one labeled
`WARNING: Possible duplicate student` line, each naming at most two students and
summarizing the rest as `N others`, so a shared placeholder cannot fill the
result box. The review advice appears once, after every matching field. A phone
warning omits the parenthetical value, because matched records can store the
same digits with different formatting and no one spelling would represent them
all.

`EditCommand` compares only name, phone, or email values that differ from the
selected record. It excludes the selected `Student` instance but still uses
the complete unfiltered student book, so a filtered list cannot hide a match
and unrelated tag or follow-up edits do not repeat an acknowledged warning.
Name comparisons share `StringUtil.normalizeNameForComparison` with name
searches: Latin accents and straight/curly apostrophe differences are ignored,
and a hyphen is compared as a space, so copied roster names can be found or
flagged using ordinary keyboard input even when two lists disagree on whether
a name such as `Mary-Jane` is hyphenated. The longer dashes are left alone,
because a reader can tell them from a hyphen and so chooses between them.

All of this is advisory: these comparisons never feed `isSameStudent` or the
storage duplicate check, so they cannot reject or discard a legitimate shared
detail.

`DotlessEmailDomainWarning` follows the same contract for a different doubt.
An email whose domain is a single label, such as `john@gmail`, is a valid host
name, yet on a student roster it is nearly always a forgotten `.com` or
`.nus.edu`. `Email.hasDotlessDomain` reports one, and `AddCommand` and
`EditCommand` append its warning as a second block after any duplicate block,
so the two read as separate concerns. `EditCommand` passes the selected record
as well, so an address the student already holds carries no warning and an
unrelated edit does not repeat an acknowledged one. Email validation is
untouched, so such an address is still entered, stored, loaded and compared as
it always was.

### Finding students

`FindCommandParser` uses `FlagTokenizer` and the existing `CliFlags` aliases for
name, NUS Student ID, phone, email and tag. Every search term must have a field
option. An option takes one word or one double-quoted phrase; apostrophes are
literal characters, so names such as `O'Brien` do not need quoting. Repeated
options retain all values. Bare terms receive `MESSAGE_UNQUALIFIED_TERM`,
including an extra bare word after an option: the parser catches the
tokenizer's `ValueAfterFlagsException` and answers with its own message, since
the shared one offers the front of the command, where `find` takes nothing.
Empty or whitespace-only values receive
`MESSAGE_EMPTY_KEYWORD`; unterminated double quotes report
`CommandTokenizer.MESSAGE_UNCLOSED_QUOTE`.

Search terms are normalized with `StringUtil.normalizeFieldValue`, collapsing
whitespace runs and applying Unicode NFC normalization. The parser creates a
`DetailsContainsKeywordsPredicate` with terms grouped by its `Field` enum.
Every term must match its selected field, using case-insensitive substrings:
AND applies both within and between fields. A name term is compared through
`StringUtil.normalizeNameForComparison`, the key the possible-duplicate warning
uses, so a search spelled with a hyphen or with a space reaches the same
students. Tags and the other fields compare their terms literally. Each tag term must occur within
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

A phone term made only of Unicode decimal digits, or of those digits with
grouping whitespace and phone punctuation (`+`, `-`, `(`, `)`, `.`), is also
compared with non-digit characters removed and each decimal digit mapped to its
ASCII equivalent by numeric value. So `91234567`, `9123-4567` and
`"9123 4567"` all match `+65 9123 4567` or `91234567`, and a copied grouped
number needs no reformatting; a query carrying a country code still requires
those digits in the stored phone. Normalizing both sides lets ASCII, full-width
and other Unicode decimal digits match each other, including digits written as
supplementary code points, so `"９１２３ ４５６７"` matches `91234567`. The
fallback applies only when a digit survives normalization, which keeps a
punctuation-only term from matching every phone and leaves an annotation such
as `"ext 21"` matching literally. Unicode whitespace follows the same rule as
field normalization, and the allowed-character check uses a possessive
quantifier so a long term scans without backtracking. Stored student details and
saved data do not change.

Both `FindCommand` and `ListCommand` report how many students are displayed.
`FindCommand` counts the matches through `Messages.getStudentsFoundOverview`,
which distinguishes `Found 0 students.`, `Found 1 student.` and
`Found <n> students.`; `ListCommand` counts after resetting the filter, giving
`Listed 0 students.`, `Listed 1 student.` or `Listed <n> students.`.

### Handling an unreadable data file

Startup distinguishes a missing student-book file from an unreadable one. A
missing `data/tab.json` is a first launch and receives the sample student book.
An existing file is first parsed as JSON and then every student record and
field is converted to its model type. Both stages finish before the normal UI
can accept a command.

`JsonUtil.readJsonFile` therefore reports a file as missing only once its
absence is established, using `Files.notExists`. `Files.exists` answers false
both for a file that is not there and for one whose folder cannot be opened,
and `MainApp.initModelManager` reads a missing file as a first run, so a folder
TAB merely cannot open would open the window on sample students in place of
records it was never able to read. A file whose existence cannot be determined
is read instead, and the access failure reported like any other.

The records themselves are already safe from that mistake. The baseline
`JsonStudentBookStorage` records for an unreadable folder is `Optional.empty()`,
so once access is restored `saveIfUnchanged` finds a book on disk that the
window never loaded and refuses the save. What is left without this check is a
window opened on sample data that then refuses every save as stale, telling a
User who changed nothing to close it and restart. Reporting the access failure
at startup replaces both with the reason.

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
`DataLoadingException`, and `JsonParseErrorFormatter` words the explanation it
carries. For a syntax error the formatter takes the deepest parser exception
that has a usable location, so the reported line and column point at the
offending token rather than the last value mapped successfully, and it pairs
that concise explanation with the affected line and at most one line on either
side. Source lines are capped at 160 characters, an affected long line cropped
around the reported column, which keeps minified JSON useful without filling the
dialog. Without a usable location the formatter omits the source rather than
falling back to Jackson's source-bearing message. During model conversion,
`JsonAdaptedStudent` identifies the invalid field and
`JsonSerializableStudentBook` adds the one-based record number and, where it
has one, the name. `JsonStudentBookStorage` carries that explanation through the
same `DataLoadingException` boundary.

`MainApp` records the loading error and creates an empty model only as an
internal placeholder, so shutdown stays safe, then hands the error to
`UiManager`. `UiManager` checks for it before constructing `MainWindow`, and on
finding one shows an error dialog with the data-file path, the specific reason
and recovery guidance. Closing that dialog closes TAB, because the normal window
never opened, so no command — `exit` included — can save the placeholder over
the unreadable file.

Failing closed was chosen over displaying a partial or read-only student book,
because it keeps recovery small and unambiguous: the User repairs or replaces
the file and restarts TAB. Loading the readable records separately, and
reloading without a restart, remain future features.

### Rejecting unknown keys and malformed tags

`JsonAdaptedStudent` and `JsonSerializableStudentBook` use Jackson's
`@JsonAnySetter` callbacks to reject unknown keys while the data is being read,
which keeps a hand-edited value from disappearing during a later unrelated
save. The student callback names the record and the allowed field names in its
error, and permits the removed `address` key as a compatibility exception;
the book also accepts the existing `_comment` metadata key.
`JsonStudentBookStorage` adds the one-based record number from Jackson's
mapping path, so even an unnamed record or two students sharing a name can be
located. The shared JSON mapper keeps its existing behavior for preferences,
and startup follows the existing loading-error path, leaving rejected data
intact.

`JsonTagsDeserializer` checks the array and string types before adapting tags,
so a malformed `tags` value reports the field and entry position rather than
Jackson class names. A missing or null array still means no tags, and tag text
continues through the existing model validation. Mapping errors retain their
source location and student record number, and do not rewrite the file.

### Recovering from bad preferences

`JsonUtil.readJsonFile` rejects a whole-file `null` with a
`DataLoadingException`, so both preference and student data loading use their
existing error handling instead of crashing, and invalid student data is
preserved for repair. `JsonUserPrefsStorage` likewise rejects an explicit
`null` for `guiSettings`, while a missing setting retains the `UserPrefs`
defaults. `MainApp.initPrefs` answers invalid preferences by restoring and
saving the defaults before the model is constructed; student data is stored
separately and a preference recovery does not touch it.

A recoverable preference write in `MainApp.initPrefs` or `MainApp.stop` logs
one `WARNING` naming the preferences path and the I/O reason, without a stack
trace. Startup keeps the loaded or default preferences for the session, and
shutdown explains that window preferences were not saved. A permission error
that carries no reason is described as permission denied.

### Confirming a clear

The following state/activity diagram shows the pending-clear confirmation flow:

<puml src="diagrams/ClearConfirmation.puml" alt="ClearConfirmation" />

`ClearCommand.execute` returns a prompt containing the whole-book count,
including students hidden by the current filter. It does not mutate the
model. An empty book instead returns a no-op message without a prompt.
`LogicManager.requestClearConfirmation` keeps a non-empty book's request in
`pendingClearCommand`, an `Optional<ClearCommand>` whose `Optional.empty()`
means nothing is pending, and returns before the usual storage write. While a
request is pending, the next input is read as a reply to the concise `[y/N]`
prompt. `StringUtil.stripFormatCharacters` runs first, sharing the command
parser's treatment of copied invisible marks while preserving shaping joiners.
Only surrounding Unicode whitespace is then trimmed — internal spaces and
visible characters stay, so extra words cannot become a confirmation — and
replies ignore case. Trimming scans from both ends with
`StringUtil.isWhitespace`, keeping the command parser's Unicode whitespace
rules, and takes linear time at worst, since an internal whitespace run is
never searched for a trailing match.

An empty reply, `n` or `no` drops the request without executing or saving
anything. Only `y` and `yes` exactly invoke `ClearCommand.executeConfirmed` and
save the empty book; a word merely starting with either letter, such as
`yellow` or `nope`, is invalid. Any other input raises a `ParseException` that
repeats the prompt with its feedback and keeps the request pending, and is never
executed as another command. Should the reply handler find no request at all —
the presence guard means it should not — it raises a `CommandException` asking
the User to enter `clear` rather than letting an unchecked exception escape.
The pending request is cleared before saving, so a storage error cannot leave a
stale confirmation active, and it lives only in memory, so restarting TAB cannot
confirm a previous request. The prompt and the replies both travel through the
existing result area and command field, with no mouse or dialog.
`MainWindow` supplies `Logic.isAwaitingConfirmation` to `CommandBox`, which
submits a blank Enter only during confirmation, where a blank reply cancels
the pending clear; otherwise Enter on a blank line does nothing. Blank is what
`StringUtil.isBlankInput` calls blank — nothing, whitespace, or only the
invisible characters a paste carries — so a stray space before Enter is
ignored the way an empty line is rather than answered with the `help` usage. Each `Logic` implementation must supply
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
| `*` | teaching assistant | be warned when a new student matches an existing record on name, phone or email | catch genuine duplicates even when the two names are spelled differently |
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
| teaching assistant | view a student's address | Canceled, no need for address |
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
* [UC04 - Import a roster (proposed)](#uc04-import-a-roster-proposed)
* [UC05 - Recover from an unreadable data file](#uc05-recover-from-an-unreadable-data-file)
* [UC06 - Delete a class no longer taught (proposed)](#uc06-delete-a-class-no-longer-taught-proposed)
* [UC07 - Archive last semester's students (proposed)](#uc07-archive-last-semester-s-students-proposed)
* [UC08 - Semester close-out (proposed)](#uc08-semester-close-out-proposed)
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
    * 1b2. TAB retains the submitted input for correction.
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
    * 1a2. TAB retains the submitted input for correction.
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

1.  **Operating environment**: Should work on any _mainstream OS_ with Java `25` (64-bit)
    installed, without requiring platform-specific installation or OS-dependent libraries. The
    JAR carries JavaFX `17.0.7`, whose macOS natives are x86_64, so an Apple Silicon Mac needs
    either the Azul `25.0.3` JDK+FX distribution the course installs, which supplies JavaFX
    itself, or an x64 JDK under Rosetta, as the Quick start explains. This satisfies
    `Constraint-Platform-Independent` and `Constraint-Java-Version`.
1.  **Portability**: Should be packaged as a single executable JAR file of at most 100 MB that runs
    directly without requiring an installer or system administrator privileges. This satisfies
    `Constraint-Portable`, `Constraint-Single-File`, and `Constraint-File-Size`.
1.  **Single-user operation**: Should be designed for a single user operating locally at any given time,
    without requiring or supporting concurrent multi-user write access to the data file. This
    satisfies `Constraint-Single-User`.
1.  **CLI-first accessibility**: All student management operations must be executable entirely via
    keyboard commands without requiring a mouse, trackpad, or other pointing device. This
    satisfies `Constraint-Typing-Preferred`.
1.  **Typing efficiency**: A user with above-average typing speed for regular English text should be able
    to accomplish regular student management tasks faster using CLI commands than using a traditional
    mouse-driven graphical user interface. This satisfies `Constraint-Typing-Preferred`.
1.  **Capacity**: Should comfortably store and manage up to 1,000 student records (sufficient for multiple
    semesters of active and archived tutorial and lab slots) without noticeable degradation in performance.
1.  **Responsiveness**: Should execute typical commands (such as adding, editing, deleting, tagging, listing,
    and filtering across up to 1,000 student records) and update the GUI within 100 milliseconds on
    standard modern desktop hardware.
1.  **Startup time**: Should launch and display the GUI, ready for user command input, within 2 seconds
    on standard modern desktop hardware with an existing data file containing up to 1,000 student records.
1.  **Screen resolution and scaling**: The GUI should render cleanly without visual clipping or overlapping
    components at screen resolutions of 1920×1080 and higher with display scaling at 100% and 125%, and
    remain functional and usable down to 1280×720 with display scaling up to 150%. This satisfies
    `Constraint-Screen-Resolution`.
1.  **Data locality and offline operation**: TAB must store all student data only in local files,
    must not transmit student data to any network service, and must support all student management
    operations without a network connection.

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
  (`U+200C`) and zero-width joiner (`U+200D`). It governs `CommandTokenizer`, normalized field values, and
  top-level command parsing, where `StudentBookParser` uses `StringUtil.isWhitespace`
  to separate the command word from its arguments.

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Given below are instructions to test TAB manually.

<box type="info" seamless>

**Note:** These instructions only provide a starting point for testers to work on;
testers are expected to do more *exploratory* testing.
</box>

### Launch and shutdown

1. Initial launch and where TAB keeps its files

   1. Test case: Copy the JAR into an empty folder, double-click it, add a
      recognizable student and exit. Then launch the same JAR by its absolute
      path from a terminal in a different folder.<br>
      Expected: The GUI first opens with sample records, the window size
      possibly not optimal. The folder beside the JAR then holds
      `data/tab.json`, `preferences.json` and `tab.log.0`, the student is
      present, the status bar shows the absolute path to that same file, and
      nothing is written in the terminal's own folder.

1. Reading a long data path

   1. Test case: Launch from a folder with a long absolute path and narrow the
      window until the status bar path no longer fits, then widen it.<br>
      Expected: The path is shortened in the middle, keeping `tab.json`
      visible, with the whole path on hover; the tooltip goes once it fits.

1. Launching from a read-only folder

   1. Prerequisites: Copy the JAR into a folder the account can read but not
      write, with `chmod 555 FOLDER`.
   1. Test case: Run `java -jar tab.jar` there, then
      `add Read Only Test -p 91234567`, then `exit`.<br>
      Expected: The GUI opens with sample records. The terminal warns that it
      cannot write `tab.log` and names `preferences.json`, each without a stack
      trace or `SEVERE` label. The add reports a save error and TAB stays open
      with the record absent, the failed change being rolled back.

1. Full-page help pager

   1. Test case: Enter `help`, press Space, b, j, k, g and G, then q.<br>
      Expected: Help replaces the list, sidebar, command box and result area,
      opening no window or browser, and the notice reads `Help mode: press q or
      Esc to return to commands.`. Each key moves as documented, G reaches the
      last hint with `(END)` in the footer, and paging past either end stays
      there. At minimum window size all text stays reachable without
      scrollbars.
   1. Test case: Read the first screenful, quit, and type
      `add NAME -p PHONE`.<br>
      Expected: The margin headings stand out from the body, and in both places
      `add` is bold in the accent colour, `-p` carries the option colour and
      `NAME`/`PHONE` the value colour. In help the brackets around an optional
      field and the prose lines carry no colour of their own.
   1. Test case: Type `find -n Alex` without submitting it, press F1, then q.
      Then enter `help oops`.<br>
      Expected: The unfinished command returns unselected with the caret at the
      end and no `q` inserted, so typing ` Tan` leaves `find -n Alex Tan`. The
      second quotes the offending input and opens no page.

1. Keyboard navigation through students, tags and replies

   1. Prerequisites: Add enough students with distinct tags that both the
      student list and the tag list overflow.
   1. Test case: Submit `list`, type `find -n Navigation` without submitting,
      then press Tab repeatedly and Shift+Tab back, trying Down, Page Down, End
      and Home in each list.<br>
      Expected: The cycle is command box → student list → tag list → reply →
      command box and reverses, each list taking an accent outline when
      focused. Arrows move by one and page keys by a screenful, End and Home
      reach either end, and repeating past an end stays there. The draft is
      unchanged and there is no focus trap.

1. Saving window preferences

   1. Test case: Resize and move the window, close it, and relaunch.<br>
      Expected: The size and position return when they fit the display.
   1. Test case: With TAB closed, set `windowWidth` and `windowHeight` to
      `1.0E9` and `windowCoordinates` to `{"x":99999,"y":99999}`; on a later
      run replace the file with `{"guiSettings": null}`.<br>
      Expected: The oversized bounds are corrected so the title bar, command
      box and status bar stay reachable, and the correction persists. The null
      settings open with defaults, report `GUI settings must not be null.`, and
      rewrite a valid file. Student data is untouched either way.

1. Narrow student cards

   1. Test case: Add `add Narrow Card -p "+65 9123 4567"
      -e a.very.long.address.indeed@u.nus.edu --id AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA`,
      resize to the minimum width, then widen slowly.<br>
      Expected: The ID and email shorten as needed, the phone stays readable,
      and the email stays inside the card's details area. Worth checking on
      Windows too, since font widths differ.
### Rejecting unexpected command arguments

1. Commands that take no parameters

   1. Prerequisites: Add a student, run `find -n no-such-student` to hide them,
      and keep a copy of `data/tab.json`.
   1. Test cases: `clear oops`, `list oops`, `help oops` and `exit oops`.<br>
      Expected: Each reports that it takes no arguments and quotes `oops`. The
      list stays filtered, no help opens, TAB stays open and the file matches
      the copy.
   1. Test cases: `clear ""` and `clear "unfinished`.<br>
      Expected: Each reports the unexpected input, including for an empty
      quoted argument, with nothing changed.
   1. Test case: `list` and `help` with only trailing spaces or tabs.<br>
      Expected: Both run normally, trailing whitespace alone not counting as an
      argument.
### Regional settings

1. Consistent English output

   1. Test case: Launch with
      `java -Duser.language=tr -Duser.country=TR -jar tab.jar` and type
      `add Priya -p 91234567 --follow-up`.<br>
      Expected: Both options are mauve and the command runs as usual, a locale
      that lower-cases `I` differently not disturbing the command words.
   1. Test case: Relaunch under `ar` / `EG`, then run `list`, `find -n Priya`
      and `clear`, cancelling with `n`.<br>
      Expected: Replies and summary counts use the digits 0–9 rather than the
      locale's own, and cancelling changes no data.
### Typing or mistyping a command word

1. A correctly spelled command word in capitals

   1. Test cases: `List`, `Help`, then `ADD McDonald Tan -p 91234567`, then
      `LIST oops`.<br>
      Expected: Each named command runs, no reply calling the word unknown, and
      the student is added with the capitals as typed on the card and in the
      file, the fold reaching the command word alone. The last reads `list
      takes no arguments. Unexpected input: "oops".`, naming the command rather
      than the capitals typed.

1. A command word one slip away from a real one

   1. Prerequisites: At least one student listed, and a copy of `data/tab.json`.
   1. Test cases: `delte 1`, `Delte 1`, `lsit`, `ls`, `felete 1`, `del 3` and
      `cle`.<br>
      Expected: Each quotes the word as typed and suggests the command meant —
      `delete`, `list`, `delete`, `clear` and so on. Two letters out is still
      near enough, a wrong first letter does not prevent a suggestion, and a
      word stopped partway still matches. None runs, the file matching the
      copy, and the text stays in the box with the line red so the slip can be
      corrected rather than retyped.
   1. Test case: `e`.<br>
      Expected: Both `edit` and `exit` are named, in that order every time.

1. A command word resembling nothing TAB knows

   1. Test cases: `qwerty`, `dlt 1` and `日本語`, then paste a line of at least
      80 characters with no spaces.<br>
      Expected: Each quotes the word and points to `help` with no suggestion.
      `dlt` is two letters from `edit` but opens with a letter that command
      does not, so TAB offers nothing rather than guessing. The long paste is
      quoted to its opening 60 characters followed by an ellipsis, rather than
      running off the result display.
### Follow-up status

1. Adding students with and without follow-up

   1. Prerequisites: Launch from an empty folder, then `clear` and reply `yes`.
   1. Test case: `add Alice Tan -p 91234567`, then
      `add Bob Lim -p 92345678 --follow-up`.<br>
      Expected: Neither Alice's card nor her reply shows `Needs follow-up`, and
      her saved record has no `flag` key. Bob's card and reply both show it and
      his record holds `"flag": true`.

1. Setting and clearing follow-up while editing

   1. Test case: Run `edit 1 --follow-up` twice, then `edit 1 -f`, then
      `edit 1 -p 81112222`.<br>
      Expected: Alice shows `Needs follow-up` throughout, repeating either
      spelling keeping it set, and the phone change leaves it in place.
   1. Test case: Run `edit 1 -nf`, then `edit 1 --no-follow-up`, then restart
      TAB.<br>
      Expected: The label goes with no blank row left behind, repeating either
      spelling keeps it cleared, and the state survives the restart.

1. Rejecting invalid or repeated follow-up markers

   1. Test cases: `edit 1 --follow-up -f`, then `edit 1 --follow-up true`,
      then `edit 1 --follow-up --no-follow-up`.<br>
      Expected: None changes Alice. The first reads `Options given more than
      once: --follow-up. Give each listed option only once.`; the second reads
      `--follow-up takes no value. Use the option on its own.`; the third says
      the two follow-up options cannot be used together.

1. Persisting and loading follow-up status

   1. Test case: With TAB closed, replace the file with records using
      `"flag": true`, `"flag": false`, `"flag": null` and no `flag` key, then
      relaunch.<br>
      Expected: Only the first shows `Needs follow-up`; the rest load as
      unflagged, including the legacy record with no key.
   1. Test case: With TAB closed, change one record to `"flag": 2` and
      relaunch.<br>
      Expected: The error identifies that record and its invalid `flag` value,
      the main window does not open, and the file is left unchanged.
### Adding and removing a tag

1. Preserving the student's other details and persisting the change

   1. Prerequisites: Add `add Tag Test -p 91234567 -e tag@example.com
      --id A01234567X -t T1 --follow-up` as one line, then `find -n "Tag Test"`.
   1. Test case: `tag 1 --add "Lab 3"`, then `tag 1 --remove "  LAB   3  "`.<br>
      Expected: The replies start `Added tag [Lab 3]:` and `Removed tag
      [Lab 3]:`. Only that tag comes and goes, `T1`, the contact details and
      the follow-up status staying put, the search staying active, and a
      restart keeping the result. Removal matches whatever case and spacing it
      is typed in.

1. Addressing a student by stable ID

   1. Prerequisites: Add `add ID Tag Test -p 92345678 --id E7654321 -t T1` and
      `add Numeric Tag Test -p 93456789 --id 1234`, then run
      `find -n NoMatchingStudent` so both are hidden.
   1. Test case: `tag e7654321 --add="Lab 3"`, then `tag -i 1234 -a T2`, then
      `tag 1 --id E7654321 -a T2`.<br>
      Expected: The first two reach the right student although neither is
      displayed, preserving the search. A bare `tag 1234 -a T2` stays an index
      request. The third asks for one target and changes nothing.

1. Reporting duplicate and absent tags

   1. Prerequisites: Return to Tag Test with `find -n "Tag Test"`.
   1. Test case: `tag 1 -a T1`, then `tag 1 -r "Lab 3"` once it is gone.<br>
      Expected: `This student already has tag [T1]. No changes made.` and
      `This student does not have tag [Lab 3]. No changes made.`, with nothing
      written either time.
   1. Test case: `tag 1 -a "Needs follow-up"`, then `tag 1 -r "Needs follow-up"`
      on this flagged student.<br>
      Expected: Adding is refused, naming `--follow-up` as the way to mark the
      student. Removing adds a second line, `This student is flagged for
      follow-up, which is not a tag. To clear the flag, use edit 1
      --no-follow-up.`, the label staying until that `edit` runs.

1. Rejecting invalid input without mutation

   1. Test cases: `tag 0 -a T2`, `tag 1 -a ""`, `tag 1 -a`,
      `tag 1 -a T2 -r T1`, `tag 1 -t T2` and `tag 1 lab4`.<br>
      Expected: Each gives the matching index, invalid-tag, missing-value or
      conflicting-action error, leaving the student, the list and the file as
      they were. Wherever the action is left unclear the reply also carries the
      `-a`/`--add` and `-r`/`--remove` guidance, naming the unknown `-t` option
      and the unexpected value `lab4`.

1. Following the reply to a two-word tag left unquoted

   1. Test cases: `tag 1 -a Lab 3`, then the quoted form the reply names,
      `tag 1 -a "Lab 3"`.<br>
      Expected: The first names `"3"` as belonging to no option and offers only
      the quotes, since the index already stands before the options. The second
      adds `Lab 3`.
   1. Test cases: `tag -a Lab 1`, then `tag 1 -a Lab`.<br>
      Expected: The first reply also offers the front of the command, which no
      target occupies there, and the second adds `Lab` to the first student.

1. Removing a tag that supplies the search match

   1. Test case: `tag 1 -a "Lab 3"`, `find -t "Lab 3"`, then `tag 1 -r "Lab 3"`.<br>
      Expected: The student leaves the filtered list, and `list` shows them
      again without that tag.

1. Reading repeated spellings from a hand-edited file

   1. Test case: With TAB closed set Tag Test's `tags` to
      `["Lab 3", "  lab   3  ", "T1"]`, keep `"flag": true`, restart, and run
      `find -n "Tag Test"` then `tag 1 -r "LAB 3"`.<br>
      Expected: The variants load as the single tag `Lab 3`, which is removed,
      leaving `T1`, the flag and every other field after a restart.
### Adding a student

1. Adding a student with the details that are known

   1. Test case: `add John Doe -p 98765432`, then
      `add Email Only --email=e1234567@u.nus.edu --id=E1234567`, then
      `add Name Only`, then `add Jane Doe -p 91234567 -t urgent -t lab1
      --follow-up`.<br>
      Expected: All four are added, the name being the one required detail. The
      replies carry no placeholder labels for absent fields, the phone-less
      card leaves no gap where a number would be, and Jane's tags are listed
      `[lab1][urgent]`, sorted alphabetically ignoring case. Only a malformed
      value is refused, as `-p 12` is.

1. Revealing a newly saved student in an overflowing list

   1. Prerequisites: A book of at least 30 students, scrolled to the top.
   1. Test case: `add Newly Added Student -p 91234567`, then return to the top
      and try `add -p 91234567`.<br>
      Expected: The new card is visible without scrolling by hand and the next
      command can be typed straight away. The refused add leaves the list where
      it is rather than jumping to the end.

1. Recovering from a name option or a missing name

   1. Test cases: `add --name "Betsy Crowe" --phone 1234567`, then
      `add -p 1234567`, then `add Betsy Crowe --phone 1234567`.<br>
      Expected: The first names the unsupported option, followed by `In the add
      command, the name comes first, without an option: add NAME -p PHONE ...`.
      The second reads `Missing required field(s): NAME` with the same hint.
      The third adds her, and she survives a restart.

1. Adding a duplicate NUS Student ID

   1. Test case: `add John Doe -p 91234567 -i A05678901K`, then
      `add Jane Doe -p 98765432 -i a05678901k`.<br>
      Expected: The second is refused, naming the ID and `John Doe`, and no
      record changes.

1. Adding during a search

   1. Prerequisites: From an empty book add `add Alice Tan -p 91110001 -t T1`,
      `add Bob Lim -p 91110002 -t T2` and `add Carl Ong -p 91110003 -t T1`.
   1. Test case: `find -t T1`, then `add Eve Ng -p 91110005 -t T1`, then
      `delete 2`.<br>
      Expected: Eve joins the filtered list with no search-cleared notice, so
      index 2 is still Carl and deleting it leaves Bob untouched.
   1. Test case: `add Dan Koh -p 91110004 -t T2`.<br>
      Expected: Dan does not match, so the reply ends `Search cleared to show
      the new student.` The list now holds everyone, so indexes must be read
      again before the next index command.
### Long contact details on a card

1. Retaining long phone text

   1. Test case: Run `add Long Phone -p VALUE`, where `VALUE` is 400 decimal
      digits, then exit and reopen TAB.<br>
      Expected: The student is added, hovering over the shortened phone shows
      the complete value, and that value survives the restart untruncated.

1. Reading an annotated phone and an email

   1. Test case: `add Priya Nair -p "1234 5678 (HP) 1111-3333 (Office)"
      -e e0923841@u.nus.edu -t T1 --follow-up`, then narrow the window to its
      490 px minimum and widen it again.<br>
      Expected: Phone and email stay visible, wrapping as needed, and a value
      too wide for its line ends with an ellipsis whose complete text is on
      hover. Widening returns them to one line without ellipses or tooltips,
      and the stored values are unaffected by how much was visible.
   1. Test case: `add Mei Lin -p 91234567`, then
      `add Mei Lin Without A Number -e e1234567@u.nus.edu`.<br>
      Expected: Both cards keep the same height. An absent email, ID or phone
      leaves no blank line, the email sitting where the number would start.

1. Checking height around wrap thresholds

   1. Prerequisites: In a disposable book add `add Siti Aminah -p 98765432
      --id A0234567X -e e0923841@u.nus.edu -t T01 -t "Lab 3" --follow-up` and
      `add Priya Nair -p 91234567
      -e aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@u.nus.edu`.
   1. Test case: Resize slowly from the default size through 760 px and 640 px
      to the minimum width and back.<br>
      Expected: Every name stays above the contact rows without clipping or
      overlap, and a card grows whenever a contact wraps, including Priya's
      without tags or a flag. Clipped values stay readable on hover, and
      toggling follow-up never changes the contact layout.
### Long names on a card

1. Telling apart two names that share a start

   1. Test case: `add Mohamed Abdul Rahman bin Ismail -p 91234567`, then
      `add Mohamed Abdul Rahman bin Ahmad -p 92345678`. Widen the window, then
      narrow it to its smallest width.<br>
      Expected: Both appear in full when there is room, with no tooltip. As the
      cards shrink each keeps its start and its end around the ellipsis, so the
      two stay distinguishable rather than collapsing to the same text, and
      hovering shows the whole name.

1. Sharing a row between a name, tags and a follow-up marker

   1. Test case: Add `add Bernice Xu -p 81234567 -t attendance-low -t consult
      -t follow-up -t lab -t midterm -t tutorial -f` and
      `add Tan Ah Kow -p 94445555 -t attendance-low -t consult
      -t "Project A/B" --follow-up`. Narrow to the minimum, widen slowly, then
      clear one marker with `edit INDEX -nf`.<br>
      Expected: Each name keeps its opening letters. At the minimum width the
      count-only chip reads `6 tags`; once chips appear the count reads
      `N more…` for the hidden ones, listed on hover. Where the tags collapse
      and the short name plus count fit, `Tan Ah Kow` shows in full rather than
      being shortened beside a blank gap, and the name takes the room the
      cleared marker releases.

1. Reading every tag that fits

   1. Test case: Add `add Alex Yeoh -p 87438807 -t T01 -t "Lab 3" -t W5
      -t consult`, widen to about 1400 px, then add
      `add Irfan Ibrahim -p 92492021 -t "CS2103T Tutorial W09"
      -t "CS2103T Tutorial W10"` and narrow and widen repeatedly.<br>
      Expected: All four chips appear alphabetically with no count when there
      is room. Chips that fit appear in full, hidden ones are counted and
      listed on hover, and a lone chip too wide for its allocation is shortened
      with its complete tag on hover. It never shows only one or two letters of
      a tag, and the follow-up marker stays visible.
### Long value-option names

1. Adding and editing with long aliases

   1. Prerequisites: Launch, run `clear`, and reply `yes`.
   1. Test case: `add Alice Tan --phone 91234567 --email alice@example.com
      --tag T1 -t "Lab 3"` as one line, then `edit 1 --name "Alice Lim"
      -p 81112222 --tag T2`, then `edit 1 --tag ""`.<br>
      Expected: Alice is added with both tags, the quoted one keeping its
      space, so short and long spellings work together. The edit changes each
      named field and drops the earlier tags, an edit replacing the tag set.
      The empty value then removes every tag.

1. Treating aliases as one logical field

   1. Test case: `edit 1 -p 81112222 --phone 82223333`, then
      `edit 1 -t T1 --tag "Lab 3"`.<br>
      Expected: The first changes nothing and reports the phone as given more
      than once although the aliases differ. The second succeeds with both
      tags, a tag option being allowed to repeat.

1. Rejecting malformed long options without changing data

   1. Test cases: `add Bob Lim --phone`, then `add Bob Lim --unknown value`,
      then `edit 1 --phon=1`.<br>
      Expected: Nothing is saved. A missing value names `--phone PHONE`, an
      unknown option names `--unknown`, and the mistyped marker is named
      without its value.

1. Joining option values with an equals sign

   1. Test case: `edit 1 --name="Alice Tan" --id=a01234567x --tag="Lab 3"` as
      one line, then `tag 1 --add=needs=review`.<br>
      Expected: Each field changes, the name and tag keeping their spaces and
      the ID stored as `A01234567X`. The tag `needs=review` is added, only the
      first equals sign separating option from value.
   1. Test case: `edit 1 --email=`, then `edit 1 --email="" --id=""`, then
      `edit 1 --follow-up=yes`.<br>
      Expected: The bare form reports that the option needs a value, an
      unquoted empty value possibly being an accidental omission. The quoted
      form clears both fields, quoting making the omission deliberate. The last
      reports that the option takes no value.
### NUS Student IDs

1. Adding and clearing an optional NUS Student ID

   1. Prerequisites: Launch from an empty folder, then `clear` and reply `yes`.
   1. Test case: `add Alice Tan -p 91234567 --id a01234567x`, then
      `add Bob Lim -p 92345678 -i A12345678Y`, then `add Cara Ng -p 93456789`.<br>
      Expected: All three are added. Alice's card shows `A01234567X`, the
      lowercase input normalized, and `-i` behaves as `--id`. Cara's card shows
      no placeholder and her record has no `nusStudentId` key.
   1. Test case: `edit 1 -i A76543210Y`, then `edit 1 --id ""`, then
      `edit 1 -i A01234567X` and `edit 1 --id "   "`.<br>
      Expected: The ID changes, then clears from card, feedback and record with
      no other detail touched, and a whitespace-only value clears it the same
      way.

1. Rejecting invalid, missing and repeated values

   1. Test cases: adding with `--id ""`, `--id A0123-456X` and
      `--id ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567`.<br>
      Expected: Nobody is added. Each message names the NUS Student ID and says
      whether it is empty, too long, or holds a character other than an ASCII
      letter or digit.
   1. Test cases: `edit 1 --id` and `edit 1 --id A11111111A -i A22222222B`.<br>
      Expected: Alice is unchanged. The first says the option needs a value,
      the second reports the field as repeated, the two spellings being
      aliases.

1. Finding and persisting NUS Student IDs

   1. Test case: Restore the ID, restart TAB, then `find --id a0123`.<br>
      Expected: Alice is found, matching being partial and ignoring case, and
      the ID survived the restart.
   1. Test case: With TAB closed, change one `nusStudentId` to `A0123-456X`
      and relaunch.<br>
      Expected: A startup error names the file, the record, the rejected ID
      and the one rule it breaks, as an `add --id A0123-456X` reply does, and
      the main window does not open.

1. Reporting unchanged edits

   1. Test case: From an empty book `add Amy Bee -p 91234567`, run
      `find -n Amy`, then `edit 1 -p 91234567`, then
      `edit 1 -n "Amy Bee" -p 82223333`.<br>
      Expected: The first edit replies `Student already has the requested
      details. No changes made.`, leaving the search and file as they were. The
      second reports the new phone, Amy staying visible.

1. Checking duplicate identity behavior

   1. Test case: From an empty book, `add Alice Tan -p 91234567 -i A01234567X`,
      `add Alice Tan -p 92345678 -i A76543210Y`, then `add Alice Tan -p 93456789`.<br>
      Expected: All three are added, a shared name not being an identity key
      while the IDs differ or are absent, and the later additions warn that
      others share the name.
   1. Test case: `add Bob Lim -p 94567891 -i a01234567x`, then `list`, note the
      third Alice's index and run `edit INDEX -i A01234567X`.<br>
      Expected: Both are rejected, each naming the ID and `Alice Tan`, and no
      record changes.

1. Warning about likely duplicate students

   1. Test case: From an empty book add
      `add Alex Yeoh -p "+65 9123 4567" -e Alex@Example.com -i A01234567X`,
      then `add "  aLeX   yEoH  " -p 81112222 -e other@example.com`, then
      `add Bernice Yu -p "+65 (9123)-4567" -e bernice@example.com`.<br>
      Expected: The first warns about nothing. The second is saved and names
      the matching `name`, case and repeated spaces not preventing the match.
      The third names the matching `phone`, the digit sequences matching
      despite punctuation, and reads `Alex Yeoh has the same phone.` without
      quoting either number. Restarting keeps both numbers as entered.
   1. Test case: Clear the book, add four students named `Student One` through
      `Student Four` each with phone `000`, then `add Student Five -p 000`.<br>
      Expected: Student Five is saved, one warning naming two of them and
      saying two others match, with the review advice appearing once.

1. Warning about an email whose domain has no dot

   1. Test case: From an empty book, `add Tan -e john@gmail`, then
      `add Ng -e john@gmail.com`, then `add Lee -e john@gmail.c`.<br>
      Expected: Tan is saved and warned about, the warning naming the address
      and suggesting a missing part such as `.com`. Ng is saved without a
      warning. Lee is refused, the two-character rule on the final label being
      validation rather than a warning.
   1. Test case: Continuing above, `list`, note Tan's index, then
      `edit INDEX -t T1`, then `edit INDEX -e tan@nus`.<br>
      Expected: The tag edit shows no warning, the address being one the
      student already holds. The address edit warns again, naming `tan@nus`.
      Restarting TAB keeps the address as entered.

### Editing a student

1. Editing a name that AB3's prefixes made impossible to type

   1. Prerequisites: `add Ravi Kumaran -p 91234567`, then `list` and note the
      index.
   1. Test case: `edit INDEX -n "Ravi s/o Kumaran"`, then the same without the
      quotation marks.<br>
      Expected: The quoted form stores the name in full, the slash being an
      ordinary character where `n/John p/ Smith` once stored `Smith` as the
      phone. The unquoted form changes nothing and reports that `s/o` belongs
      to no option.

1. Editing by NUS Student ID

   1. Prerequisites: Note a student's ID, such as Charlotte Oliveiro's
      `A02345678G` in the sample data.
   1. Test case: `edit A02345678G -p 81112222`, then
      `edit a02345678g -p 81112333`, then `find -n Alex` followed by
      `edit A02345678G -p 81112444` while the search hides her.<br>
      Expected: Each reports `Edited student: Charlotte Oliveiro` with the new
      number, so an ID matches whatever its case and reaches her even when she
      is not displayed. Each command needs its own value: repeating one already
      stored replies `Student already has the requested details. No changes
      made.` An `edit 1` in that state would edit Alex instead, index 1 being
      the first student shown.
   1. Test case: `edit A01234567X --id A03456789H`, where another student holds
      the second ID, then `edit A99999999Z -p 81112222` and
      `edit A0123-456 -p 81112222`.<br>
      Expected: None changes anything: the first refuses an ID another student
      holds, the second reads `No student has NUS Student ID A99999999Z.`, and
      the third names the hyphen.

1. Clearing the tags

   1. Test case: Give the student two tags, then `edit INDEX -t ""`, then
      `edit INDEX -t` and `edit INDEX -t T1 -t ""`.<br>
      Expected: The first clears both tags and touches nothing else; the second
      names `-t TAG` as needing a value; the third is refused, an empty value
      clearing the tags only on its own.

1. Repeating an edit after a search

   1. Prerequisites: Add `add Alex Yeoh -p 87438807`, `add John Doe -p 98765432`
      and `add John Smith -p 91112222`, then run `find -n john`.
   1. Test case: Run `edit 1 --follow-up` twice, then `edit 1 -n Jack` followed
      by `edit 1 -p 91112233`.<br>
      Expected: Both flags go to John Doe, the search staying active so index 1
      keeps pointing at him. Renaming him to Jack then takes him off the
      filtered list and John Smith shifts up, so the last command changes John
      Smith's phone rather than Jack's — the shift the User Guide warns about.

1. Reporting several mistakes at once

   1. Test case: `edit 0 -p 12`, then `edit` and `edit INDEX`.<br>
      Expected: Nothing changes. The first opens `Index must be a positive
      integer.` and also reports the invalid phone, without the usage block.
      The second names the missing index and says a field is needed; the third
      asks only for a field.
### Clearing a student's optional details

1. Removing an optional detail without removing the record

   1. Prerequisites: Add `add Alice Tan -p 91234567 -e alice@example.com
      --id A1234567X -t T1 -t "Lab 1" --follow-up` as one line, and note her
      displayed `INDEX`.
   1. Test case: `edit INDEX -e ""`, then `edit INDEX -p ""`, then
      `edit INDEX -t "   "`.<br>
      Expected: Each detail disappears from the card and the feedback, the
      remaining ones closing up with no blank line, and every other field and
      the follow-up status stay as they were. The saved record drops its
      `phone` and `email` keys, while cleared tags are written as an empty
      `"tags": []` rather than removed. The details stay absent after a
      restart, and a whitespace-only value is trimmed and clears the field the
      same way.
   1. Test case: Repeat any of those commands once the field is already
      absent.<br>
      Expected: Each succeeds with nothing else changed.

1. Supplying a detail later

   1. Test case: `find -p 9123`, then `list`, note Alice's `INDEX` again, and
      run `edit INDEX -p "+65 9123 4567"` followed by `find -p 9123`.<br>
      Expected: The first search does not list her, holding no number, so
      `list` is needed before the edit can name her by position. Her card then
      shows the number as entered and the second search lists her.

1. Refusing a clear that is not meant as one

   1. Test case: `list`, note Alice's `INDEX` once more, then `edit INDEX -p`
      and `edit INDEX -p 12`.<br>
      Expected: The first names `-p PHONE` as needing a value; the second
      reports the malformed number. Neither changes the student or the file.
   1. Test case: `edit INDEX -t " " -t T2`, then
      `edit INDEX -e "" -e alice@example.com`.<br>
      Expected: The first reads `A blank tag clears all tags only when it is
      given on its own.`; the second reports the repeated option. Nothing
      changes either way.
### Email addresses

1. Recording addresses with symbols

   1. Test case: `add Mei Tan -p 91234567 -e mei__tan@hotmail.com`, then
      `add Sean O'Brien -p 82345678 -e sean.o'brien@example.com`.<br>
      Expected: Both are added with the underscores and the apostrophe intact,
      and both survive a restart exactly as recorded.
   1. Test case: Using Mei's index, `edit INDEX -e mei..tan@example.com`.<br>
      Expected: An Email error explains that dots must separate non-empty
      groups, leaving the record and file unchanged. A leading or trailing dot,
      a space inside the address, or an underscore in the domain behave the
      same way.
   1. Test case: With TAB closed, set Mei's saved email to
      `mei..tan@example.com` by hand and restart.<br>
      Expected: A data-file error identifies the Email field rather than the
      value being dropped.

1. Refusing an address longer than the limit

   1. Test case: `edit 1 -e` followed by an address of more than 254
      characters, then the same command with a phone of `12` as well.<br>
      Expected: Neither changes anything. The message names the 254-character
      limit and the length given, quoting only the opening characters rather
      than filling the result box. With the bad phone too, both problems are
      reported, so the length check joins the other field checks rather than
      stopping them.
   1. Test case: An address of exactly 254 characters.<br>
      Expected: The edit succeeds, the limit being the longest an address may
      be, so no real address is refused.

1. Reading a clipped address

   1. Test case: Add `add Siti Nur-Aisyah -p "6516-2727 ext 21"
      -e siti.nuraisyah.binte.ahmad@gmail.com -t T1 -f`, narrow the window
      until the email is cut, hover over it, then widen it again.<br>
      Expected: The tooltip shows the whole address while it is cut, and the
      tooltip goes once it fits. Clearing it with `edit INDEX -e ""` removes
      the label with no placeholder left behind.
### Deleting a student

1. Deleting a student while all students are being shown

   1. Prerequisites: Run `list` with several students present, and note one
      student's NUS Student ID, such as Charlotte Oliveiro's `A02345678G` in
      the sample data.
   1. Test case: `delete 1`<br>
      Expected: The first record goes and the reply identifies the removed
      student with their details.
   1. Test cases, each a separate run from the same starting data:
      `delete A02345678G`; `delete a02345678g`; and `find -n Alex` followed by
      `delete A02345678G` while the search hides her.<br>
      Expected: Each run deletes Charlotte and the reply identifies her, so an
      ID matches whatever its case and reaches her even when she is not
      displayed. They are separate runs because the first deletion leaves no
      student for the next to find.
   1. Test case: `add "Digit Ida" -p 91234567 --id 1ABC`, then `delete 1ABC`,
      then `delete --id 1ABC`.<br>
      Expected: The bare form is refused as an unusable index, because a target
      starting with a digit reads as one, and the reply names
      `delete --id 1ABC`. The explicit form deletes her.

1. Refusing a target it cannot use

   1. Test case: `delete 0`, then `delete -1` and `delete 1.5`.<br>
      Expected: Nothing is deleted. The first reads `Index must be a positive
      integer.` followed by the `delete --id 0` recovery rather than the usage
      block; the other two give that reason on its own.
   1. Test case: `delete A99999999Z`, then `delete`, `delete 999` and
      `delete 1 2`.<br>
      Expected: `No student has NUS Student ID A99999999Z.`; a bare `delete`
      shows the command's format; `delete 999` is refused as past the end of
      the displayed list; and `delete 1 2` reads `Unexpected value after INDEX:
      "2". delete accepts one index or NUS Student ID and nothing else.` No
      student is deleted.
   1. Test case: `delete Alex Yeoh`, then `edit Alex Yeoh -p 91234567` and
      `tag Alex Yeoh -a T1`.<br>
      Expected: Nothing is changed. Each reply names the word to remove after
      `NUS_STUDENT_ID`, the form a target that is not a number is read as:
      `Unexpected value after NUS_STUDENT_ID: "Yeoh". delete accepts one index
      or NUS Student ID and nothing else.` for `delete`, and the same opening
      followed by `Put field values after an option.` for the other two. No
      reply calls `Alex` an index.
   1. Test case: `delete 1` followed by a non-breaking space (`U+00A0`) pasted
      from a web page.<br>
      Expected: The first record is deleted exactly as with an ordinary space.
      TAB does not call the command malformed for a character the user cannot
      see.

1. More test cases to be added.
### The command line

1. Recalling commands and restoring a draft

   1. Test case: Submit `list`, then `find -n "Siti Nur-Aisyah"`, type
      `add draft` without submitting it, press Up three times, then Down
      twice.<br>
      Expected: Up recalls the find command, then `list`, then stays at `list`,
      each recalled line keeping its colours with the caret at the end. Down
      walks back and restores the unsent draft. In a fresh session, before
      anything is submitted, Up and Down leave the text alone.
   1. Test case: Submit `addd`, run `clear` and press Enter on the empty reply,
      then press Up.<br>
      Expected: The rejected `addd` is recalled like any other line and
      recalling it does not run it. Clear is cancelled without changing any
      students and Up recalls `clear` with no blank entry between. Restarting
      TAB clears the history.

1. Pasting multi-line text

   1. Test case: Copy two adjacent spreadsheet cells holding `Wei Ming` and
      `Tan`; type `add `, paste, add ` -p 91234567` and press Enter. Repeat
      with a soft line break rather than a newline, as
      `printf 'Siew Ling\vGoh' | xclip -selection clipboard` produces.<br>
      Expected: Each pasted break reads as a space, so the words stay apart
      with matching colours and one student is added under the joined name.
      Pasting into a selection replaces it and leaves the caret after the
      inserted text.

1. Checking what the colours say

   1. Test case: Type `add "Siti Nur-Aisyah" -p 98765432 -t T1` without
      pressing Enter, then submit `addd`, then type another character.<br>
      Expected: `add` is bold in the accent colour, the quoted name green, the
      markers mauve and the values ordinary text. The rejected word turns the
      whole line red, the reply quoting `addd` and suggesting `add`, and the
      next character takes the red off.
   1. Test case: Type `add"Ravi s/o Kumaran" -p 91234567` with no space after
      `add`, then press Enter.<br>
      Expected: The bold command word ends at `add"Ravi`, with `s/o` an
      ordinary word after it, and the reply reports that unknown command,
      splitting the line where the colours already did.

1. Checking a long or bidirectional command

   1. Test case: Type a `find` command long enough to run past the right-hand
      edge, press Home then End, then Ctrl+A, then click in the middle.<br>
      Expected: The line scrolls so the caret stays in view at both ends rather
      than being cut off, a band is drawn behind the whole command with every
      part readable on top of it, and the caret lands on the character clicked.
      Narrowing the window and clicking again behaves the same.
   1. Test case: Type `add Ali محمد דוד Tan -p 91234567`, click between letters
      of an RTL word, type a character, then select across the Latin and RTL
      words.<br>
      Expected: The command stays on one readable line, the caret marks the
      insertion point after a click, and only the selected characters are
      highlighted even where the selection falls into separate visual segments.
### The summary beside the list

1. Checking that the summary follows the list

   1. Prerequisites: Several students, at least one flagged with `edit 1 -f`,
      and several sharing a tag. The sample data ships none flagged.
   1. Test case: `list`, then `find -n Alex`.<br>
      Expected: The total matches the number of cards, each tag in use is
      listed with its count, most used first and equal counts alphabetical
      whatever case they were typed in. The search drops the counts to cover
      only the students now listed.
   1. Test case: `clear`, then `yes`.<br>
      Expected: The flagged count and the total read zero and the tag list is
      empty, rather than the panel disappearing.

1. Counting case variants of one tag

   1. Test case: From an empty book add three students, then `tag 1 -a T1`,
      `tag 2 -a t1` and `tag 3 -a T1`.<br>
      Expected: The TAGS section holds one row, `T1` with the count `3`, rather
      than a row per spelling.

1. Distinguishing long tags

   1. Test case: `add Eve Koh -p 81112222 -t consultation-week-3`, then
      `add Fay Lo -p 81112223 -t consultation-week-3 -t consultation-week-4`.
      Then add enough distinct long tags to fill the sidebar.<br>
      Expected: Both complete tag names show on wrapped lines with counts `2`
      and `1` beside the right names, neither row overlapping the next and no
      widening needed to read the endings. Once full, the tags scroll
      vertically and the student total stays visible.
### Student counts in list and find feedback

1. Accurate student counts in command feedback

   1. Prerequisites: An empty student book, reached with `clear` and `yes`.
   1. Test case: `list`, then `find -n Alex`.<br>
      Expected: `Listed 0 students.` and `Found 0 students.`, with nothing
      displayed.
   1. Test case: `add "Amy Bee" -p 85355255 -e amy@example.com`, then `list`
      and `find -n Amy`.<br>
      Expected: `Listed 1 student.` and `Found 1 student.`, in the singular.
   1. Test case: `add "Bob Choo" -p 88888888 -e bob@example.com`, then `list`,
      then `delete 1` and `list` again.<br>
      Expected: `Listed 2 students.` in the plural, then `Listed 1 student.`
      once one is deleted.
### Locating equivalent name spellings and Unicode case variants

1. Searching copied roster names without changing their stored spelling

   1. Prerequisites: In a disposable folder, add `add Sean O’Brien -p 87654321`,
      `add Nguyễn Văn An -p 81234567`, `add José Rivera -p 81234568` and
      `add ΝΙΚΟΣ -p 91234567 -t ΟΣ`.
   1. Test cases: `find -n O'Brien`, `find -n nguyen` and `find -n jose`.<br>
      Expected: Each finds its student although the term is typed with a
      straight apostrophe or without accents, and the cards keep the curly
      apostrophe and the accents.
   1. Test cases: `find -n Σ`, `find -n ς` and `find -t οσ`.<br>
      Expected: Each finds ΝΙΚΟΣ, so the two lower-case forms of a Greek sigma
      match the capital. `tag 1 -a οσ` reports `This student already has tag
      [ΟΣ]. No changes made.`
   1. Test case: `add Sean O'Brien -p 83334444`, then restart TAB and repeat
      the searches.<br>
      Expected: The add succeeds with a possible-duplicate name warning rather
      than a refusal, and the original spellings survive saving and reloading
      while the normalized comparisons still find them.
   1. Test case: `add "Mary Jane" -p 84445555`, then `add Mary-Jane -p
      85556666`, then `find -n "mary jane"`.<br>
      Expected: The second add warns that Mary Jane has the same name, the two
      cards keep the space and the hyphen they were given, and the search
      lists both students.
### Normalizing hidden control characters in saved names and tags

1. Test case: With TAB closed, set one student's saved name to `"Am\u0001y"`
   and tags to `["La\u007fb"]`, then restart.<br>
   Expected: The card shows `Amy` and `Lab`, and `find -n Amy` and
   `find -t Lab` both find that student. Loading alone leaves the file as it
   is; the next save writes the names without the hidden characters.
### Keeping one spelling of repeated command tags

1. Test case: From an empty book, run
   `add Casey Low -p 91234567 -t Lab -t lab -t LAB -t Σ -t ς`, then
   `edit 1 -t lab -t LAB -t ς -t Σ`.<br>
   Expected: Casey holds only `Lab` and `Σ`, surviving a restart. The edit
   replaces them with `lab` and `ς`, the first spelling of each
   case-insensitive group being the one saved.
### Recovering from save-path collisions

1. Test case: Put an unrelated regular file named `data` beside the JAR, launch
   TAB, and edit a student's phone.<br>
   Expected: TAB names the occupied data-folder path and asks you to close TAB
   and move or rename the file. The student, the active search and the
   unrelated file are unchanged.
1. Test case: With TAB closed, replace the empty `data/.tab.json.lock` file
   with a folder, relaunch and edit a student.<br>
   Expected: TAB names the lock path, explains that it is a folder, and asks
   you to close every TAB window before moving or renaming it. Nothing is
   changed.
### Diagnosing student-target mistakes

1. Prerequisites: From an empty book, add `add Amy Bee -p 91234567 --id 001234`
   and `add Other Student -p 82223333`, then run `find -n Amy`.
   1. Test cases: `delete 3`, `edit 3 -p 81112222` and `tag 3 -a T1`.<br>
      Expected: Each quotes index `3`, reports one displayed student and
      suggests `list`. Nothing changes. After `find -n Missing` the same
      commands report zero displayed students.
   1. Test case: `edit 6591234567 -p 81112222`, then `delete 6591234567`.<br>
      Expected: Each explains the upper index bound rather than calling a
      positive integer non-positive, and delete also shows the explicit ID
      form.
   1. Test case: With Amy hidden, `delete 001234`.<br>
      Expected: The error keeps the leading zeroes and suggests
      `delete --id 001234`, which deletes her even while the filter hides her.
### Locating students by digits in formatted phone numbers

1. Full numbers, partial numbers, and unchanged saved formatting

   1. Prerequisites: In a disposable copy, `clear` and reply `yes`, then
      `add Alex Yeoh -p "+65 9123 4567"` and `add Bernice Yu -p 9876-5432`.
      Restart TAB and keep a copy of `data/tab.json`.
   1. Test cases: `find -p 91234567`, `find -p 91234`, `find -p 9123-4567` and
      `find -p +6591234567`.<br>
      Expected: Each shows only Alex Yeoh, `Found 1 student.`, his phone still
      written `+65 9123 4567`. A term of digits and phone punctuation compares
      by the digits alone.
   1. Test cases: `find -p "ext 21"`, `find -p " + ( ) - . "` and
      `find -p 91234567 -n Bernice`.<br>
      Expected: `Found 0 students.` each time: annotations and
      punctuation-only terms do not become digit searches, and both terms of
      the last one are required. The saved file matches the copy throughout.

1. Long nonmatching phone terms

   1. Test case: Paste `find -p ` followed by 50,000 digits and a final `!`.<br>
      Expected: TAB promptly reports `Found 0 students.` and stays responsive.

1. Equivalent Unicode digits

   1. Prerequisites: From an empty book, `add Wang Fang -p ９１２３４５６７`
      and `add Lim Jia Hui -p 87654321`.
   1. Test cases: `find -p 91234567`, then `find -p ٩١٢٣`.<br>
      Expected: Each shows only Wang Fang, her phone still stored in full-width
      digits, so digits of any script compare alike. `find -p ８７６５` finds
      only Lim Jia Hui, whose number is stored in ASCII.
### Quoting values with input-method punctuation

1. Curly and full-width double quotes

   1. Prerequisites: From an empty book.
   1. Test case: `add “Betsy Crowe” -p 1234567`, then `tag 1 -a “组 1”` and
      `tag 1 -a ＂Lab 3＂`.<br>
      Expected: Each succeeds, the name and tags stored without the surrounding
      quotes, and quoted values appear green while typing.
   1. Test case: `find -t ＂Lab 3＂`, then `edit 1 -n “Unclosed`, then restart
      TAB.<br>
      Expected: The search finds the student; the unclosed quote is reported
      and the name is unchanged; the stored values survive the restart without
      quote marks.
### Locating students with quoted phrases and error handling

1. Quoted phrases, mixed terms, and unclosed quotes

   1. Prerequisites: From an empty book, add `add "Alice Tan" -p 91111111
      -t "Lab 3"` and `add "Bob Lee" -p 93333333 -t Lab`.
   1. Test case: `find -t "Lab 3"`, then `find -n Alice -t "Lab 3"`.<br>
      Expected: Each shows only Alice Tan, `Found 1 student.`. Bob Lee is
      excluded because the phrase does not match separate `Lab` and `3`
      fragments, and the second shows the two terms may sit in different
      fields.
   1. Test case: `find -t "Lab  3"` with two spaces inside the quotes.<br>
      Expected: Alice Tan still matches, search terms being normalized like
      stored values, which collapses interior whitespace.
   1. Test cases: `find -t "Lab 3`, then `find -t ""`, then a bare `find`.<br>
      Expected: The first reads `A quoted value is never closed. Add the
      closing " or remove the opening one.`; the second `A search term is
      empty. Remove the empty quotes or put a value between them.`; the third
      the find usage. The displayed list is unchanged after each.
### Finding students within selected fields

1. Field isolation and required combinations

   1. Prerequisites: In a disposable copy, `clear` and reply `yes`, then add
      `add Jason Lim -p "+65 9123 4567" --id A01234567X -e jason@nus.edu
      -t "lab tutorial" -t friend`, `add Jason Zhou -p 93333333 -t "lab 5"
      -t "tutorial 3"`, `add Jason Tan -p 94444444 -t lab` and
      `add Friend Lee -p 95555555 -e friend@example.com`.
   1. Test case: `find -t friend`, then `find -n Jason -t lab -t tutorial`.<br>
      Expected: The first shows only Jason Lim, excluding Friend Lee's name and
      email matches. The second shows Jason Lim and Jason Zhou, Jason Tan
      lacking the required tutorial term, so repeating a selector requires all
      terms without requiring different tags.
   1. Test case: `find -t "lab tutorial"`, then
      `find --id a0123 -p 9123-4567 -e NUS.EDU`.<br>
      Expected: Each shows only Jason Lim: a phrase cannot be assembled across
      tags, and an absent optional field matches nothing.

1. Double quotes group phrases; apostrophes remain literal

   1. Prerequisites: Add `add Jason Wei Tan -p 96666666` and
      `add O'Brien -p 97777777`.
   1. Test case: `find -n Jason -n Tan`, then `find -n "Jason Tan"`.<br>
      Expected: The first shows Jason Tan and Jason Wei Tan, both separate
      terms being required; the second shows only Jason Tan, who holds the
      whole phrase.
   1. Test case: `find -n O'Brien`, then `find -n 'Jason Tan'`.<br>
      Expected: The apostrophe belongs to the name, so O'Brien is found. Single
      quotes do not group words, so the second identifies `Tan'` as an
      unexpected value.

1. Rejected input preserves the current results and saved data

   1. Test cases: `find Jason`, `find -t lab tutorial`, `find -t`,
      `find --unknown friend`, `find -t ""` and `find -t "lab`.<br>
      Expected: The first two read alike, since a bare term is the same mistake
      before and after an option, and both name `find -n Jason -t Lab -n Tan`
      and `find -n "Jason Tan"` as the forms that work. The rest name the
      missing value, unknown option, empty term and unclosed quote. The
      displayed list is unchanged after every refusal.
   1. Test cases: Run the two forms the second reply names,
      `find -t lab -t tutorial` and `find -t "lab tutorial"`.<br>
      Expected: Both are accepted and report their counts, so following the
      reply costs no second refusal.
   1. Test case: Run `list` and restart TAB.<br>
      Expected: All six students and their details remain, the saved file
      untouched by any search, successful or refused.
### Finding students by follow-up status

1. Status-only and combined searches preserve data

   1. Prerequisites: From an empty book, add `add Jason Tan -p 91111111 -t Lab -f`,
      `add Jason Lim -p 92222222 -t Lab` and `add Alice Lee -p 93333333 -t Tutorial -f`.
   1. Test cases: `find -f`, then `find -nf`.<br>
      Expected: The first shows Jason Tan and Alice Lee with `Found 2
      students.`; the second only Jason Lim. The long spellings behave
      identically.
   1. Test cases: `find -f -n Jason -t Lab`, then `find -n Jason -nf -t Lab`.<br>
      Expected: Only Jason Tan, then only Jason Lim, so the status and every
      field term must match and the status option may sit between field
      options.
   1. Test cases: `find -f -nf`, `find -f --follow-up` and `find -f true`.<br>
      Expected: A clear error for conflicting selectors, a repeated selector,
      and `-f takes no value. Use the option on its own.` for the last. The
      displayed list is unchanged, and restarting TAB leaves every student and
      status intact.
### Confirming before clearing all students

1. Confirmation, cancellation, and keyboard use

   1. Prerequisites: Add at least two students, run `find -n no-such-student`
      so none is visible, and keep a copy of `data/tab.json`.
   1. Test case: Type `clear` and press Enter.<br>
      Expected: The prompt counts every student including the hidden ones,
      warns that deletion cannot be undone, and ends with `[y/N]`, using
      `student` for one record and `students` for several. Neither the filtered
      list nor the file changes.
   1. Test cases: Reply `maybe`, then `"yes"`, then `exit`.<br>
      Expected: None runs. The feedback names the accepted replies and repeats
      the prompt, leaving the filter and file as they were.
   1. Test case: Reply ` NO `, then from a new `clear` press Enter without
      typing anything.<br>
      Expected: Each cancels, the file matching the copy, the empty reply being
      the default the capital `N` indicates.
   1. Test case: Enter `clear`, then ` YeS `.<br>
      Expected: Every student disappears, the saved book is empty, and
      restarting TAB shows an empty list.
   1. Test case: Add fresh records, enter `clear`, and paste `y` followed by
      U+200E LEFT-TO-RIGHT MARK.<br>
      Expected: The reply confirms and saves an empty book, an invisible mark
      not making a valid answer unreadable. A marked `yellow` stays invalid and
      keeps the confirmation pending.
   1. Test case: Enter `clear` on the empty book; then add a student, enter
      `clear`, close TAB before replying, and restart.<br>
      Expected: The first reports that the book is already empty with no
      confirmation pending, so `help` works at once. After the restart the
      student is still present and nothing is pending.
### Saving data

1. Loading a hand-edited data file

   1. Prerequisites: Add a recognizable student, close every TAB window using
      the file, and back up `data/tab.json`.
   1. Test case: Change that student's phone to `81234567`, remove another's
      `phone` key entirely and set a third's to `null`, then restart.<br>
      Expected: The edited value is shown and the two records without a number
      load with no blank line on their cards. Restarting again keeps all three,
      and `edit 1 -p 91234567` adds a number to one without giving the others
      a `phone` key.

1. Reading values TAB cannot accept

   1. Test case: With TAB closed, set a student's `phone` to `"12"`, then on a
      later run set another's `tags` to `"friends"` rather than a list.<br>
      Expected: Each refuses the file at startup, naming the student record and
      the field — the first `Phone "12" is not valid: a phone number needs at
      least 3 digits`, the second asking for a JSON array of strings. The file
      is left unchanged, so the value is reported rather than dropped.
   1. Test case: Set one student's `tags` to `[" t1 ", "T1", "Lab  3", "lab 3"]`,
      then on a later run add `"Needs follow-up"` to any record's `tags`.<br>
      Expected: The repeated spellings load as the two tags `t1` and `Lab 3`,
      keeping the first spelling of each. The reserved label refuses the file,
      pointing at `"flag"`; `"Needs follow-up soon"` is accepted, only the
      label itself being reserved.

1. Refusing a stale second window's save

   1. Prerequisites: Open two TAB windows from the same disposable folder
      before making any changes.
   1. Test case: In window A enter `add Alice Probe -p 81230001 --id A1111111X`,
      then in window B `add Bob Probe -p 81230002 --id A2222222X`.<br>
      Expected: A succeeds. B reports that the file changed, asks you to close
      it and restart, and changes nothing, keeping its own records and filter
      while A's file stays untouched. Restarting B shows Alice, after which
      adding Bob succeeds.

1. Preserving existing data when a save fails

   1. Prerequisites: Add a recognizable student, exit, keep a copy of the file,
      and make the `data` folder non-writable with `chmod 555 data`.
   1. Test case: Launch TAB, enter `add Bob Tan -p 91234567`, then `list` and
      `help`.<br>
      Expected: The add reports that TAB could not save for want of permission
      and Bob does not appear, the failed change being undone in memory. The
      other commands still work and the file matches the copy. Restore write
      permission afterwards.

1. Dealing with a missing or corrupted data file

   1. Prerequisites: Keep a copy of a valid multi-student `data/tab.json`.
   1. Test case: Replace the contents with `not json format!`, then on a later
      run restore the copy and remove the comma after one student's `"name"`.<br>
      Expected: Each refuses to open the main window, naming the file,
      reporting the line and column, and showing the affected line numbered
      with at most one line on either side. The file is left as it is.
   1. Test case: Change the second student's `nusStudentId` key to
      `nusStudentID`, keeping its value.<br>
      Expected: The error names the unknown key, the record and that student,
      and lists the supported fields, erasing no value. Correcting the spelling
      lets it load; a legacy `address` key is ignored rather than refused.
   1. Test case: Move `data/tab.json` elsewhere, then restore it and make the
      folder unreadable with `chmod 000 data`.<br>
      Expected: A missing file is treated as a first launch and opens with
      sample students, while an unreadable folder gives a dialog saying so and
      shows no sample students. Restore with `chmod 755 data`.

1. Loading files saved with different encodings

   1. Test case: With a student named `José Tan` saved, save the file as
      Windows-1252 (ANSI) and launch TAB, then reopen it as Windows-1252, save
      as UTF-8 and relaunch.<br>
      Expected: The first says the file is not valid UTF-8 and explains how to
      resave it, leaving it unchanged. The second loads with the accent intact,
      as does UTF-8 with a byte order mark.

1. More test cases to be added.
### Editing students using an explicit ID target

1. Prerequisites: Add `add Numeric Student -p 81234567 --id 001234 -f` and
   `add Other Student -p 82345678 --id A436`, then run `find -n Other`.
   1. Test case: `tag --id 001234 -r "Needs follow-up"`, then the command its
      reply suggests.<br>
      Expected: The first changes nothing and points at
      `edit --target-id 001234 --no-follow-up`. Running that clears Numeric
      Student's flag while Other Student stays displayed and the search stays
      active.
   1. Test case: `edit --target-id=001234 --id 1ABC`, then restart TAB.<br>
      Expected: Only that student's ID changes, and the new ID and cleared flag
      survive the restart.
   1. Test cases: `edit 1 --target-id 001234 -nf`, `edit --target-id "" -nf`
      and `edit --target-id Missing436 -nf`.<br>
      Expected: A mixed target, an empty ID and a nonexistent student are each
      reported without changing a record, the displayed list or the file. A
      bare `edit 001234 -nf` still reads as an index.
