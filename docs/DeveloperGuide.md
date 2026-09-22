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

--------------------------------------------------------------------------------------------------------------------

## **Setting up, getting started**

Refer to the guide [_Setting up and getting started_](SettingUp.md).

--------------------------------------------------------------------------------------------------------------------

## **Design**

### Architecture

<puml src="diagrams/ArchitectureDiagram.puml" width="280" />

The ***Architecture Diagram*** given above explains the high-level design of the App.

The following provides a quick overview of the main components and their interactions.

**Main components of the architecture**

**`Main`** (consisting of classes [`Main`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/Main.java) and [`MainApp`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/MainApp.java)) is in charge of the app launch and shut down.
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

<puml src="diagrams/ArchitectureSequenceDiagram.puml" width="574" />

Each of the four main components (also shown in the diagram above),

* defines its *API* in an `interface` with the same name as the Component.
* provides its functionality through a concrete `{Component Name}Manager` class that implements the corresponding API interface.

For example, the `Logic` component defines its API in `Logic.java` and implements it in `LogicManager.java`. Other components interact with a component through its interface rather than its concrete class, preventing them from coupling to that component's implementation, as illustrated in the following partial class diagram.

<puml src="diagrams/ComponentManagers.puml" width="300" />

The sections below give more details of each component.

### UI component

