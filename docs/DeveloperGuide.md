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

* This project is based on the AddressBook-Level3 project created by the
  [SE-EDU initiative](https://se-education.org).
* `StringUtil.editDistance` implements the published
  [optimal string alignment distance](https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance),
  a known algorithm whose recurrence we wrote out ourselves. No code was
  copied, and the idea is reused in that one method.

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

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-T16-1/tp/blob/master/src/main/java/seedu/tab/ui/MainWindow.java).

The `UI` component,

* executes commands entered by the User using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Student` objects from the model.

The student card's secondary row reserves one pixel at its right edge because
JavaFX 17's `HBox` can leave one pixel undistributed when shrinking children.
This keeps the email inside the details area at narrow widths, including when
platform fonts produce different preferred label widths.

The student card's `TagRow` retains every tag name, but creates at most three
tag labels. Layout uses the full name count to size the overflow chip and
list all hidden tags in its tooltip, even when fewer labels are created.

Student-card email labels use `UiText.showToFit` to retain their full text
and show a tooltip only while the available width cuts the address. Width
listeners remove the tooltip when widening the card makes it unnecessary.

`HelpWindow` re-fits its scene after the first rendering pulse, allowing
for native frame sizes, then uses `sizeToScene()` without fixed FXML scene
dimensions. This lets JavaFX include native decoration sizes correctly.
Its copy button keeps the same border insets when focused, so the focus
indicator does not increase the content's preferred size. The regression
test waits for rendering and native resize events, then measures the scene
rather than its surrounding frame.

#### Fitting names and tags on a card

`CardHeadline` reserves a readable start of the name before allocating room
to tags. When that leaves no room for a tag chip, `TagRow` uses the shorter
`N tags` total instead of `N more…`, preserving a complete count beside the
name. Its tooltip still lists every hidden tag.

#### Colouring the command line

`CommandTextField` converts tab and line separators to spaces in text
replacement before JavaFX filters control characters. It preserves pasted
word boundaries while keeping the command on one line, including when a
paste replaces selected text. CRLF is treated as one separator.

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
  so it sits where the text does.
* **the selection.** The field draws its highlight at its own offset, which
  broke into fragments behind the copy, so it is turned off in the stylesheet
  and drawn here instead.

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
whole student book: after `find Alex` it counts the matches. That is the
reading the User Guide documents, and it keeps the panel answering the same
question as the list it sits beside. Summarising the whole book instead would
need `Logic` to expose it.

The list is an `ObservableList`, so a listener keeps the counts current
without the panel being told when a command changes anything. The counting
itself is static and takes a plain `List<Student>`, so it is tested without a
toolkit; only the rendering needs one.

Tags counted the same are listed alphabetically ignoring case, because a
reader does not read `Zoo` as coming before `apple`.

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

### Common classes

Classes used by multiple components are in the `seedu.tab.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

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
Value-taking options take the token after them, and `-t` or `--tag` repeats.
The presence-only `--follow-up` option (alias `-f`) and edit-only
`--no-follow-up` option (alias `-nf`) instead record an empty value and consume
no following token. This lets
`FlagArgumentMap` distinguish an
absent option from a present marker without a separate representation. An
unknown option, a value-taking option left without a value, and a token
belonging to no option are each reported for what they are, rather than as one
generic complaint.

`Flag` also lets a value-taking option carry aliases. NUS Student ID uses
`--id` as its canonical option and the unused `-i` as its typing-friendly
alias. Both spellings map to the same `FLAG_NUS_STUDENT_ID`, so giving
both is rejected as a repeated single-valued field rather than letting one
silently override the other. Usage labels and error messages use the canonical
`--id NUS_STUDENT_ID` spelling.

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

### Commands with no arguments

`StudentBookParser` rejects non-whitespace input after `clear`, `list`,
`help`, and `exit` before returning a command. Its existing Unicode
whitespace rule accepts pasted whitespace as well as spaces and tabs.
The feedback names the command and unexpected input, with long input
shortened to keep the reason visible. Quotes do not make an argument absent:
`clear ""` is refused too. A parse failure reaches neither command execution
nor `LogicManager`'s storage write, so it preserves both the displayed list
and the JSON file.

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

The comparison folds case, so `LIST` is read as an attempt at `list`, while
the message quotes the word as it was typed, because a User who typed capitals
needs to see them to recognise what they did. The quoted word is shortened the
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
change their identity. The four-argument constructor defaults to no flag so
existing callers remain source-compatible.

