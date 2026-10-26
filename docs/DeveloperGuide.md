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

`CommandBox` owns its `CommandHistory`, `CommandTextField` and `HighlightedTextOverlay`. The overlay is what calls the static `CommandHighlighter.highlight` method to color the input, leaving `CommandBox` with running the command, recalling earlier ones and marking a failure. The card controls extend JavaFX classes directly: `CardHeadline` extends `Pane`, `CardName` extends `Label`, `TagRow` extends `Region`, and `ContactRow` extends `FlowPane`. These controls are composed in `StudentListCard.fxml`.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/ui/MainWindow.java).

The `UI` component,

* executes commands entered by the User using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Student` objects from the model.

`AddCommand` includes the new student in its `CommandResult`.
`MainWindow` asks `StudentListPanel` to reveal that card only after `Logic.execute`
returns successfully, so failed saves cannot scroll to an unsaved student.
Scrolling leaves focus in the command box and uses the displayed list after
the existing search-preservation rules have been applied. It locates the exact
saved student object so an earlier record with identical details is not revealed
instead, and skips a target that has left the displayed list before layout.

The student card's `TagRow` creates a label for every tag, with its full text.
Layout shows as many complete chips as fit and lists all hidden tags in the
overflow chip's tooltip. There is no fixed chip count or label length cap.

English numeric feedback and data-load diagnostics format with `Locale.ROOT`,
and command highlight style names use the same locale when lowercasing. This
keeps digits and CSS selectors consistent without changing the JVM locale or
student data.

`SummaryPanel` wraps complete tag names within the fixed 150 px sidebar.
Each count retains its preferred width, while the tag list scrolls vertically
as wrapped rows grow. Names are not shortened, so tags differing only at the
end remain distinguishable without hovering.

`MainWindow` handles plain Tab and Shift+Tab on the student page to keep a
stable focus cycle: command box, student list, summary tag list, then the
reply when visible. The student list stays one Tab from command entry even
after a reply appears; Shift+Tab returns directly. Empty lists stay reachable.
`SummaryPanel.fxml` makes the tag `ScrollPane` focus-traversable, and the theme
marks its focus with an accent outline without changing its layout size.
JavaFX supplies arrow and Home/End navigation for both lists, plus paging for
the student list. `SummaryPanel` handles Page Up/Down using the tag viewport's
height and content overflow: JavaFX's default `ScrollPane` page keys otherwise
use the same increment as its arrow keys. The command box retains its separate
Up/Down history behavior. Help uses its own pager and restores command focus
when closed.

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
The reference is drawn as spans of `Text` in a wrapped `TextFlow` inside a
scrollbar-free viewport, which takes the remaining window height. `HelpText` gives
each span the class its token asks for — heading, command word, option, placeholder
or plain — reusing the command box's own colours, so a marker reads the same where
it is described as where it is typed. A command word carrying punctuation stays
plain, since it is being named in a sentence rather than shown as syntax, and a
two-letter capital such as `ID` is prose rather than a field.
Page movement uses the current viewport and the font those spans share, so resizing
and different fonts do not skip text. A persistent footer shows progress and navigation keys.
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

Parser and highlighter syntax characters are defined by `CommandTokenizer`,
so quoting, escaping and attached option markers stay consistent. Each keeps
its own scanning loop because highlighting must also accept unfinished input.
`ParserUtil.parseFollowUpSelector` decodes the optional follow-up state for
both find and edit. Edit performs its full duplicate-option check first, then
checks the selector before collecting field errors, preserving error priority.
Email and ID edits share clear-or-parse handling; each retains its own validator
and descriptor setter. Add and edit share
`StudentIdValidation.requireNoDuplicate`, with edit permitting the replaced
record to retain its ID.

#### Restoring window geometry

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

#### Fitting names and tags on a card

`CardHeadline` prefers a readable start of the name when allocating room to
tags, but yields part of that allowance for a complete tag count. The name
retains space for a character from each end. When no tag chip fits, `TagRow`
uses the shorter `N tags` total instead of `N more…`, preserving a complete
count beside the name. Its tooltip still lists every hidden tag. A partial
chip requires room for four characters and an ellipsis, measured in its
styled font with padding, so a two-letter fragment gives way to the total.
A shorter tag can still appear in full.
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

#### Coloring the command line

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
User Guide invites the User to rely on. That means two splits the coloring
cannot make with the rule it uses for values. `StudentBookParser` ends the
command word at the first separator whatever the quotes, so `add"Ravi s/o
Kumaran"` is the command word `add"Ravi` and two words after it; reading that
word with the value rule would draw one bold quoted word and then report an
unknown command the colors never showed. `FlagTokenizer` reads `--tag="Lab
3"` as an option and a value, so the marker and the equals sign are colored
as the option and the rest as its value. A marker holding a quote or a
backslash is left whole instead, because either moves the equals sign the
parser counts to, and a word split in the wrong place would be the same bug
again.

A `TextField` cannot color its own content, so `CommandBox` keeps the field
for input and hands `HighlightedTextOverlay` the nodes that draw a colored
copy of the command behind it, with the field's own text transparent. The
overlay owns that copy, the caret and the selection band. The field still takes every keystroke and still decides
which character a click lands on, which keeps editing exactly as it was.

`CommandHighlighter` decides the colors. It returns spans covering every
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
command is colored while it is still half-typed.

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
  text the coloring was told about have both settled. The copy therefore
  follows the node's own position. The node is the one the field is drawing
  in, since an empty line shows the prompt in a node of the same kind which
  the field hides rather than throws away, and a command reading as the
  prompt would otherwise be taken for the hidden one.
* **the caret.** The field's caret is drawn in its text color, which is
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
and of every color the line uses against that band, including the one a
refused command is written in, so that selecting a command cannot hide half of
it. The bar it holds them to is 3:1 rather than the 4.5:1 WCAG asks of text
this size: on a window this dark, a band light enough to be seen against the
line (2.2:1) lightens the background enough that 4.5:1 would need every token
color moved towards white, which is not the palette the mockup sets. The
trade-off is deliberate and worth revisiting if the palette changes.

#### Recalling session commands

`CommandBox` records each submitted command in `CommandHistory` before execution,
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

#### Summarizing the list

`SummaryPanel` shows the flagged count, a count per tag and the total, beside
the list. It counts `Logic#getFilteredStudentList()`, which is the only list
`Logic` exposes, so it summarizes **what the list is showing** rather than the
whole student book: after `find -n Alex` it counts the matches. That is the
reading the User Guide documents, and it keeps the panel answering the same
question as the list it sits beside. Summarizing the whole book instead would
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

`JsonStudentBookStorage` reports data-folder/file collisions and save-lock failures
at the operation boundary, where the failing path and its purpose are known.
It preserves permission exceptions for the existing permission-specific feedback
and never removes an obstructing file or folder. Logic rolls back records and the
filter after these failures just as it does for other save errors.

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