The **API** of this component is specified in [`Ui.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/ui/Ui.java)

<puml src="diagrams/UiClassDiagram.puml" alt="Structure of the UI Component"/>

The UI consists of a `MainWindow` and its parts, such as `CommandBox`, `ResultDisplay`, `PersonListPanel`, and `StatusBarFooter`. All of these, including `MainWindow`, inherit from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Person` objects from the model.

### Logic component

**API** : [`Logic.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<puml src="diagrams/LogicClassDiagram.puml" width="550"/>

The sequence diagram below illustrates the interactions within the `Logic` component, taking `execute("delete 1")` API call as an example.

<puml src="diagrams/DeleteSequenceDiagram.puml" alt="Interactions Inside the Logic Component for the `delete 1` Command" />

<box type="info" seamless>

**Note:** The lifeline for `DeleteCommandParser` should end at the destroy marker (X), but due to a limitation of PlantUML, the lifeline continues till the end of diagram.
</box>


How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to an `AddressBookParser` object, which in turn creates a parser that matches the command (e.g., `DeleteCommandParser`) and uses it to parse the command.
1. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `DeleteCommand`) which is executed by the `LogicManager`.
1. The command can communicate with the `Model` when it is executed (e.g. to delete a person).<br>
   Note that although this is shown as a single step in the diagram above for simplicity, the code can require several interactions between the command object and the `Model` to complete the operation.
1. The result of the command execution is encapsulated as a `CommandResult` object which is returned from `Logic`.

Here are the other classes in `Logic` (omitted from the class diagram above) that are used for parsing a user command:

<puml src="diagrams/ParserClasses.puml" width="600"/>

How the parsing works:
* When called upon to parse a user command, the `AddressBookParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddCommandParser`). The parser uses the other classes shown above to parse the user command and create an `XYZCommand` object (e.g., `AddCommand`). The `AddressBookParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddCommandParser` and `DeleteCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.

### Model component
**API** : [`Model.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/model/Model.java)

<puml src="diagrams/ModelClassDiagram.puml" width="450" />


The `Model` component,

* stores the address book data i.e., all `Person` objects (which are contained in a `UniquePersonList` object).
* stores the `Person` objects selected by the current filter, such as search results, in a separate _filtered_ list. It exposes this list as an unmodifiable `ObservableList<Person>` that the UI can observe and bind to, so the UI updates when the list changes.
* stores a `UserPrefs` object that represents the user’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)


<box type="info" seamless>

**Note:** The alternative, arguably more object-oriented, design below keeps a unique list of tags in `AddressBook`, and each `Person` references tags from that list. This lets `AddressBook` maintain one `Tag` object per unique tag instead of each `Person` holding its own `Tag` objects.<br>

<puml src="diagrams/BetterModelClassDiagram.puml" width="450" />
</box>


### Storage component

**API** : [`Storage.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/storage/Storage.java)

<puml src="diagrams/StorageClassDiagram.puml" width="550" />

The `Storage` component,
* can save both address book data and user preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonAddressBookStorage` and `JsonUserPrefsStorage` (one class per data file).
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`)

### Common classes

Classes used by multiple components are in the `seedu.address.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### How `add` reads its arguments

`add` marks its fields with options, `NAME -p PHONE [-e EMAIL] [-t TAG]...`,
where the other commands mark theirs with prefixes such as `p/`.

A prefix has to be a sequence a value never contains, and no such sequence
exists for a name. `s/o`, `d/o`, `a/l` and `a/p` are ordinary parts of
Singaporean and Malaysian names, and `a/l` was in fact unreadable for as long
as an address field was marked `a/`. Widening the prefixes would only move the
collision somewhere else. Options move the problem out of the value entirely:
an option is a whole token, so a name is read as a name however it is spelled.

Parsing runs in two passes, the way a shell does it.

`CommandTokenizer` splits the arguments on whitespace and lets double quotes
group a value holding spaces. A token records whether a quote took part in it,
because that is the only thing separating the value `-Ahmad` from the option
`-e`. A backslash escapes a quote or another backslash, so a value may hold
either; a backslash before anything else stays in the value, so nothing a user
typed disappears without being asked for.

An option appearing where a value is expected is read as the option before it
having been left empty, rather than as a value. `add John -p 123 -t -e` reports
that `-t` needs a value instead of storing `-e` as a tag. A value that really
does open with a hyphen is given in quotes.

`FlagTokenizer` reads the tokens. Those before the first option are the name,
joined with single spaces, so a name of several words needs no quotes unless it
would otherwise be misread. Each option takes the token after it, and `-t`
repeats. An unknown option, an option left without a value, and a token
belonging to no option are each reported for what they are, rather than as one
generic complaint.

The shape of the command is settled before any field is read. An unclosed
quote, an unknown option, or a single-valued option given twice all leave it
unclear which field a value belongs to, so each is refused on its own instead
of being collected with the field errors.

`edit` still takes prefixes, and that is a known limitation rather than a
considered design. The collision the options remove from `add` is still present
in `edit`: `edit 1 n/John p/ Smith` reads `Smith` as a phone number, so a name
that `add` now accepts cannot be typed into `edit`. The two commands reading
differently is a second problem on top of the first. Converting `edit` is
tracked as issue #105, and was kept out of the change that introduced the options
so that the new parsing could be reviewed on its own.

### Field values

`Name`, `Tag` and `Phone` each store the form returned by
`StringUtil.normalizeFieldValue`: without zero-width characters, with runs of
whitespace reduced to one ASCII space, the ends trimmed, and the result
composed to Unicode NFC. Composing happens last, because a zero-width
character between a base letter and its combining mark blocks the two from
composing.

They normalize for different reasons, and the rule each one enforces differs.

| Field | Why it is normalized | What it must hold |
| --- | --- | --- |
| `Name` | it is the identity a student is compared by, and the only field the search reads | at least one letter or number |
| `Tag` | it is a key in the set of a student's tags, so two that look alike must not both be stored | at least one letter or number |
| `Phone` | it is displayed beside the others and gains nothing from being stored differently | at least 3 digits |

**The email may be absent.** A student is recorded before an email address is
known rather than one being invented, so `Student` holds it as null and hands
it out as `Optional<Email>`. The field is nullable rather than
`Optional<Email>` because an `Optional` field neither serializes through
Jackson nor survives a round trip, and returning one obliges a caller to decide
what to do when there is none.

A saved file simply carries no `email` key. One that holds a malformed address
is still refused, rather than the address being quietly dropped.

An email cannot be cleared once set. `edit` replaces a value and has no
spelling for removing one, the way `t/` alone empties the tags.

`Phone` is **not** an identity and **not** a key: two students may hold the
same number, and `isSameStudent` does not read it. It is normalized only so
that a number typed with an odd space is stored the way it looks.

Only `Name` is searched. `NameContainsKeywordsPredicate` reads `getName()`
alone, so a tag or a phone number cannot be found by `find` today.

Nothing else is rejected in any of the three. A phone number may hold `+`,
spaces, brackets and an extension; a tag may hold spaces, hyphens and any
script. None of those characters can hinder the app, because none of these
fields is parsed, dialled, or used to build a file path.

#### Student names

A name is displayed, split into words by the search, and compared as the
identity of a student. `Name` therefore stores a normalized form rather than
the raw input, so that two names which look identical cannot be searched
differently or admitted as two students.

**Normalizing.** On construction a name is

* composed to Unicode form NFC, so that `Nguyễn` written as one code point and
  as `e` plus two combining marks become the same string
* stripped of the zero-width space and the byte order mark, which are invisible
  and carry no meaning of their own
* reduced to single ASCII spaces, with the ends trimmed, so that a
  non-breaking space cannot hide a word from the search

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

**A name that holds a command prefix still fails to parse.** The prefixes are
`n/`, `p/`, `e/` and `t/`, and a name holding any of them followed by a space
is read as the start of another field. `a/l` and `a/p`, ordinary components of
a Malaysian name, used to fail this way until the address field was removed
and the `a/` prefix with it. The rest are fixed by replacing the prefix syntax
with positional arguments and flags, not by changing `Name`.

### \[Proposed\] Undo/redo feature

#### Proposed Implementation

The proposed undo/redo mechanism is facilitated by `VersionedAddressBook`. It extends `AddressBook` with an undo/redo history, stored internally as an `addressBookStateList` and `currentStatePointer`. Additionally, it implements the following operations:

* `VersionedAddressBook#commit()` -- Saves the current address book state in its history.
* `VersionedAddressBook#undo()` -- Restores the previous address book state from its history.
* `VersionedAddressBook#redo()` -- Restores a previously undone address book state from its history.

These operations are exposed in the `Model` interface as `Model#commitAddressBook()`, `Model#undoAddressBook()` and `Model#redoAddressBook()` respectively.

Given below is an example usage scenario and how the undo/redo mechanism behaves at each step.

Step 1. The user launches the application for the first time. The `VersionedAddressBook` will be initialized with the initial address book state, and the `currentStatePointer` pointing to that single address book state.

<puml src="diagrams/UndoRedoState0.puml" alt="UndoRedoState0" />

Step 2. The user executes `delete 5` command to delete the 5th person in the address book. The `delete` command calls `Model#commitAddressBook()`, causing the modified state of the address book after the `delete 5` command executes to be saved in the `addressBookStateList`, and the `currentStatePointer` is shifted to the newly inserted address book state.

<puml src="diagrams/UndoRedoState1.puml" alt="UndoRedoState1" />

Step 3. The user executes `add David …​` to add a new person. The `add` command also calls `Model#commitAddressBook()`, causing another modified address book state to be saved into the `addressBookStateList`.

<puml src="diagrams/UndoRedoState2.puml" alt="UndoRedoState2" />

<box type="info" seamless>

**Note:** If a command fails its execution, it will not call `Model#commitAddressBook()`, so the address book state will not be saved into the `addressBookStateList`.
</box>

Step 4. The user now decides that adding the person was a mistake, and decides to undo that action by executing the `undo` command. The `undo` command will call `Model#undoAddressBook()`, which will shift the `currentStatePointer` once to the left, pointing it to the previous address book state, and restores the address book to that state.

<puml src="diagrams/UndoRedoState3.puml" alt="UndoRedoState3" />


<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index 0, pointing to the initial AddressBook state, then there are no previous AddressBook states to restore. The `undo` command uses `Model#canUndoAddressBook()` to check if this is the case. If so, it will return an error to the user rather
than attempting to perform the undo.
</box>

The following sequence diagram shows how an undo operation goes through the `Logic` component:

<puml src="diagrams/UndoSequenceDiagram-Logic.puml" alt="UndoSequenceDiagram-Logic" />

<box type="info" seamless>

**Note:** The lifeline for `UndoCommand` should end at the destroy marker (X), but due to a limitation of PlantUML, it continues to the end of the diagram.
</box>

Similarly, how an undo operation goes through the `Model` component is shown below:

<puml src="diagrams/UndoSequenceDiagram-Model.puml" alt="UndoSequenceDiagram-Model" />

The `redo` command does the opposite — it calls `Model#redoAddressBook()`, which shifts the `currentStatePointer` once to the right, pointing to the previously undone state, and restores the address book to that state.

<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index `addressBookStateList.size() - 1`, pointing to the latest address book state, then there are no undone AddressBook states to restore. The `redo` command uses `Model#canRedoAddressBook()` to check if this is the case. If so, it will return an error to the user rather than attempting to perform the redo.
</box>

Step 5. The user then decides to execute the command `list`. Commands that do not modify the address book, such as `list`, will usually not call `Model#commitAddressBook()`, `Model#undoAddressBook()` or `Model#redoAddressBook()`. Thus, the `addressBookStateList` remains unchanged.

<puml src="diagrams/UndoRedoState4.puml" alt="UndoRedoState4" />

Step 6. The user executes `clear`, which calls `Model#commitAddressBook()`. Since the `currentStatePointer` is not pointing at the end of the `addressBookStateList`, all address book states after the `currentStatePointer` will be purged. Reason: It no longer makes sense to redo the `add David …` command. This is the behavior that most modern desktop applications follow.

<puml src="diagrams/UndoRedoState5.puml" alt="UndoRedoState5" />

The following activity diagram summarizes what happens when a user executes a new command:

<puml src="diagrams/CommitActivityDiagram.puml" width="250" />

#### Design considerations:

**Aspect: How undo & redo execute:**

* **Alternative 1 (current choice):** Saves the entire address book.
  * Pros: Easy to implement.
  * Cons: May have performance issues in terms of memory usage.

* **Alternative 2:** Individual command knows how to undo/redo by
  itself.
  * Pros: Will use less memory (e.g. for `delete`, just save the person being deleted).
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

* has a need to manage 150 to 250 student contacts, sliced by slot, module,
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
| `* * *` | teaching assistant who prefers a CLI | start the app from a terminal | begin working without leaving the keyboard |
| `* * *` | teaching assistant who prefers a CLI | exit the app with a command | close it without reaching for the mouse |
| `* * *` | teaching assistant | view a list of all my students | see everyone I am responsible for at a glance |
| `* * *` | teaching assistant | view a student's name | identify the student |
| `* * *` | teaching assistant | view a student's email | contact them via email for any emergency updates |
| `* * *` | teaching assistant | view a student's tag | know which class the student is in |
| `* * *` | teaching assistant | add records of a student | keep track of new students I have |
| `* * *` | teaching assistant | delete a student's record | remove students who dropped my class from the student book |
| `* * *` | teaching assistant | save the updates I made to various records | ensure my updates persist in future sessions |
| `* * *` | teaching assistant who can type fast and prefers CLI | complete every task the app offers using only the keyboard | never break out of typing to reach for a mouse or trackpad |
| `* * *` | teaching assistant in Singapore | save a name containing s/o, d/o, hyphens or apostrophes | record students under the name that appears on the official roster |
| `* * *` | teaching assistant | flag a student as needing follow-up, and clear the flag once I have replied | keep an accurate list of who is still waiting on me |
| `* * *` | teaching assistant | find students by part of any detail I remember, whatever the letter case | locate a student from a fragment, such as part of a name or a few digits of a phone number |
| `* * *` | teaching assistant handling student data | use the app fully offline with data stored only on my own machine | comply with the expectation that student data stays off third-party services |
| `* *` | forgetful or new user | see a summary of everything the app can do, from inside the app | learn or recall how to use it without leaving the keyboard |
| `* *` | teaching assistant | edit a student's record | ensure a student's record reflect the latest information I know |
| `* *` | teaching assistant | add a tag to a student without disturbing the tags already on their record | build up a student’s tags over the semester without losing earlier ones |
| `* *` | teaching assistant | remove a single tag from a student | drop a student from one group without rebuilding the rest of their record |
| `* *` | teaching assistant | add a student without an email address | record the students I only have partial details for, instead of inventing data |
| `* *` | teaching assistant | know how long it has been since I marked a student for follow-up | know how stale our last interaction is |
| `* *` | teaching assistant who hand-edits the data file | be told at startup that my saved data is unreadable, and where the problem is, instead of the app starting empty | repair it rather than silently lose everything |
| `* *` | teaching assistant | view a student's phone number | call them directly when an urgent matter comes up |
| `* *` | teaching assistant | undo my last command | revert a mistake before it makes trouble |
| `* *` | teaching assistant | find students by a tag | focus on a single tutorial group |
| `*` | teaching assistant | view a student's NUS ID | match a student's name to records on other digital platforms (e.g. attendance) |
| `*` | teaching assistant | find students by a specific detail such as name, NUS ID, phone or email | go straight to the right record when I know one thing about the student |
| `*` | teaching assistant who can type fast and prefers CLI | see a record whenever I make changes to it | confirm that the changes I made are correct |
| `*` | teaching assistant with many students | import many students from a file and give them all the same tag at once | get a whole roster into the app without entering each student by hand |
| `*` | teaching assistant with many students | edit records of many students en masse | update a whole group of students without repeating the same edit |
| `*` | teaching assistant with many students | delete entire classes | remove a class I no longer teach at the end of a semester |
| `*` | teaching assistant | define my own short alias for a tag I use constantly | label a whole roster with far fewer keystrokes |
| `*` | teaching assistant | reset the aliases I have defined | clear shortcuts that no longer match the classes I teach |
| `*` | teaching assistant with exchange students | save a phone number with a country code and spaces | contact students whose numbers are not local 8-digit numbers |
| `*` | teaching assistant | be asked to confirm before all data is wiped | avoid destroying a semester of records with one mistyped command |
| `*` | teaching assistant | view a student's remarks | recall important context such as special needs |
| `*` | teaching assistant | add a remark to a student's record | note down requests or needs I should remember |
| `*` | teaching assistant | redo an action I undid | restore a change I reverted by accident |
| `*` | teaching assistant | be warned when a new student matches an existing record on phone, email or NUS ID | catch genuine duplicates even when the two names are spelled differently |
| `*` | teaching assistant | merge two duplicate records after seeing how they differ | combine the information from both without losing either |
| `*` | teaching assistant | find students who have no tag | spot students not yet assigned to a group |
| `*` | teaching assistant | sort my list by a detail I choose, such as name, NUS ID, or when the student was added | read the list in the order that suits the task in front of me |
| `*` | teaching assistant | group my students by tag | see the class composition at a glance |
| `*` | teaching assistant | export my records to a file | share or print the list externally |
| `*` | teaching assistant | clear all data | start fresh with a clean student book |
| `*` | teaching assistant | reset the app to sample data | explore how the app works before entering real records |
| `*` | teaching assistant | keep separate data files per course | avoid mixing contacts from different modules |
| `*` | long-time teaching assistant | archive the records of students who graduated | keep past students for reference without cluttering my active list |
| `*` | teaching assistant who can type fast | recall and reuse my recent commands | repeat the same command without retyping it |
| `*` | teaching assistant who can type fast | auto-complete commands as I type | type common commands with fewer keystrokes |
| `*` | teaching assistant | see a count of my students | quickly confirm nothing was lost after an import or delete |
| `*` | teaching assistant | be told what was wrong with my input and which part of it caused the problem | correct it in one attempt instead of guessing |
| `*` | teaching assistant who hand-edits the data file | reload the data file from disk without restarting the app | fix the file and carry on in the same sitting |
| `*` | teaching assistant | list students I have not followed up with since a given date | make sure no one who needs help flies under the radar |
| `*` | teaching assistant working in twenty-second windows | identify a student by their NUS ID when deleting a record | avoid removing the wrong person when several students are listed |
| `*` | teaching assistant working in twenty-second windows | identify a student by their NUS ID when editing a record | avoid changing the wrong person’s details |
| `*` | teaching assistant | record two students who genuinely have the same name | keep two different people as two different records |
| `*` | teaching assistant who can type fast | be shown a likely correction when I mistype a command word | recover without looking up the syntax |
| `*` | teaching assistant | look back at results I have already replaced | re-read earlier output without running the search again |
| `*` | teaching assistant | start from my previous input when an entry is rejected | correct one detail instead of retyping everything |
| `*` | teaching assistant on a 1920 × 1080 laptop | start the app with all of it already visible on my screen | begin working without resizing anything first |

#### Considered and dropped

These were raised during requirement gathering and left out of the product.

| As a ... | I wanted to ... | Why it was dropped |
|--------|---------------|--------------------|
| teaching assistant | view a student's address | Cancelled, no need for address |
| teaching assistant | send an email to a student | Not in the scope of the app, it is not a messaging app |
| teaching assistant | send a Telegram message to a student | Not in the scope of the app, it is not a messaging app |
| teaching assistant | send an SMS to a student | Not in the scope of the app, it is not a messaging app |
| teaching assistant with many students | send a mass message to an entire class, by connecting to email | Not in the scope of the app, it is not a messaging app |
| teaching assistant | record a student's attendance status per tutorial | Not in the scope of the app, it is not an attendance taking app. Other platforms already exist to serve this purpose. |
| teaching assistant | view a student's attendance status | Not in the scope of the app, it is not an attendance taking app. Other platforms already exist to serve this purpose. |
| teaching assistant | record grades for a student | Not in the scope of the app, it is not a gradebook. Other platforms already exist to serve this purpose. |

### Use cases

(For all use cases below, the **System** is `TAB` and the **Actor** is the `user`, unless specified otherwise.)

**Table of contents**

* [UC01 - Add a student](#uc01---add-a-student)
* [UC02 - Find a student](#uc02---find-a-student)
* [UC04 - Import a roster](#uc04---import-a-roster)
* [UC05 - Recover from an unreadable data file](#uc05---recover-from-an-unreadable-data-file)
* [UC06 - Delete a class no longer taught](#uc06---delete-a-class-no-longer-taught)
* [UC07 - Archive last semester's students](#uc07---archive-last-semesters-students)
* [UC08 - Close out a semester](#uc08---close-out-a-semester)

#### UC01 - Add a student

**MSS**

1.  User requests to add a student.
2.  User provides available details, including the name.
3.  TAB saves the student.
4.  TAB shows the new record, including the tags it applied.

    Use case ends.

**Extensions**

* 2a. At least one detail is not in an acceptable form.

    * 2a1. TAB names every detail it rejected and the reason.
    * 2a2. TAB leaves the original input in the command box.
    * 2a3. User corrects the details and submits again.

      Steps 2a1-2a3 are repeated until every detail is acceptable.

      Use case resumes at step 3.

* 2b. The student matches another student whom TAB already holds, on NUS ID, email or phone.

    * 2b1. TAB shows the matching student and asks whether to add the new one anyway.
    * 2b2. User confirms that the two are different people.

      Use case resumes at step 3.

* 3a. TAB cannot write to the data file.

    * 3a1. TAB reports that the student was not saved, and why.

      Use case ends.

* *a. At any time, User chooses to abandon the addition.

    * *a1. User clears the input.

      Use case ends.

#### UC02 - Find a student

**MSS**

1.  User requests to find students.
2.  User provides search terms, and optionally indicates the specific field for each search term, like name, email or tag.
3.  TAB shows a list of students matching any part of the search terms, displaying their identifying details.

    Use case ends.

**Extensions**

* 2a. The search terms are not in an acceptable form.

    * 2a1. TAB reports the error.
    * 2a2. TAB leaves the original input in the command box.
    * 2a3. User corrects the search terms and submits again.

      Steps 2a1-2a3 are repeated until the search terms are acceptable.

      Use case resumes at step 3.

* 2b. No student matches the search terms.

    * 2b1. TAB reports that no matching students were found.

      Use case ends.

* *a. At any time, User chooses to abandon the action.

    * *a1. User clears the input.

      Use case ends.

#### UC04 - Import a roster

**MSS**

1.  User requests to import a roster of students from a file.
2.  User provides the file and the class tag to label every student with.
3.  TAB saves the new students, tagged with the given class.
4.  TAB shows how many students were added, matched and skipped.

    Use case ends.

**Extensions**

* 2a. TAB cannot find or read the file.

    * 2a1. TAB reports the problem and leaves the records unchanged.

      Use case ends.

* 2b. A row in the file is missing a required detail or is otherwise malformed.

    * 2b1. TAB names the row and the reason, skips it, and continues to process the other rows.

      Use case resumes at step 3.

* 2c. A student matches another student whom TAB already holds, on NUS ID, email or phone.

    * 2c1. TAB lists the match and asks whether to update the existing records with the imported details.
    * 2c2. User indicates whether to update the existing record.
    * 2c3. TAB updates instead of saving this student, and continues to process the other rows.

      Use case resumes at step 3.

* 2d. TAB cannot write to the data file.

    * 2d1. TAB reports that the import was not saved, and why.

      Use case ends.

* *a. At any time, User chooses to abandon the import.

    * *a1. User clears the input.

      Use case ends.

#### UC05 - Recover from an unreadable data file

**MSS**

1.  User starts TAB.
2.  TAB informs the User that a record is unreadable, identifies the record and the reason, leaves the file unchanged, and displays the readable records.
3.  User repairs the file.
4.  User requests to reload the data file.
5.  TAB reloads the data and confirms that all records are readable.

    Use case ends.

**Extensions**

* 2a. TAB cannot read the file at all, for example because it is not a TAB data file.

    * 2a1. TAB reports why and leaves the file untouched.
    * 2a2. User repairs or replaces the file.

      Use case resumes at step 4.

* 5a. One or more records are still unreadable.

    * 5a1. TAB identifies each unreadable record and the reason, leaves the file unchanged, and displays the
      readable records.
    * 5a2. User repairs the file and requests to reload it.

      Steps 5a1-5a2 are repeated until all records are readable.

      Use case resumes at step 5.

* *a. At any time, User chooses to abandon the recovery.

    * *a1. TAB leaves the file as it found it and continues with the readable records.

      Use case ends.

#### UC06 - Delete a class no longer taught

**MSS**

1.  User requests to delete a class, giving its class tag.
2.  TAB shows the class and every student in it.
3.  TAB asks the user to confirm the deletion, warning that students whose only tag is this class will be removed as well.
4.  User confirms the deletion.
5.  TAB removes the class tag, and the students that it left with no other tag.
6.  TAB shows how many students were affected.

    Use case ends.

**Extensions**

* 2a. TAB holds no student tagged with that class.

    * 2a1. TAB reports that there is no such class.

      Use case ends.

* 3a. User chooses not to delete the class.

    * 3a1. TAB leaves the records unchanged.

      Use case ends.

* 5a. TAB cannot write to the data file.

    * 5a1. TAB reports that the deletion was not saved, and why.

      Use case ends.

* *a. At any time, User chooses to abandon the deletion.

    * *a1. User clears the input.

      Use case ends.

#### UC07 - Archive last semester's students

**MSS**

1.  User requests to archive a past class, giving its class tag and the semester.
2.  TAB shows the students that would be archived.
3.  TAB asks for confirmation.
4.  User confirms.
5.  TAB moves those students into the archive, out of the active list.
6.  TAB shows how many students were archived.

    Use case ends.

**Extensions**

* 2a. No student is tagged with that class for that semester.

    * 2a1. TAB reports that there is nothing to archive.

      Use case ends.

* 3a. User chooses not to archive the students.

    * 3a1. TAB leaves the records unchanged.

      Use case ends.

* 5a. TAB cannot write to the data file or the archive.

    * 5a1. TAB reports that the archiving was not saved, and why.

      Use case ends.

* *a. At any time, User chooses to abandon the archiving.

    * *a1. User clears the input.

      Use case ends.

#### UC08 - Close out a semester

**MSS**

1.  User requests to close out a semester, giving the semester and, if needed, the module(s) it covered.
2.  TAB lists every student still active for that semester.
3.  TAB asks how to treat any records still awaiting a follow-up, and asks for confirmation.
4.  User confirms the close-out.
5.  TAB archives the semester's students, recording who was still awaiting a follow-up.
6.  TAB starts a clean student list for the coming semester, keeping the archive loadable.

    Use case ends.

**Extensions**

* 2a. No student is active for that semester.

    * 2a1. TAB reports that there is nothing to close out.

      Use case ends.

* 3a. Records still await a follow-up.

    * 3a1. TAB reminds the user how many, and asks whether to archive them with the rest or hold them back.
    * 3a2. User chooses how to treat them.

      Use case resumes at step 4.

* 5a. TAB cannot write the archive or the fresh file.

    * 5a1. TAB reports that the close-out was not saved, and why.

      Use case ends.

* *a. At any time, User chooses to abandon the close-out.

    * *a1. User clears the input.

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

* **Mainstream OS**: Windows, Linux, Unix, or macOS
* **Private contact detail**: A contact detail that is not meant to be shared with others

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Given below are instructions to test the app manually.

<box type="info" seamless>

**Note:** These instructions only provide a starting point for testers to work on;
testers are expected to do more *exploratory* testing.
</box>

### Launch and shutdown

1. Initial launch

   1. Download the JAR file and copy it into an empty folder.

   1. Double-click the JAR file.<br>
      Expected: The GUI opens with a set of sample contacts. The window size may not be optimal.

1. Saving window preferences

   1. Resize the window to an optimal size. Move the window to a different location. Close the window.

   1. Relaunch the app by double-clicking the JAR file.<br>
       Expected: The most recent window size and location are retained.

1. _{ more test cases … }_

### Deleting a person

1. Deleting a person while all persons are being shown

   1. Prerequisites: List all persons using the `list` command, with multiple persons in the list.

   1. Test case: `delete 1`<br>
      Expected: The first contact is deleted from the list. The status message shows the deleted contact's details.

   1. Test case: `delete 0`<br>
      Expected: No person is deleted. The status message shows error details.

   1. Other incorrect delete commands to try: `delete`, `delete x`, `...` (where x is larger than the list size)<br>
      Expected: Similar to previous.

1. _{ more test cases … }_

### Saving data

1. Dealing with missing/corrupted data files

   1. _{Explain how to simulate missing or corrupted data files and state the expected behavior.}_

1. _{ more test cases … }_