Search combines ordinary tags with the optional flag and matches their
inherited `tagName` values. The student card and command feedback display the
same value, while the card keeps the flag in its separate follow-up row.

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

`StudentCard` shows a `Needs follow-up` label for true. For false it makes the
label both invisible and unmanaged, so the card keeps no blank placeholder
row. Successful add, edit and delete feedback formats student records
consistently with the student card: optional fields (email, tags, follow-up
status) appear only when present, with tags ordered alphabetically ignoring
case, avoiding placeholder labels or empty tag markers.

### Field values

`Name`, `Tag` and `Phone` each store the form returned by
`StringUtil.normalizeFieldValue`: without zero-width characters, with each run of
whitespace reduced to one ASCII space and leading and trailing whitespace removed,
and with the result composed to Unicode NFC. Composing happens last, because a zero-width
character between a base letter and its combining mark blocks the two from
composing.

They normalize for different reasons, and the rule each one enforces differs.

| Field | Why it is normalized | What it must hold |
| --- | --- | --- |
| `Name` | it is the identity field used to compare students, so visually identical names should have the same stored form | at least one letter or number |
| `Tag` | it is a key in the set of a student's tags, so two that look alike must not both be stored | at least one letter or number |
| `Phone` | it is displayed beside the others and gains nothing from being stored differently | at least 3 digits |

**The NUS Student ID may be absent.** `NusStudentId` accepts between 1 and 32
ASCII letters and digits. Input letters are case-insensitive and the value is
stored in uppercase, so `e0123456` and `E0123456` are the same value. This
accommodates variations in the identifiers issued to different groups of
students while preventing an accidentally pasted value from hiding the other
details on a student's card. `Student` holds a
nullable `NusStudentId` and exposes it as `Optional<NusStudentId>`, matching
the existing optional-email boundary without inventing a sentinel value.
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

NUS Student ID is searchable but is **not an identity key**. Student identity remains
the normalized name, so two different names may carry the same NUS Student ID, just as
they may carry the same phone or email. This avoids rejecting legitimate data
or claiming certainty when a reused detail might be a correction or a data-entry
mistake. Adding or editing a record to a name already in the book is still
rejected. A future duplicate-warning workflow can surface shared contact
details without changing this model contract.

**The email may be absent.** A student is recorded before an email address is
known rather than one being invented. `Student` retains its nullable email
constructor contract and exposes the value as `Optional<Email>` so callers
handle absence explicitly. `JsonAdaptedStudent` writes the email as a string
when present; the optional wrapper is not written to JSON.

The data file simply carries no `email` key. One that holds a malformed address
is still refused, rather than the address being quietly dropped.

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
keeps the value optional until the existing
`Student` constructor boundary. The saved record omits the `email` key and
reloads through the existing optional-email storage contract.

`Email.MAX_LENGTH` caps at 254 characters the address a **command** may enter.
`ParserUtil.parseEmail` checks `Email.isWithinLengthLimit` before
`Email.isValidEmail`, and `getFailureReason` carries the same guard because it
matches the local part on its own.

The limit is not there to simplify validation, which the guidance on arbitrary
limits rules out. `VALIDATION_REGEX` nests one quantifier inside another,
`([+_.-][^\W_]+)*`, and Java matches a repeated group by recursion, one frame
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
that a number typed with unusual whitespace is stored consistently.

`FindCommandParser` reads search arguments using `CommandTokenizer`, allowing
keywords to be enclosed in double quotes to match phrases containing spaces
(such as `"Lab 3"`). Unterminated quotes report `CommandTokenizer.MESSAGE_UNCLOSED_QUOTE`,
while empty or whitespace-only quoted terms are rejected with `MESSAGE_EMPTY_KEYWORD`.
Search terms are normalized with `StringUtil.normalizeFieldValue`, collapsing internal
whitespace runs and applying Unicode NFC normalization. It creates a
`DetailsContainsKeywordsPredicate`, which checks each keyword or phrase
against the student's name, NUS Student ID, phone, email, ordinary tags,
and optional follow-up flag. Matching is case-insensitive and partial; any
keyword can match.

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

`JsonUtil` turns malformed JSON or an incompatible JSON shape into a
`DataLoadingException` that retains the parser's line and column. During model
conversion, `JsonAdaptedStudent` identifies the invalid field and
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