`JsonTagsDeserializer` checks the array and string types before adapting tags,
so malformed `tags` values report the field and entry position rather than
Jackson class names. Missing or null arrays still mean no tags; tag text
continues through the existing model validation. Mapping errors retain their
source location and student record number, and do not rewrite the file.

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
`add NAME [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up]` and
`edit TARGET [-n NAME] [-p PHONE] [-e EMAIL] [--id NUS_STUDENT_ID] [-t TAG]... [--follow-up | --no-follow-up]`,
where AB3 marked them with prefixes such as `p/`. The `edit` target is a
shown index, a letter-leading NUS Student ID, or `--target-id NUS_STUDENT_ID`.
A dedicated target option keeps selection unambiguous when `--id` sets or
clears the ID field, and permits digit-leading IDs without changing how bare
numbers select displayed indices. `EditCommandParser` reuses
`ParserUtil.parseRequiredStudentTarget` for preamble targets and
`ParserUtil.parseNusStudentId` for explicit targets, building a `StudentTarget`
in either case. Duplicate target options and mixed preamble/option targets
are rejected before execution. Selecting a target alone does not count as a
field edit. ID targets resolve across the whole book, preserving the search
predicate after editing and using the existing persistence path.
The value options have descriptive long aliases: `--name`, `--phone`,
`--email`, and `--tag` correspond to `-n`, `-p`, `-e`, and `-t` respectively.
`add` takes its name in the preamble. `edit` accepts `-n`/`--name` to change a
name, and `find` accepts the same option to search names.

A prefix has to be a sequence a value never contains, and no such sequence
exists for a name. `s/o`, `d/o`, `a/l` and `a/p` are ordinary parts of
Singaporean and Malaysian names, and `a/l` was in fact unreadable for as long
as an address field was marked `a/`. Widening the prefixes would only move the
collision somewhere else. Options move the problem out of the value entirely:
an option is a whole token, so a name is read as a name however it is spelled.

Parsing runs in two passes, the way a shell does it.

`CommandTokenizer` splits the arguments on whitespace and lets double quotes
group a value containing whitespace. Straight, curly and full-width double quotes
are interchangeable delimiters, including in mixed pairs, to accept input from
input methods and pasted documents. `CommandTokenizer#isQuote` shares this rule
with the syntax highlighter; all supported quotes can be escaped to retain them
literally. A token records whether a quote took part in it,
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
them. Each short and long marker resolves to the same `CliFlag`, so mixed aliases
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

The presence-only `--follow-up` option (alias `-f`) and
`--no-follow-up` option (alias `-nf`) instead record an empty value and consume
no following token. `add` accepts only `--follow-up`; `edit` accepts both to set
or clear the status, and `find` accepts both to filter by that status. This lets
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

`CliFlag` also lets a value-taking option carry aliases. NUS Student ID uses
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
unquoted negative number as both an invalid index and an unknown option.
For numeric tag targets, `parseRequiredIndex` reports a value following a
valid index; for edit and letter-leading tag targets,
`parseRequiredStudentTarget` reports a value following a valid target.
Tag syntax failures and a missing action option include guidance for both
addition and removal.

`ParserUtil.parseIndex` distinguishes malformed/non-positive input from a positive
integer exceeding the supported index range. `StudentTarget` reports out-of-range
positions with their original input spelling and the current displayed count,
including zero. Invalid non-positive or non-integer targets of edit, tag and
delete retain the concise positive-integer reason from `parseIndex`, alongside
any field errors; delete adds the explicit `--id` form whenever the refused
target reads as a valid NUS Student ID.
Retaining that spelling also lets delete suggest an exact `--id`
command for a stored numeric ID without dropping leading zeroes. An in-range
number remains an index even when another record holds the same numeric ID.
The original spelling affects diagnostics only, not target equality or hashing.

`TagCommand` stores a `StudentTarget`, as edit and delete do. It resolves an
index against the displayed list and an ID across the entire book, so a
roster ID remains usable while a search hides the student. It compares the
normalized tag names ignoring case before copying the existing tag set and
constructing a replacement student with every other field preserved. The
search filter stays active; removing a tag used by that filter may hide the
student from the displayed list. `TagCommand.Action` selects addition or
removal while sharing target resolution, matching, student replacement and
feedback.
`ParserUtil.parseTags` folds case variants in `add` and replacement tags in `edit`
using `String.CASE_INSENSITIVE_ORDER`, matching the tag command and summary.
`ParseProblems` retains input order so the first normalized spelling wins while
still collecting every invalid tag's diagnostic. `JsonAdaptedStudent` uses the same
case-insensitive ordering after validating and normalizing stored tags, keeping
the first spelling in the JSON array. This makes loaded cards and tag summaries
agree; the next successful save writes one tag per group. Tag equality remains
unchanged. Removal removes every matching variant of the requested group while
retaining unrelated tags. A general tag named `Needs follow-up` remains separate
from the follow-up flag, though the parsers no longer let one be added. Duplicate and successful removal feedback use a matching
stored tag's spelling. If several case variants match, the reply chooses the
first in the card's tag display order.

For a hidden flagged student, the absent-tag hint supplies
`edit --target-id NUS_STUDENT_ID --no-follow-up` with the student's stored ID.
This addresses even digit-leading IDs without changing the active search.

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
substitutions and swaps of neighboring characters turn one word into the
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
dispatches with, so `Delte` is read as an attempt at `delete`; a word spelled
correctly in any case is dispatched before it reaches here. The message quotes
the word as it was typed, because a User who typed capitals needs to see them
to recognize what they did. The quoted word is shortened the
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

`Student` stores phone, email and NUS Student ID as `Optional<Phone>`,
`Optional<Email>` and `Optional<NusStudentId>`. Both constructors require non-null arguments;
`Optional.empty()` explicitly represents an unknown detail. One constructor
accepts the follow-up boolean, and the other defaults to unflagged for sample
data. There are no overloads accepting nullable email or ID values, and no
`Student` field is null. Add, edit, storage conversion, and test builders pass
optionals directly into this contract. Nullable JSON strings remain confined
to the storage adapter, preserving the existing file format.

`Name`, `Tag`, `Phone`, `Email` and `NusStudentId` each store the form returned by
`StringUtil.normalizeFieldValue`: without invisible format characters such as
direction marks and non-whitespace controls, with each run of whitespace reduced
to one ASCII space
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

Nothing else is rejected in `Name`, `Tag` or `Phone`. A phone number may hold `+`,
whitespace, brackets and an extension; a tag may hold whitespace, hyphens and any
script. None of those characters can hinder TAB, because none of these
fields is parsed, dialled, or used to build a file path.

**Phone text intentionally has no maximum length.** Teaching assistants may
record several numbers, extensions and contact annotations in one field. A fixed
cap could reject useful contact details or prevent an existing saved record from
loading. The minimum of three Unicode decimal digits still applies. Acceptance
of a 400-digit value alone does not establish a usability, performance or
persistence problem that justifies a cap. Email's separate length restriction
addresses its validation and storage concerns; it is not a reason to restrict
phone text. Reconsider this decision if a concrete problem is demonstrated,
with a migration plan for existing data and a limit justified by contact use
cases. Long phone presentation uses ellipses and complete-value tooltips rather
than truncating the stored text.

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