A name is displayed, searched as text, and used as a student's
identity field. `Name` therefore stores a normalized field value rather than
the raw input, so that two names which look identical cannot be searched
differently or admitted as two students.

**Normalizing.** On construction a name is

* composed to Unicode form NFC, so that `Nguyễn` written as one code point and
  as `e` plus two combining marks become the same string
* stripped of the zero-width space and the byte order mark, which are invisible
  and carry no meaning of their own
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
and the follow-up pill, so a name longer than `StudentCard.MAX_NAME_CHARACTERS`
is cut and the whole of it is left on the card as a tooltip. The count is fixed
rather than taken from the room the row has left over, so the same name breaks
off in the same place on every card instead of moving as the rest of the card
changes. A name within the count is cut all the same where the row runs out of
room, so the name goes on the card through `UiText.showMiddleShortenedToFit`,
which hands the label the whole name on hover whenever it is drawn narrower
than its text needs. The tags take that path for the same reason.

The cut is taken from the middle, keeping 13 characters of the start and 9 of
the end. A cut at the close alone puts `Mohamed Abdul Rahman bin Ismail` and
`Mohamed Abdul Rahman bin Ahmad` on two cards as the same text, and a reader
scanning the list has nothing to tell the rows apart by; the end of a name is
where two such students differ, which is what the 9 characters buy. Wrapping
the name to a second line was weighed and dropped, because it lets any card
grow to two lines tall and so costs the list more rows than the collision costs
a reader, and the pill and the tags still have to fit beside the name. A name
within one character of the count is left whole, since the ellipsis takes a
place of its own and the cut would save no room. `UiText` counts both ends with
`StringUtil.firstCharacters` and `StringUtil.lastCharacters`, so neither cut
separates a letter from the accent that belongs to it nor an emoji from its
other half.

Two cuts reach the name and they have to agree on where they take text from.
The count is the card's own and is applied before the card is laid out, so it
depends on the name alone. The second is taken once the line knows how much
room it has, and so depends on what the card carries beside the name.
`CardName` takes that second cut itself, from the middle and through the same
`StringUtil` counting, rather than leaving it to the label. A cut at the end
would read as the same text on two cards: at the window's smallest width both
`Mohamed Abdul Rahman bin Ismail` and `Mohamed Abdul Rahman bin Ahmad` drew as
`Mohamed Abd...`. A label told to elide from the middle instead cuts at a
character index, which falls between the two halves of a character Java stores
in two, so `Alexander 𠮷田` drew as `Ale…` with the back half of its fourth
character after it.

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
next input as a reply to the concise `[y/N]` prompt. Only surrounding Unicode
whitespace is trimmed; internal characters are kept so a value cannot become
an accepted word by removing characters from within it. Replies ignore case.
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

### \[Proposed\] Undo/redo feature

#### Proposed Implementation

The proposed undo/redo mechanism is facilitated by `VersionedStudentBook`. It extends `StudentBook` with an undo/redo history, stored internally as a `studentBookStateList` and `currentStatePointer`. Additionally, it implements the following operations:

* `VersionedStudentBook#commit()` -- Saves the current state of the student book in its history.
* `VersionedStudentBook#undo()` -- Restores the previous state of the student book from its history.
* `VersionedStudentBook#redo()` -- Restores a previously undone state of the student book from its history.

These operations would be exposed in the `Model` interface as `Model#commitStudentBook()`, `Model#undoStudentBook()` and `Model#redoStudentBook()` respectively.

Given below is an example usage scenario and how the undo/redo mechanism behaves at each step.

Step 1. The User launches TAB for the first time. The `VersionedStudentBook` will be initialized with the initial state of the student book, and the `currentStatePointer` pointing to that single state.

<puml src="diagrams/UndoRedoState0.puml" alt="UndoRedoState0" />

Step 2. The User executes `delete 5` command to delete the 5th student in the student book. The `delete` command calls `Model#commitStudentBook()`, causing the modified state of the student book after the `delete 5` command executes to be saved in the `studentBookStateList`, and the `currentStatePointer` is shifted to the newly inserted state.

<puml src="diagrams/UndoRedoState1.puml" alt="UndoRedoState1" />

Step 3. The User executes `add David …​` to add a new student. The `add` command also calls `Model#commitStudentBook()`, causing another modified state of the student book to be saved into the `studentBookStateList`.