`EditCommand` first compares the prospective record with the selected record using
full equality. An unchanged edit returns an explicit no-change response before
checking duplicates or replacing the student, preserving the filter and avoiding
unnecessary list notifications. `LogicManager` continues to skip saving an unchanged book.

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

The commands collect the warnings before mutation and separate them from the
ordinary success message with a blank line. Matches are grouped by field into
one prominently labeled `WARNING: Possible duplicate student` line. A line
names at most two students and summarizes further matches as `N others`, so a
shared placeholder does not fill the result box. The review advice appears
once after every matching field. Phone warnings omit the parenthetical value:
matched records can store different formatting for the same digits, so no single
formatted value represents all named students.

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
other fields. `EditStudentDescriptor` stores an `OptionalFieldEdit<Email>`
that encapsulates the value and edit intent; `isEmailEdited()` is derived from
that state. An unedited field leaves the existing value alone, an edited field
with an empty optional clears it, and an edited field with an address replaces
it. `setEmail` requires an address and `clearEmail` explicitly requests removal.
Both mark the email as edited, so callers cannot toggle the intent independently
of the replacement value. The `OptionalFieldEdit` copy constructor preserves
all three states. The descriptor is not persisted, so its state does not affect
JSON.

The parser leaves the email untouched when `-e` is absent, calls `clearEmail`
for a token that is empty after trimming, and sets a replacement only after
validation succeeds. A rejected value is recorded as a parse problem without
assigning a null email or requesting a clear. A bare option is still an error.
A clear satisfies the parser's requirement for a provided field even when the email
is already absent; execution then reports no changes if no other value changes. `EditCommand`
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
same number, hold none at all, and `isSameStudent` reads neither case. It is
normalized only so that a number typed with unusual whitespace, or wrapped in the
invisible marks a chat app puts around a copied number, is stored consistently.

**The phone number may be absent.** A class roster can give a name, an NUS
Student ID and a university email without giving a personal mobile number, so a
student is recorded before a number is known rather than one being invented.
This follows the optional email in every layer, so that one pattern covers both
details. `Student` accepts, stores and exposes `Optional<Phone>`.

`AddCommandParser` wraps the parsed value in an optional and no longer reports a
missing `-p`; the name is the only field it still requires. `EditStudentDescriptor`
holds an `OptionalFieldEdit<Phone>`, so `edit INDEX -p ""` clears the number and a
value replaces it, exactly as `-e ""` clears an email. A number that is supplied
is still validated, and a rejected one is recorded as a parse problem rather than
clearing the field.

`JsonAdaptedStudent` shares `normalizeOptionalValue` with the email: a missing
`phone` key, JSON `null`, and a string that is empty after normalization all load
as absent, while a malformed non-null number rejects the data file instead of
being silently dropped. Jackson omits the key on save when no number is present,
so files written before the phone became optional continue to load and a saved
record stays editable by hand.

`StudentCard` leaves the phone label unmanaged when the number is absent, so the
remaining contact details close up rather than keeping a blank line, and
`Messages.format` omits the `Phone:` label. The `PHONE` search field returns false
for a student without a number, consistent with every other absent optional field.

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
The allowed-character check uses a possessive quantifier to scan long phone terms
without backtracking. Digit normalization separately checks that at least one digit
remains, so punctuation-only terms cannot match every phone.
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
`DataLoadingException`, and `JsonParseErrorFormatter` words the
explanation it carries. For a syntax error, the formatter uses the
deepest parser exception with a usable location so that the displayed
line and column identify the offending token rather than the last
successfully mapped value. It pairs the concise parser explanation with
the affected line and at most one line on either side. Each source line
is limited to 160 characters; an affected long line is cropped around
the reported column so that minified JSON remains useful without
filling the dialog. If the parser supplies no usable location, the
formatter omits the source instead of falling back to Jackson's
source-bearing message.
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

1. Initial launch

   1. Download the JAR file, copy it into an empty folder, and double-click
      it.<br>
      Expected: The GUI opens with a set of sample student records. The window
      size may not be optimal.

1. Keeping application files with the JAR

   1. Test case: Launch the JAR from its folder, add a recognizable student and
      exit. Then open a terminal in a different folder and launch the same JAR
      by its absolute path.<br>
      Expected: The folder beside the JAR holds `data/tab.json`,
      `preferences.json` and `tab.log.0`. The recognizable student is present,
      the status bar shows the absolute path to that same file, and nothing is
      written in the terminal's own folder.

1. Reading a long data path

   1. Test case: Launch from a folder with a long absolute path and narrow the
      window until the status bar path no longer fits.<br>
      Expected: The path is shortened in the middle, keeping `tab.json`
      visible, and hovering shows it in full. Widening until it fits removes
      the tooltip.

1. Launching from a read-only folder

   1. Prerequisites: Copy the JAR into a folder the account can read but not
      write: `chmod 555 FOLDER` on Linux or macOS, or remove write permission
      for the test account on Windows.
   1. Test case: Run `java -jar tab.jar` from that folder, then
      `add Read Only Test -p 91234567`, then `list` and `exit`.<br>
      Expected: The GUI opens with sample records. The terminal warns that it
      cannot write `tab.log` and names `preferences.json`, each warning without
      a stack trace or `SEVERE` label, and with no
      `ExceptionInInitializerError`. The add reports a save error and TAB stays
      open with the record absent, since the failed change is rolled back.
      `exit` closes TAB with one preferences warning.

1. Full-page help pager

   1. Test case: Enter `help`, then press Space, b, j, k, g and G.<br>
      Expected: Help replaces the student list, sidebar, command box and result
      area, opening no window or browser. The notice reads `Help mode: press q
      or Esc to return to commands.`. Each key moves as documented, G reaches
      the last hint and the footer says `(END)`, and paging past either end
      stays there. Repeat at minimum window size: all text stays reachable
      without scrollbars.
   1. Test case: Read the first screenful, quit with q, and type
      `add NAME -p PHONE`.<br>
      Expected: `COMMANDS` and the other margin headings stand out from the
      body. In both places `add` is bold in the accent colour, `-p` carries the
      option colour and `NAME`/`PHONE` the value colour. In help the brackets
      around an optional field stay in the body colour, and prose lines such as
      `help, list, clear and exit take no arguments.` carry no colour of their
      own.
   1. Test case: Type `find -n Alex` without submitting it, press F1, then q.
      Press c while help is open.<br>
      Expected: The unfinished command returns unselected with the caret at the
      end, so typing ` Tan` leaves `find -n Alex Tan`. No q is inserted. c
      copies the User Guide address and the footer confirms it.
   1. Test case: Enter `help oops`.<br>
      Expected: `help takes no arguments. Unexpected input: ...` quotes the
      offending input, no page opens, and student data is unchanged. `help`
      with trailing spaces still opens the pager.

1. Keyboard navigation through students, tags and replies

   1. Prerequisites: In a disposable copy, add enough students with distinct
      tags that both the student list and the tag list overflow, for example
      `add Navigation 01 -p 90000001 -t tutorial-group-01` through
      `Navigation 30`.
   1. Test case: From the command box press Tab, then Down, Page Down, End and
      Home; press Tab again for the tag list and repeat.<br>
      Expected: Each list takes an accent outline when focused. Arrows move or
      scroll by one, page keys by a screenful, and End and Home reach the last
      and first entries. Repeating past either end stays there. Shift+Tab
      returns towards the command box and student records are unchanged.
   1. Test case: Submit `list`, type `find -n Navigation` without submitting,
      and press Tab repeatedly, then Shift+Tab.<br>
      Expected: The cycle is command box → student list → tag list → reply →
      command box, reversed by Shift+Tab, with the student list still one Tab
      from the command box once the reply appears. The draft is unchanged and
      there is no focus trap.

1. Saving window preferences

   1. Test case: Resize and move the window, close it, and relaunch.<br>
      Expected: The size and position return when they fit the display's usable
      area.
   1. Test case: With TAB closed, set `windowWidth` and `windowHeight` in
      `preferences.json` to `1.0E9` and `windowCoordinates` to
      `{"x":99999,"y":99999}`, then relaunch.<br>
      Expected: The window fits the primary display, with its title bar,
      command box and status bar reachable. Closing and relaunching keeps the
      corrected bounds and leaves student data unchanged.
   1. Test case: With TAB closed, replace `preferences.json` with
      `{"guiSettings": null}`, launch, then close and launch again.<br>
      Expected: The first launch opens with default window settings, shows the
      saved student, reports that preferences could not be loaded followed by
      `GUI settings must not be null.`, and rewrites a valid file. The second
      launch opens normally and the data file is untouched. Replacing the whole
      file with `null` behaves the same way, reporting
      `The JSON document must not be null.`

1. Narrow student cards

   1. Test case: Add
      `add Narrow Card -p "+65 9123 4567" -e a.very.long.address.indeed@u.nus.edu --id AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA`,
      resize the window to its minimum width, then widen it slowly.<br>
      Expected: The ID and email shorten as needed, the phone stays readable,
      and the email stays inside the card's details area. Worth checking on
      Windows as well, since font widths differ.
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
### Typing a command word in another case

1. A correctly spelled command word in capitals

   1. Test cases: `List`, `LIST` and `Help`.<br>
      Expected: Each runs the command of that name, so the students are listed
      and help opens. No reply names a word TAB does not know.
   1. Test case: `ADD McDonald Tan -p 91234567`.<br>
      Expected: A student named `McDonald Tan` is added with the capitals as
      typed, on the card and in the file, the fold reaching the command word
      alone.
   1. Test case: `LIST oops`.<br>
      Expected: `list takes no arguments. Unexpected input: "oops".`, naming
      the command rather than the capitals typed, and nothing changes.
### Mistyping a command word

1. A command word one slip away from a real one

   1. Prerequisites: At least one student listed, and a copy of `data/tab.json`.
   1. Test case: `delte 1`, then `Delte 1`.<br>
      Expected: Each reply quotes the word as it was typed and suggests
      `delete`. Nothing is deleted, the file matches the copy, and the text
      stays in the command box with the line red, so the missing letter can be
      typed in rather than the command retyped.
   1. Test cases: `lsit`, `hepl`, `ls` and `felete 1`.<br>
      Expected: `list`, `help`, `list` and `delete` are suggested; two letters
      out is still near enough, and a wrong first letter does not prevent a
      suggestion. None of them runs.
   1. Test cases: `del 3` and `cle`.<br>
      Expected: A word stopped partway still suggests `delete` and `clear`,
      and nothing is deleted or cleared.
   1. Test case: `e`.<br>
      Expected: Both `edit` and `exit` are named, in that order every time.