<puml src="diagrams/UndoRedoState2.puml" alt="UndoRedoState2" />

<box type="info" seamless>

**Note:** If a command fails its execution, it will not call `Model#commitStudentBook()`, so the state of the student book will not be saved into the `studentBookStateList`.
</box>

Step 4. The User now decides that adding the student was a mistake, and decides to undo that action by executing the `undo` command. The `undo` command will call `Model#undoStudentBook()`, which will shift the `currentStatePointer` once to the left, pointing it to the previous state, and restores the student book to that state.

<puml src="diagrams/UndoRedoState3.puml" alt="UndoRedoState3" />


<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index 0, pointing to the initial `StudentBook` state, then there are no previous `StudentBook` states to restore. The `undo` command uses `Model#canUndoStudentBook()` to check if this is the case. If so, it will return an error to the User rather
than attempting to perform the undo.
</box>

The following sequence diagram shows how an undo operation goes through the `Logic` component:

<puml src="diagrams/UndoSequenceDiagram-Logic.puml" alt="UndoSequenceDiagram-Logic" />

<box type="info" seamless>

**Note:** The lifeline for `UndoCommand` should end at the destroy marker (X), but due to a limitation of PlantUML, it continues to the end of the diagram.
</box>

Similarly, how an undo operation goes through the `Model` component is shown below:

<puml src="diagrams/UndoSequenceDiagram-Model.puml" alt="UndoSequenceDiagram-Model" />

The `redo` command does the opposite — it calls `Model#redoStudentBook()`, which shifts the `currentStatePointer` once to the right, pointing to the previously undone state, and restores the student book to that state.

<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index `studentBookStateList.size() - 1`, pointing to the latest `StudentBook` state, then there are no undone `StudentBook` states to restore. The `redo` command uses `Model#canRedoStudentBook()` to check if this is the case. If so, it will return an error to the User rather than attempting to perform the redo.
</box>

Step 5. The User then decides to execute the command `list`. Commands that do not modify the student book, such as `list`, will usually not call `Model#commitStudentBook()`, `Model#undoStudentBook()` or `Model#redoStudentBook()`. Thus, the `studentBookStateList` remains unchanged.

<puml src="diagrams/UndoRedoState4.puml" alt="UndoRedoState4" />

Step 6. The User executes `clear`, which calls `Model#commitStudentBook()`. Since the `currentStatePointer` is not pointing at the end of the `studentBookStateList`, all states of the student book after the `currentStatePointer` will be purged. Reason: It no longer makes sense to redo the `add David …` command. This is the behavior that most modern desktop applications follow.

<puml src="diagrams/UndoRedoState5.puml" alt="UndoRedoState5" />

The following activity diagram summarizes what happens when the User executes a new command:

<puml src="diagrams/CommitActivityDiagram.puml" width="250" />

#### Design considerations:

**Aspect: How undo & redo execute:**

* **Alternative 1 (current choice):** Saves the entire student book.
  * Pros: Easy to implement.
  * Cons: May have performance issues in terms of memory usage.

* **Alternative 2:** Individual command knows how to undo/redo by
  itself.
  * Pros: Will use less memory (e.g. for `delete`, just save the student being deleted).
  * Cons: We must ensure that the implementation of each individual command is correct.

_{more aspects and alternatives to be added}_

### \[Proposed\] Data archiving