1. A command word resembling nothing TAB knows

   1. Test cases: `qwerty`, `dlt 1` and `日本語`.<br>
      Expected: Each quotes the word and points to `help` with no suggestion.
      `dlt` is two letters from `edit` but opens with a letter that command
      does not, so TAB offers nothing rather than guessing.
   1. Test case: Paste a line of at least 80 characters with no spaces.<br>
      Expected: The reply quotes the opening 60 characters followed by an
      ellipsis and still ends with the pointer to `help`, rather than running
      off the result display.
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
      once: --follow-up. Give each listed option only once.`; the second
      reports that `true` belongs to no option; the third says the two
      follow-up options cannot be used together.

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
### Adding one tag

1. Preserving the student's other details and saving the new tag

   1. Prerequisites: Add `add Tag Test -p 91234567 -e tag@example.com
      --id A01234567X -t T1 --follow-up` as one line, then `find -n "Tag Test"`.
   1. Test case: `tag 1 --add "Lab 3"`.<br>
      Expected: The reply starts `Added tag [Lab 3]:`. The student holds both
      `T1` and `Lab 3`, every other detail and the follow-up status are
      unchanged, and the search stays active. Restarting TAB keeps all of it.

1. Tagging by stable student ID

   1. Prerequisites: Add `add ID Tag Test -p 92345678 --id E7654321 -t T1` and
      `add Numeric Tag Test -p 93456789 --id 1234`, then run
      `find -n NoMatchingStudent` so both are hidden.
   1. Test case: `tag e7654321 --add="Lab 3"`, then `tag -i 1234 -a T2`.<br>
      Expected: Each addresses the right student although neither is displayed,
      preserving the active search and every unrelated field. A bare
      `tag 1234 -a T2` stays an index request and fails while fewer than 1234
      students are shown.
   1. Test case: `tag 1 --id E7654321 -a T2`.<br>
      Expected: The reply asks for one target, and nothing changes.

1. Rejecting duplicates and invalid input without mutation

   1. Prerequisites: Return to Tag Test with `find -n "Tag Test"` and keep a
      copy of `data/tab.json` to compare after each refusal.
   1. Test case: `tag 1 -a "  lab   3  "`.<br>
      Expected: `This student already has tag [Lab 3]. No changes made.`, with
      the existing spelling, the displayed list and the file untouched.
   1. Test case: `tag 1 -a "Needs follow-up"`, then the same in another case
      and spacing.<br>
      Expected: Both are refused, naming `--follow-up` as the way to mark the
      student instead.
   1. Test cases: `tag 0 -a T2`, `tag 1 -a ""`, `tag 1 -a` and
      `tag 1 -a T2 --add T3`.<br>
      Expected: Each gives the matching index, invalid-tag, missing-value or
      duplicate-option error, leaving the student, the list and the file as
      they were.
   1. Test cases: `tag 1 -t T2` and `tag 1 lab4`.<br>
      Expected: Both show the `-a`/`--add` and `-r`/`--remove` guidance with
      examples, the first naming the unknown `-t` option and the second naming
      `lab4` as an unexpected value after the index.
### Removing one tag

1. Preserving other details and persisting removal

   1. Prerequisites: Continue from the tag-addition tests, with Tag Test
      holding `T1` and `Lab 3` and needing follow-up.
   1. Test case: `tag 1 --remove "  LAB   3  "`.<br>
      Expected: The reply starts `Removed tag [Lab 3]:`. Only that tag goes;
      `T1`, the contact details and the follow-up status remain, and the search
      stays active. Restarting TAB keeps the result.

1. Reporting absent tags and invalid input without saving

   1. Test case: `tag 1 -r "Lab 3"` again, now that it is gone.<br>
      Expected: `This student does not have tag [Lab 3]. No changes made.`, and
      nothing is written.
   1. Test case: `tag 1 -r "Needs follow-up"` on this flagged student.<br>
      Expected: A second line reads `This student is flagged for follow-up,
      which is not a tag. To clear the flag, use edit 1 --no-follow-up.` The
      label stays until that `edit` runs. On an unflagged student only the
      first line appears.
   1. Test cases: `tag 0 -r T1`, `tag 1 -r ""`, `tag 1 -r` and
      `tag 1 -a T2 -r T1`.<br>
      Expected: Index, invalid-tag, missing-value and conflicting-action errors
      respectively, with no record, list or file change.

1. Removing a tag that supplies the search match

   1. Test case: `tag 1 -a "Lab 3"`, `find -t "Lab 3"`, then `tag 1 -r "Lab 3"`.<br>
      Expected: The student leaves the filtered list. `list` shows them again,
      without `Lab 3`.

1. Removing variants from a human-edited data file

   1. Prerequisites: With TAB closed, set Tag Test's `tags` to
      `["Lab 3", "  lab   3  ", "T1"]`, keep `"flag": true`, restart, and run
      `find -n "Tag Test"`.
   1. Test case: `tag 1 -r "LAB 3"`.<br>
      Expected: The variants load as the single tag `Lab 3`, which is removed,
      leaving `T1`, the flag and every other field in place after a restart.
### Adding a student

1. Adding a student with minimal fields

   1. Test case: `add John Doe -p 98765432`<br>
      Expected: John Doe appears in the list and the reply reads
      `New student added: John Doe; Phone: 98765432`, with no placeholder
      labels for the absent email or tags.
   1. Test case: `add Email Only --email=e1234567@u.nus.edu --id=E1234567`,
      then `add Name Only`.<br>
      Expected: Both are added, the name being the one required detail. The
      first reply carries no `Phone:` label and the card leaves no gap where a
      number would be; reopening TAB keeps it that way. Only a malformed value
      is refused, as `-p 12` is with
      `Phone "12" is not valid: a phone number needs at least 3 digits`.

1. Adding a student with tags and follow-up

   1. Test case: `add Jane Doe -p 91234567 -t urgent -t lab1 --follow-up`<br>
      Expected: The reply reads `New student added: Jane Doe; Phone: 91234567;
      Tags: [lab1][urgent]; Needs follow-up`, the tags sorted alphabetically
      ignoring case.

1. Revealing a newly saved student in an overflowing list

   1. Prerequisites: Use a book with at least 30 students, scrolled to the top,
      with focus in the command box.
   1. Test case: `add Newly Added Student -p 91234567`.<br>
      Expected: The new card is visible without scrolling by hand, and the next
      command can be typed straight away.
   1. Test case: Return to the top, then try `add -p 91234567` and an addition
      with a duplicate NUS Student ID.<br>
      Expected: Neither adds a student and the list does not jump to the end.

1. Recovering from a name option or a missing name

   1. Test cases: `add --name "Betsy Crowe" --phone 1234567`, then
      `add -p 1234567`, then `add Betsy Crowe --phone 1234567`.<br>
      Expected: The first names the unsupported option, followed by `In the add
      command, the name comes first, without an option: add NAME -p PHONE ...`.
      The second reads `Missing required field(s): NAME` with the same hint.
      The third adds Betsy, and she survives a restart.

1. Adding a duplicate NUS Student ID

   1. Prerequisites: `add John Doe -p 91234567 -i A05678901K`.
   1. Test case: `add Jane Doe -p 98765432 -i a05678901k`<br>
      Expected: Nobody is added. The reply names NUS Student ID `A05678901K`
      and `John Doe`, and no record is changed.

1. Adding during a search

   1. Prerequisites: In an empty book add `add Alice Tan -p 91110001 -t T1`,
      `add Bob Lim -p 91110002 -t T2` and `add Carl Ong -p 91110003 -t T1`.
   1. Test case: `find -t T1`, then `add Eve Ng -p 91110005 -t T1`.<br>
      Expected: Alice, Carl and Eve appear in that order, with no
      search-cleared notice. `delete 2` then removes Carl, leaving Bob
      untouched.
   1. Test case: `add Dan Koh -p 91110004 -t T2`.<br>
      Expected: All four remaining students appear and the reply ends
      `Search cleared to show the new student.`, so indexes need re-reading
      before the next index command.
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
      Expected: Both names appear in full when there is room, with no tooltip.
      As the cards shrink each keeps its start and its end around the ellipsis,
      so the two stay distinguishable rather than collapsing to the same text,
      and hovering shows the whole name.

1. Keeping names readable beside tags and a follow-up marker

   1. Test case: Add `add Bernice Xu -p 81234567 -t attendance-low -t consult
      -t follow-up -t lab -t midterm -t tutorial -f`, narrow the window to its
      minimum, then widen it slowly.<br>
      Expected: The name keeps its opening letters. At the minimum width the
      count-only chip reads `6 tags`; once chips appear the count reads
      `N more…` for the hidden ones, and hovering lists them.

1. Returning unused count space to a short name

   1. Test case: Add `add Tan Ah Kow -p 94445555 -t attendance-low -t consult
      -t "Project A/B" --follow-up` and narrow the window to about 520 px.
      Then clear the marker with `edit INDEX -nf` and resize again.<br>
      Expected: Where the tags collapse to `3 tags` and the name plus count
      fit, `Tan Ah Kow` shows in full rather than being shortened beside a
      blank gap. The name takes the room the marker releases.

1. Reading every tag that fits

   1. Test case: Add `add Alex Yeoh -p 87438807 -t T01 -t "Lab 3" -t W5
      -t consult` and widen the window to about 1400 px. Then add
      `add Irfan Ibrahim -p 92492021 -t "CS2103T Tutorial W09"
      -t "CS2103T Tutorial W10"` and narrow and widen repeatedly.<br>
      Expected: All four chips appear alphabetically with no count when there
      is room. Chips that fit appear in full, hidden ones are counted and
      listed on hover, and a lone chip too wide for its allocation is shortened
      with its complete tag on hover. Widening restores them.

1. Reading tags beside a long name and follow-up marker

   1. Test case: Add `add Siti Nur-Aisyah Binte Abdullah Rahman -p 91234567 -f
      -t consultation-week-3 -t "Lab 3" -t "Tutorial Group 11"` as one command,
      then resize through about 740 px.<br>
      Expected: The row shows a readable tag prefix with `2 more…`, or `3 tags`
      where that prefix will not fit. It never shows only one or two letters of
      the first tag, and the follow-up marker stays visible.
### Long value-option names

1. Adding and editing with long aliases

   1. Prerequisites: Launch, run `clear`, and reply `yes`.
   1. Test case: `add Alice Tan --phone 91234567 --email alice@example.com
      --tag T1 -t "Lab 3"` as one line, then `edit 1 --name "Alice Lim"
      -p 81112222 --tag T2`.<br>
      Expected: Alice is added with both tags, the quoted one keeping its
      space, so the short and long spellings work together. The edit changes
      each named field and drops `T1` and `Lab 3`, an edit replacing the tag
      set rather than adding to it.
   1. Test case: `edit 1 --email ""`, then `edit 1 --tag ""`.<br>
      Expected: The first removes her email and the second every tag, with no
      other field touched.

1. Treating aliases as one logical field

   1. Test case: `edit 1 -p 81112222 --phone 82223333`, then
      `edit 1 -t T1 --tag "Lab 3"`.<br>
      Expected: The first changes nothing and reports the phone as given more
      than once, although the two values use different aliases. The second
      succeeds with both tags, a tag option being allowed to repeat.

1. Rejecting malformed long options without changing data

   1. Test cases: `add Bob Lim --phone`, `add Bob Lim --unknown value` and
      `edit 1 --phone`.<br>
      Expected: Nothing is saved. A missing value names `--phone PHONE`; an
      unknown option names `--unknown`. Alice's details and the file are
      unchanged.

1. Joining option values with an equals sign

   1. Test case: `edit 1 --name="Alice Tan" -p=81112222
      --email=alice@example.com --id=a01234567x --tag="Lab 3"` as one line,
      then `tag 1 --add=needs=review`.<br>
      Expected: Every field changes, the name and tag keeping their spaces and
      the ID stored as `A01234567X`. The tag `needs=review` is added, only the
      first equals sign separating the option from its value.
   1. Test case: `edit 1 --email=`, then `edit 1 --email="" --id="" --tag=""`.<br>
      Expected: The bare form changes nothing and reports that the option needs
      a value, since an unquoted empty value may be an accidental omission. The
      quoted form clears the email, the ID and every tag, quoting making each
      omission deliberate.
   1. Test cases: `edit 1 --phon=1` and `edit 1 --follow-up=yes`.<br>
      Expected: Neither changes a field. The first reports that there is no
      `--phon` option, naming the marker without its value; the second reports
      that the option takes no value.
### NUS Student IDs

1. Adding optional NUS Student IDs with either option spelling

   1. Prerequisites: Launch from an empty folder, then `clear` and reply `yes`.
   1. Test case: `add Alice Tan -p 91234567 --id a01234567x`, then
      `add Bob Lim -p 92345678 -i A12345678Y`.<br>
      Expected: Both are added. Alice's card and the result show `A01234567X`,
      the lowercase input normalized to uppercase, and her saved record holds
      that form. `-i` behaves exactly as `--id`.
   1. Test case: `add Cara Ng -p 93456789`<br>
      Expected: Cara is added with no blank placeholder on her card, and her
      saved record has no `nusStudentId` key.

1. Rejecting invalid, missing and repeated values

   1. Test cases: try adding a differently named student with each of
      `--id ""`, `--id A0123-456X` and
      `--id ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567`.<br>
      Expected: No student is added. Each message identifies the NUS Student ID
      and says whether it is empty, longer than 32 characters, or holds a
      character other than an ASCII letter or digit. The saved file is
      unchanged.
   1. Test cases: `edit 1 --id` and `edit 1 --id A11111111A -i A22222222B`.<br>
      Expected: Alice is unchanged. The first says `--id NUS_STUDENT_ID` needs
      a value; the second reports the field as repeated, because the two
      spellings are aliases.

1. Replacing and clearing an NUS Student ID

   1. Test case: `edit 1 -p 81112222`, then `edit 1 -i A76543210Y`.<br>
      Expected: The phone change leaves her ID at `A01234567X`; the second
      command changes only the ID.
   1. Test case: `edit 1 --id ""`, then `edit 1 -i A01234567X` followed by
      `edit 1 --id "   "`.<br>
      Expected: The ID disappears from her card, feedback and saved record,
      with no other detail touched. A whitespace-only value is trimmed and
      clears it the same way.

1. Finding and persisting NUS Student IDs

   1. Test case: Restore the ID, restart TAB, then run `find --id a0123` and
      `find --id 23456`.<br>
      Expected: Each shows Alice, so matching is partial and ignores case, and
      the ID survived the restart.
   1. Test case: With TAB closed, change one `nusStudentId` to `A0123-456X`
      and relaunch.<br>
      Expected: A startup error names the file, the student record and the
      invalid ID. The main window does not open and the file is left unchanged.

1. Reporting unchanged edits

   1. Prerequisites: From an empty book, `add Amy Bee -p 91234567`, then
      `find -n Amy`.
   1. Test cases: `edit 1 -p 91234567` and `edit 1 -i ""`.<br>
      Expected: Each replies `Student already has the requested details. No
      changes made.`, leaving the student, the active search and the saved file
      as they were.
   1. Test case: `edit 1 -n "Amy Bee" -p 82223333`.<br>
      Expected: `Edited student: Amy Bee; Phone: 82223333`. Amy stays visible
      and repeating the command reports no changes.

1. Checking duplicate identity behavior

   1. Prerequisites: Start from an empty book.
   1. Test case: `add Alice Tan -p 91234567 -i A01234567X`, then
      `add Alice Tan -p 92345678 -i A76543210Y`, then
      `add Alice Tan -p 93456789`.<br>
      Expected: All three are added, because a shared name is not an identity
      key while the IDs differ or are absent. The later additions warn that
      other students share the name, the third grouping both records into one
      warning.
   1. Test case: `add Bob Lim -p 94567891 -i a01234567x`, then `list`, note the
      third Alice's index and run `edit INDEX -i A01234567X`.<br>
      Expected: Both are rejected, each naming NUS Student ID `A01234567X` and
      student `Alice Tan`, and no existing record changes.

1. Warning about likely duplicate students

   1. Prerequisites: From an empty book, add
      `add Alex Yeoh -p "+65 9123 4567" -e Alex@Example.com -i A01234567X`.<br>
      Expected: No warning, since no other student is present.
   1. Test case: `add "  aLeX   yEoH  " -p 81112222 -e other@example.com`<br>
      Expected: The record is saved and the result names Alex Yeoh and the
      matching `name`; case and repeated spaces do not prevent the match.
   1. Test case: `add Bernice Yu -p "+65 (9123)-4567" -e bernice@example.com`<br>
      Expected: Bernice is saved and the result names the matching `phone`,
      because the digit sequences match despite punctuation. The warning reads
      `Alex Yeoh has the same phone.` without quoting either number. Restart
      TAB; both numbers keep the formatting they were entered with.
   1. Test case: Clear the book, add four students named `Student One` through
      `Student Four` each with phone `000`, then `add Student Five -p 000`.<br>
      Expected: Student Five is saved. One phone warning names two of them and
      says that two others match, with the review advice appearing once rather
      than per student.
### Editing a student

1. Editing a name that AB3's prefixes made impossible to type

   1. Prerequisites: Run `add Ravi Kumaran -p 91234567`, then `list`, and note
      the new student's index.
   1. Test case: `edit INDEX -n "Ravi s/o Kumaran"`, then the same without the
      quotation marks.<br>
      Expected: The quoted form stores the name in full, the slash being an
      ordinary character where `n/John p/ Smith` once stored `Smith` as the
      phone. The unquoted form changes nothing and reports that `s/o` belongs
      to no option, since a name marked by an option takes one token.

1. Editing by NUS Student ID

   1. Prerequisites: Run `list` and note a student's NUS Student ID, such as
      Charlotte Oliveiro's `A02345678G` in the sample data.
   1. Test case: `edit A02345678G -p 81112222`, then the same with the ID in
      lowercase.<br>
      Expected: Both change the phone of the student holding that ID and name
      them in the reply, because matching an ID ignores case.
   1. Test case: `find -n Alex`, then `edit A02345678G -p 81112222` while the
      search hides Charlotte.<br>
      Expected: Her phone changes even though she is not displayed. An
      `edit 1 -p 81112222` in the same state would edit Alex instead, index 1
      being the first student the search shows.
   1. Test case: `edit A01234567X --id A03456789H`, where another student holds
      the second ID, then `edit A99999999Z -p 81112222` and
      `edit A0123-456 -p 81112222`.<br>
      Expected: None changes anything. The first refuses an ID another student
      holds; the second reads `No student has NUS Student ID A99999999Z.`; the
      third names the hyphen as the reason the target is not a valid ID.

1. Clearing the tags

   1. Prerequisites: Give the student two tags with `edit INDEX -t T1 -t "Lab 3"`.
   1. Test case: `edit INDEX -t ""`, then `edit INDEX -t` and
      `edit INDEX -t T1 -t ""`.<br>
      Expected: The first clears both tags and touches nothing else. The second
      names `-t TAG` as needing a value rather than clearing. The third is
      refused, because an empty value clears the tags only on its own.

1. Repeating an edit after a search

   1. Prerequisites: Add `add Alex Yeoh -p 87438807` and
      `add John Doe -p 98765432`, then run `find -n john`.
   1. Test case: Run `edit 1 --follow-up` twice.<br>
      Expected: Both flag John Doe, the only student listed. The search stays
      active, so index 1 keeps pointing at him and Alex stays unflagged.
   1. Test case: Add `add John Smith -p 91112222`, run `find -n john`, then
      `edit 1 -n Jack` followed by `edit 1 -p 91112233`.<br>
      Expected: Renaming John Doe to Jack takes him off the filtered list and
      John Smith shifts up to index 1, so the second command changes John
      Smith's phone rather than Jack's. The User Guide warns that an edit
      removing a match shifts the remaining indices.

1. Reporting several mistakes at once

   1. Test case: `edit 0 -p 12`, then `edit` and `edit INDEX`.<br>
      Expected: Nothing changes. The first reply opens
      `Index must be a positive integer.` and also reports the invalid phone,
      without the usage block. The second names the missing index and says a
      field is needed; the third asks only for a field.
### Clearing a student's optional details

1. Removing an optional detail without removing the record

   1. Prerequisites: Add `add Alice Tan -p 91234567 -e alice@example.com
      --id A1234567X -t T1 -t "Lab 1" --follow-up` as one line, and note her
      displayed `INDEX`.
   1. Test case: `edit INDEX -e ""`, then `edit INDEX -p ""`, then
      `edit INDEX -t "   "`.<br>
      Expected: Each detail disappears from the card and the feedback, the
      remaining ones closing up with no blank line, and every other field and
      the follow-up status stay as they were. The saved record loses the
      matching key, and the details stay absent after a restart. A
      whitespace-only value is trimmed and clears the field the same way.
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
   1. Test case: `delete A02345678G`, then the same ID in lowercase.<br>
      Expected: Charlotte is deleted wherever she appears and the reply
      identifies her; matching an ID ignores case. Running
      `find -n Alex` first and deleting her by ID still works while the search
      hides her.
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
      "2". delete accepts one index and nothing else.` No student is deleted.
   1. Test case: `delete 1` followed by a non-breaking space (`U+00A0`) pasted
      from a web page.<br>
      Expected: The first record is deleted exactly as with an ordinary space.
      TAB does not call the command malformed for a character the user cannot
      see.

1. More test cases to be added.
### The command line

1. Recalling commands and restoring a draft

   1. In a fresh session, type `draft` and press Up and Down.<br>
      Expected: The text stays `draft`, no command having been submitted.
   1. Submit `list`, then `find -n "Siti Nur-Aisyah"`, then type `add draft`
      without submitting it. Press Up twice, then once more, then Down
      twice.<br>
      Expected: Up recalls the find command, then `list`, then stays at `list`,
      each recalled line keeping its colors with the caret at the end. Down
      walks back and restores the unsent `add draft`.
   1. Submit `addd`, then run `clear` and press Enter on the empty reply.
      Press Up.<br>
      Expected: The rejected `addd` is recalled like any other line, and
      correcting or recalling it does not run it. Clear is canceled without
      changing any students and Up recalls `clear` with no blank entry between.
      Restarting TAB clears the history.

1. Pasting multi-line text

   1. Test case: Copy two adjacent spreadsheet cells holding `Wei Ming` and
      `Tan`. Type `add `, paste, type ` -p 91234567`, and press Enter.<br>
      Expected: The two stay separate words with matching colors, and one
      student named `Wei Ming Tan` is added. A trailing line break from the
      spreadsheet may leave an extra space before `-p` without changing the
      command.
   1. Test case: Paste text holding a soft line break rather than a newline —
      on Linux, `printf 'Siew Ling\vGoh' | xclip -selection clipboard` — into
      `add `, add ` -p 91234563`, and press Enter.<br>
      Expected: The box reads `add Siew Ling Goh -p 91234563` with the words
      apart, and the student is saved under that name. Pasting into a selection
      replaces it and leaves the caret after the inserted text.