_{Explain here how the data archiving feature will be implemented}_


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
by tag, and see who is still awaiting a reply. Fully offline, in a plain-text
file locally, with every change undoable.


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
| `* * *` | teaching assistant | find students by part of any detail I remember, whatever the letter case | locate a student from a fragment, such as part of a name or a few digits of a phone number |
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
| `*` | teaching assistant | record two students who genuinely have the same name | keep separate student records for them |
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
* [UC04 - Import a roster](#uc04-import-a-roster)
* [UC05 - Recover from an unreadable data file](#uc05-recover-from-an-unreadable-data-file)
* [UC06 - Delete a class no longer taught](#uc06-delete-a-class-no-longer-taught)
* [UC07 - Archive last semester's students](#uc07-archive-last-semester-s-students)
* [UC08 - Semester close-out](#uc08-semester-close-out)
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

* 1c. The normalized name matches a student whom TAB already holds.

    * 1c1. TAB reports that the student already exists and leaves the student
      records unchanged.

      Use case ends.

* 1d. Before submitting the command, User chooses to abandon the addition.

    Use case ends.

* 2a. TAB cannot write to the data file.

    * 2a1. TAB reports that the student was not saved, and why.

      Use case ends.

#### UC02 - Find a student and act on the right one

**MSS**

1.  User requests to find students, providing one or more search terms.
2.  TAB shows the students for whom any term matches any part of a name, NUS Student ID,
    phone, email, tag, or follow-up status, regardless of letter case.
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

#### UC04 - Import a roster

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
      reports why it is unreadable, and leaves it unchanged.

      Use case resumes at step 3.

* 6a. One or more student records are still unreadable.

    * 6a1. TAB reports the first unreadable student record and reason, leaves
      the data file unchanged, and closes after User dismisses the error.
    * 6a2. User repairs the data file and starts TAB again.

      Steps 6a1-6a2 are repeated until all student records are readable.

      Use case resumes at step 6.

#### UC06 - Delete a class no longer taught

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

#### UC07 - Archive last semester's students

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

#### UC08 - Semester close-out

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

* 3d. The edited normalized name matches another student whom TAB already holds.

    * 3d1. TAB reports that the student already exists and leaves both student
      records unchanged.

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

*{More to be added}*


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
* **Identity field**: A field used by TAB to identify whether two records refer to the same student. Student identity
  is determined by normalized name. NUS Student IDs, phone numbers, and emails are searchable details, not identity keys.
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
* **Semester close-out**: The end-of-semester workflow (UC08) where active student records are archived
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

1. Help window sizing

   1. Run `help` and wait for the window to open.<br>
      Expected: The whole User Guide address and `Copy URL` button are visible.
      Check Windows, Linux and macOS because frame sizes and focus styling differ.

   1. Close the help window and run `help` again.<br>
      Expected: Both remain fully visible after reopening.

1. Saving window preferences

   1. Resize the window to an optimal size. Move the window to a different location. Close the window.

   1. Relaunch TAB by double-clicking the JAR file.<br>
       Expected: The most recent window size and location are retained.

1. Narrow student cards

   1. Add a student with
      `add Narrow Card -p "+65 9123 4567" -e a.very.long.address.indeed@u.nus.edu --id AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA`.

   1. Resize the window to its minimum width, then widen it slowly.<br>
      Expected: The ID and email shorten as needed, the phone remains readable,
      and the email stays inside the card's details area. Check on Windows as
      well as Linux and macOS, since font widths differ.

### Rejecting unexpected command arguments

1. Commands that take no parameters

   1. Prerequisites: Add a student, run `find no-such-student` to hide the
      student, and keep a copy of `data/tab.json`.

   1. Test cases: `clear oops`, `list oops`, `help oops`, and `exit oops`.<br>
      Expected: Each command reports that it takes no arguments and quotes
      `oops`. The list stays filtered, no help window opens, TAB stays open,
      and the JSON file matches the copy byte for byte.

   1. Test cases: `clear "oops"`, `clear ""`, and `clear "unfinished`.<br>
      Expected: Each reports the unexpected input. The list and JSON file
      stay unchanged, including for an empty quoted argument.

   1. Test cases: `list` and `help` with only trailing spaces or tabs.<br>
      Expected: The list shows all students and help opens respectively.
      `exit` with trailing whitespace closes TAB. Run plain `clear` with
      trailing whitespace only when the test data can be removed; it follows
      the documented clear flow.

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
      None of them runs: the list is unchanged, no help window opens, and no
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
      No help window opens until `help` itself is run.

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

1. Finding students by follow-up status

   1. Prerequisites: Continue from the preceding test with Alice unflagged
      and Bob flagged.

   1. Test cases: `find follow-up` and `find NEEDS`<br>
      Expected: Each search shows Bob and excludes Alice, matching part of
      the flag's displayed name without regard to case.

1. Setting and clearing follow-up while editing

   1. Prerequisites: Run `list` after the preceding search, restoring Alice
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
      Expected: Neither command adds Cara. The first reports a repeated `--follow-up`;
      the second reports that `true` belongs to no option.

   1. Test cases: `edit 1 --follow-up -f` and `edit 1 --follow-up true`<br>
      Expected: Neither command changes Alice. The first reports a repeated
      `--follow-up`; the second reports that `true` belongs to no option.

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

1. Adding a duplicate student

   1. Prerequisites: A student named `John Doe` with phone `98765432` is already in the list.

   1. Test case: `add John Doe -p 98765432`<br>
      Expected: No student is added. The status message reports that this student
      already exists in the student book, and no record is mutated.

### Long names on a card

1. Telling apart two names that share a start

   1. Prerequisites: Launch the JAR and widen the window until a card has room
      for a long name.

   1. Test case: `add Mohamed Abdul Rahman bin Ismail -p 91234567`, then
      `add Mohamed Abdul Rahman bin Ahmad -p 92345678`.<br>
      Expected: The two cards read `Mohamed Abdul…in Ismail` and
      `Mohamed Abdul…bin Ahmad`, so the names are distinguishable without
      reading the index. Hovering over either name shows the whole of it.

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
      Expected: Each tooltip shows the corresponding complete name, even though
      neither name exceeds the card's character limit.
   1. Widen the window until both names fit.<br>
      Expected: Each name appears in full and its tooltip is removed.

1. Leaving a name that nearly fits alone

   1. Test case: `add Jonathan Chuah Wei Ming -p 93456789`.<br>
      Expected: The card shows the whole name with no ellipsis, because a cut
      this close to the limit would save no room.

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

   1. Test cases: `find a0123` and `find 23456`.<br>
      Expected: Each search shows Alice, demonstrating partial,
      case-insensitive NUS Student ID matching. The ID remains after the restart.

   1. Exit TAB. Back up `data/tab.json`, then give otherwise valid records a
      `"nusStudentId": null` value and no `nusStudentId` key respectively. Relaunch TAB.<br>
      Expected: Both records load without an NUS Student ID.

   1. Exit TAB and change one `nusStudentId` value to `A0123-456X`, then relaunch.<br>
      Expected: TAB rejects the malformed data file and starts with an empty
      student book, leaving the invalid file in place. Restore the backup
      before entering another command.

1. Checking duplicate identity behavior

   1. Prerequisites: Start from an empty book.

   1. Test cases: add `add Alice Tan -p 91234567 -i A01234567X`, then
      `add Bob Lim -p 92345678 -i A01234567X`.<br>
      Expected: Both students are added because different normalized names are
      different identities, even when the NUS Student ID is shared.

   1. Test case: `add Alice Tan -p 93456789 -i A76543210Y`<br>
      Expected: The addition is rejected as a duplicate name and neither
      existing record changes.

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

1. Reporting several mistakes at once

   1. Prerequisites: Run `list` and note the list size.

   1. Test case: `edit 0 -p 12`<br>
      Expected: No change. One message names both the index and the phone
      number, rather than only the first fault found.

   1. Test cases: `edit` and `edit INDEX`<br>
      Expected: No change. The first names the missing index and says a field
      to edit is needed; the second asks only for a field.

1. _{ more test cases … }_

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

   1. Test case: `delete 1`<br>
      Expected: The first student record is deleted from the list. The status message identifies
      the removed student and shows their details (`Deleted student: NAME; Phone: PHONE...`).

   1. Test case: `delete 0`<br>
      Expected: No student is deleted. The status message shows error details, and the student book
      remains unchanged.

   1. Other incorrect delete commands to try: `delete`, `delete x`, `...` (where x is larger than the list size)<br>
      Expected: Similar to previous.

1. _{ more test cases … }_

### The command line

1. Recalling commands and restoring a draft

   1. In a fresh session, type `draft` and press Up and Down.<br>
      Expected: The text remains `draft` because no command has been submitted.
   1. Replace it with `list` and press Enter. Run `find "Siti Nur-Aisyah"`,
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

### The summary beside the list

1. Checking that the summary follows the list

   1. Prerequisites: several students, at least one flagged and several
      sharing a tag.
   1. Test case: `list`<br>
      Expected: The panel's total matches the number of cards. Each tag in use
      is listed with its count, most used first, and tags with equal counts
      are in alphabetical order whatever case they were typed in.
   1. Test case: `find Alex`<br>
      Expected: The counts fall to cover only the students now listed. The
      total reads the number of matches.
   1. Note: the sample data ships no flagged student, so flag one with
      `edit 1 -f` before checking the flagged count.
   1. Test case: `clear`, then `yes`.<br>
      Expected: The flagged count and the total read zero, and the tag list is
      empty rather than the panel disappearing.

### Student counts in list and find feedback

1. Accurate student counts in command feedback

   1. Prerequisites: An empty student book. Run `clear` and confirm with `yes`
      if any students remain.

   1. Test case: `list`<br>
      Expected: Feedback reports `Listed 0 students.` and the displayed list is empty.

   1. Test case: `find Alex`<br>
      Expected: Feedback reports `Found 0 students.` and no students are displayed.

   1. Test case: `add "Amy Bee" -p 85355255 -e amy@example.com`, then `list`<br>
      Expected: Feedback reports `Listed 1 student.` with proper singular wording.

   1. Test case: `find Amy`<br>
      Expected: Feedback reports `Found 1 student.`.

   1. Test case: `add "Bob Choo" -p 88888888 -e bob@example.com`, then `list`<br>
      Expected: Feedback reports `Listed 2 students.` with plural wording and accurate count.

   1. Test case: `find Amy Bob`<br>
      Expected: Feedback reports `Found 2 students.`.

   1. Test case: `delete 1`, then `list`<br>
      Expected: Feedback reports `Listed 1 student.`.

### Locating students with quoted phrases and error handling

1. Quoted phrases, mixed terms, and unclosed quotes

   1. Prerequisites: Clear all students (`clear`, then `yes`). Add two students:
      `add "Alice Tan" -p 91111111 -t "Lab 3"` and
      `add "Bob Lee" -p 93333333 -t Lab`.

   1. Test case: `find "Lab 3"`<br>
      Expected: Matches only `Alice Tan` (`Found 1 student.`). `Bob Lee` is excluded because
      the phrase "Lab 3" does not match separate "Lab" and "3" fragments.

   1. Test case: `find Bob "Lab 3"`<br>
      Expected: Matches both `Alice Tan` and `Bob Lee` (`Found 2 students.`), showing OR matching
      across words and quoted phrases.

   1. Test case: `find "Lab 3`<br>
      Expected: Error reported: `A quoted value is never closed. Add the closing " or remove the opening one.`
      The displayed student list remains unchanged.

   1. Test case: `find "Lab  3"` (two spaces inside the quotes)<br>
      Expected: Matches `Alice Tan` (`Found 1 student.`), because search terms are normalized
      like stored field values, collapsing interior whitespace runs.

   1. Test case: `find ""` or `find "   "`<br>
      Expected: Error reported: `A search term is empty. Remove the empty quotes or put a value between them.`
      The displayed student list remains unchanged.

   1. Test case: `find Alice ""` or `find Alice "   "`<br>
      Expected: Error reported: `A search term is empty. Remove the empty quotes or put a value between them.`
      The displayed student list remains unchanged.

   1. Test case: `find` or `find    ` (with no search terms)<br>
      Expected: Error reported: `Invalid command format! ...` followed by find usage instructions.
      The displayed student list remains unchanged.

### Confirming before clearing all students

1. Confirmation, cancellation, and keyboard use

   1. Prerequisites: Add at least two students, run `find no-such-student`
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

   1. Test case: Enter `clear` again.<br>
      Expected: TAB reports that the book is already empty. No confirmation
      is pending, so `help` works immediately.

   1. Test case: Add a student, enter `clear`, close TAB before replying,
      and restart it.<br>
      Expected: The student is still present and no confirmation is pending.

### Saving data

1. Dealing with missing/corrupted data files

   1. Prerequisites: Launch TAB, add a recognizable student, exit, and keep a
      byte-for-byte copy of `data/tab.json`.

   1. Test case: Replace the file contents with `not json format!` and launch
      TAB.<br>
      Expected: An error dialog identifies `data/tab.json`, reports malformed
      JSON with its line and column, and asks the User to repair or replace it.
      The main window does not open. After dismissing the dialog, the file still
      matches the malformed contents byte for byte.

   1. Test case: Replace the file contents with `{}` and launch TAB.<br>
      Expected: The dialog reports that the student list is missing. The main
      window does not open and the file remains byte-for-byte unchanged.

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

   1. Test case: Move `data/tab.json` elsewhere and relaunch TAB.<br>
      Expected: TAB treats the missing file as a first launch and opens with
      sample students instead of showing an error.

   1. Test case: Restore a valid file written before the optional `flag` field
      existed, including its removed `address` keys, and relaunch TAB.<br>
      Expected: TAB loads every student, ignores the old address values and
      treats a missing flag as not needing follow-up.

1. _{ more test cases … }_