1. Checking what the colors say

   1. Test case: Type `add "Siti Nur-Aisyah" -p 98765432 -t T1` without
      pressing Enter.<br>
      Expected: `add` is bold in the accent color, `"Siti Nur-Aisyah"` green,
      `-p` and `-t` mauve, and the remaining values ordinary text.
   1. Test case: Type `addd` and press Enter, then type another character.<br>
      Expected: The whole line turns red and the reply quotes `addd` and
      suggests `add`, adding nobody. The next character takes the red off.
   1. Test case: Type `add"Ravi s/o Kumaran" -p 91234567` with no space after
      `add`, then press Enter.<br>
      Expected: The bold command word ends at `add"Ravi`, with `s/o` an
      ordinary word after it, and the reply reports that unknown command —
      splitting the line where the colors already did.

1. Checking a command longer than the line

   1. Test case: Type a `find` command long enough to run past the right-hand
      edge, press Home then End, then Ctrl+A, then click in the middle.<br>
      Expected: The line scrolls so the caret stays in view at both ends rather
      than being cut off by the edge, a band is drawn behind the whole command
      with every part still readable on top of it, and the caret lands on the
      character clicked rather than one or two away. Narrowing the window and
      clicking again behaves the same.

1. Checking right-to-left and mixed-script commands

   1. Test case: Type `add محمد علي حسن -p 91234567`, then
      `add Ali محمد דוד Tan -p 91234567`, with `add José Tan -p 91234567` as a
      control. Click between letters of an RTL word, type a character, then
      select across the Latin and RTL words with Shift+Left and Shift+Right.<br>
      Expected: Each command stays on one readable line and scrolls
      horizontally when it outgrows the width. The caret marks the insertion
      point after a click, and only the selected characters are highlighted,
      even where the selection falls into separate visual segments.
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
      Expected: Each names the missing field option, unexpected value, missing
      value, unknown option, empty term or unclosed quote. The displayed list
      is unchanged after every refusal.
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
      and a bare value after an option that takes none. The displayed list is
      unchanged, and restarting TAB leaves every student and status intact.
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

1. Loading a correctly hand-edited data file

   1. Prerequisites: Add a recognizable student, close every TAB window using
      this data file, and back up `data/tab.json`.
   1. Test case: Change that student's phone to `81234567`, preserving valid
      JSON, save as UTF-8, and restart TAB.<br>
      Expected: The student shows the edited phone. Close TAB and restart
      again; the hand-edited value remains.

1. Loading a record whose optional details are absent

   1. Test case: With TAB closed, remove one student's `phone` key entirely
      and set another's to `null`, then restart.<br>
      Expected: Both records load with no number and no blank line on their
      cards. Run `edit 1 -p 91234567` for one of them and restart; that number
      is saved while the other record still has no `phone` key.

1. Reporting a saved value TAB cannot read

   1. Test case: With TAB closed, set a student's `phone` to `"12"` and
      restart.<br>
      Expected: Startup refuses the file with `Phone "12" is not valid: a phone
      number needs at least 3 digits`, naming the student record. The file is
      left unchanged, so the value is reported rather than dropped.
   1. Test case: Set a student's `tags` to `"friends"` rather than a list, and
      restart.<br>
      Expected: The error names the student record and `tags`, and asks for a
      JSON array of strings.

1. Reading repeated and reserved tags from a file

   1. Test case: Set one student's `tags` to `[" t1 ", "T1", "Lab  3", "lab 3"]`
      and restart.<br>
      Expected: Their card shows only `t1` and `Lab 3`, matching the summary.
      Edit that student and restart; the saved tags keep those first spellings
      with no duplicates.
   1. Test case: Add `"Needs follow-up"` to any record's `tags` and restart.<br>
      Expected: The main window does not open. TAB names the record and
      explains that the label marks a student for follow-up rather than naming
      a tag, pointing at `"flag"`. `"Needs follow-up soon"` is accepted, since
      only the label itself is reserved.

1. Refusing a stale second window's save

   1. Prerequisites: Open two TAB windows from the same disposable folder
      before making any changes.
   1. Test case: In window A, enter `add Alice Probe -p 81230001 --id A1111111X`.
      In window B, enter `add Bob Probe -p 81230002 --id A2222222X`.<br>
      Expected: A succeeds. B reports that the data file changed, asks you to
      close this window and restart, and says no changes were made. B keeps its
      previous records and filter, and A's saved file is untouched. Closing and
      restarting B shows Alice, after which adding Bob succeeds.

1. Preserving existing data when a save fails

   1. Prerequisites: Add a recognizable student, exit, and keep a copy of
      `data/tab.json`. Make the `data` folder non-writable: `chmod 555 data` on
      Linux or macOS, or remove write permission for the test account on
      Windows.
   1. Test case: Launch TAB and enter `add Bob Tan -p 91234567`.<br>
      Expected: TAB reports that it could not save to `data/tab.json` for want
      of permission. Bob does not appear, because the failed add is undone in
      memory, and the file still matches the copy.
   1. Test case: Enter `list`, `find -n Amy` and `help`, press q, then
      `exit`.<br>
      Expected: Each command works, including the help page, and the file is
      still unchanged. Restore write permission before continuing.

1. Dealing with a missing or corrupted data file

   1. Prerequisites: Keep a copy of a valid multi-student `data/tab.json`.
   1. Test case: Replace the contents with `not json format!` and launch
      TAB.<br>
      Expected: An error dialog names `data/tab.json`, reports malformed JSON
      with its line and column, shows the affected line numbered, and asks the
      User to repair or replace it. The main window does not open and the file
      is left as it is.
   1. Test case: Restore the copy, remove the comma after one student's
      `"name"` value, and launch TAB.<br>
      Expected: The dialog marks the next line as affected and shows at most
      one numbered line on either side, without listing unrelated records.
   1. Test case: Replace the contents with `{}` and launch TAB.<br>
      Expected: The dialog reports that the student list is missing.
   1. Test case: Move `data/tab.json` elsewhere and relaunch.<br>
      Expected: TAB treats the missing file as a first launch and opens with
      sample students rather than an error.
   1. Test case: Restore the file and make the folder unreadable with
      `chmod 000 data`, then relaunch.<br>
      Expected: The dialog says TAB does not have permission to read the file
      or its folder, and no sample students are shown. Restore with
      `chmod 755 data` and confirm every student returns.

1. Detecting misspelled data-file keys

   1. Prerequisites: Back up a valid multi-student `data/tab.json`.
   1. Test case: Change the second student's `nusStudentId` to `nusStudentID`,
      keeping the value, and launch TAB.<br>
      Expected: The startup error names the file, the unknown key, student
      record 2 and that student's name, and lists the supported fields. The
      main window does not open and no value is erased.
   1. Test case: Correct the spelling, relaunch, change a student through TAB,
      and relaunch again.<br>
      Expected: Every supported value survives. A legacy `address` key is
      ignored rather than refused, so files written before it was removed still
      load.

1. Loading files saved with different encodings

   1. Prerequisites: Exit TAB and back up a `data/tab.json` holding a student
      named `José Tan`.
   1. Test case: Save the file as Windows-1252 (ANSI) and launch TAB.<br>
      Expected: The dialog says the file is not valid UTF-8 and explains how to
      resave it. The main window never opens and the file is unchanged.
   1. Test case: Reopen it as Windows-1252, save as UTF-8, and relaunch.<br>
      Expected: Both students load with the accent intact. Saving as UTF-8 with
      a byte order mark also loads.

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
